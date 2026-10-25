package com.example.musikku.data.repository

import android.content.Context
import android.util.Log
import com.example.musikku.data.local.dao.SongDao
import com.example.musikku.data.local.entity.SongEntity
import com.example.musikku.data.remote.youtube.InvidiousInstanceManager
import com.example.musikku.data.remote.youtube.YouTubeApiService
import com.example.musikku.data.remote.youtube.YouTubeSearchItemDto
import com.example.musikku.data.remote.youtube.toSongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.musikku.data.preferences.SettingsRepository
import com.example.musikku.data.worker.DownloadSongWorker
import kotlinx.coroutines.flow.first

/**
 * Repository untuk pencarian dan pemutaran audio YouTube Music via Invidious / Piped API.
 * Mendukung pencarian lagu, musik trending, resolusi stream audio direct m4a/opus,
 * serta failover otomatis antar instance publik menggunakan InvidiousInstanceManager.
 */
class YouTubeMusicRepository(
    private val context: Context,
    private val apiService: YouTubeApiService,
    private val songDao: SongDao,
    private val settingsRepository: SettingsRepository,
    private val instanceManager: InvidiousInstanceManager
) {
    companion object {
        private const val TAG = "YouTubeMusicRepo"

        /** Jumlah maksimum percobaan instance sebelum menyerah */
        private const val MAX_RETRY_ATTEMPTS = 5
    }

    /**
     * Ambil daftar musik yang sedang trending di YouTube Music.
     */
    suspend fun getTrendingMusic(): Result<List<YouTubeSearchItemDto>> = withContext(Dispatchers.IO) {
        // Prioritaskan pencarian musik trending langsung via NewPipeExtractor di perangkat agar instan & stabil
        val trendingResult = searchMusic("Trending Musik Indonesia")
        if (trendingResult.isSuccess && !trendingResult.getOrNull().isNullOrEmpty()) {
            return@withContext trendingResult
        }
        return@withContext searchMusic("Lagu Pop Indonesia Terbaru")
    }

    /**
     * Cari lagu atau video musik berdasarkan kata kunci pencarian.
     */
    suspend fun searchMusic(query: String): Result<List<YouTubeSearchItemDto>> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return@withContext getTrendingMusic()
        }

        // ─── 1. Prioritas Utama: Ekstraksi pencarian langsung via NewPipeExtractor ───
        try {
            Log.d(TAG, "Mencari '$trimmed' via NewPipeExtractor langsung di HP...")
            val queryHandler = org.schabi.newpipe.extractor.ServiceList.YouTube.searchQHFactory.fromQuery(trimmed)
            val searchInfo = org.schabi.newpipe.extractor.search.SearchInfo.getInfo(
                org.schabi.newpipe.extractor.ServiceList.YouTube,
                queryHandler
            )
            val items = searchInfo.relatedItems.mapNotNull { infoItem ->
                if (infoItem is org.schabi.newpipe.extractor.stream.StreamInfoItem) {
                    val url = infoItem.url.orEmpty()
                    val videoId = if (url.contains("v=")) {
                        url.substringAfter("v=").substringBefore("&")
                    } else {
                        url.substringAfterLast("/")
                    }
                    if (videoId.isNotBlank()) {
                        YouTubeSearchItemDto(
                            type = "video",
                            title = infoItem.name.orEmpty(),
                            videoId = videoId,
                            author = infoItem.uploaderName,
                            authorUrl = infoItem.uploaderUrl,
                            lengthSeconds = infoItem.duration.let { if (it > 0) it else 0L },
                            viewCount = infoItem.viewCount.let { if (it > 0) it else 0L },
                            videoThumbnails = null
                        )
                    } else null
                } else null
            }
            if (items.isNotEmpty()) {
                Log.d(TAG, "Sukses mencari '$trimmed' via NewPipeExtractor! Ditemukan ${items.size} item")
                return@withContext Result.success(items)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "NewPipeExtractor pencarian gagal: ${e.message}, mencoba fallback Invidious...")
        }

        // ─── 2. Fallback: Invidious Instances ───
        var lastException: Exception? = null
        val instances = instanceManager.getAllInstances()
        val maxAttempts = minOf(MAX_RETRY_ATTEMPTS, instances.size)

        for (attempt in 0 until maxAttempts) {
            val base = if (attempt == 0) {
                instanceManager.getWorkingInstance()
            } else {
                instances.getOrNull(attempt) ?: break
            }

            try {
                Log.d(TAG, "Mencari '$trimmed' di instance: $base (percobaan ${attempt + 1}/$maxAttempts)")
                val endpoint = "$base/api/v1/search"
                val items = apiService.searchVideos(endpoint = endpoint, query = trimmed, type = "video")
                val validItems = items.filter { it.videoId.isNotBlank() }
                if (validItems.isNotEmpty()) {
                    instanceManager.markSuccess(base)
                    return@withContext Result.success(validItems)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gagal mencari di $base: ${e.message}")
                lastException = e
                instanceManager.markFailed(base)
            }
        }

        Result.failure(lastException ?: Exception("Gagal mencari lagu di YouTube Music. Periksa koneksi internet atau coba lagi nanti."))
    }

    /**
     * Resolusi URL stream audio langsung dari YouTube untuk video tertentu.
     * Memprioritaskan format AAC 128kbps (itag 140 / m4a) yang paling optimal untuk ExoPlayer.
     */
    suspend fun resolveAudioStream(item: YouTubeSearchItemDto): Result<SongEntity> = withContext(Dispatchers.IO) {
        val videoId = item.videoId
        if (videoId.isBlank()) {
            return@withContext Result.failure(Exception("ID video YouTube tidak valid"))
        }

        // ─── 1. Prioritas Utama: Ekstrak audio stream langsung di HP via NewPipeExtractor ───
        try {
            Log.d(TAG, "Mencoba ekstraksi langsung di HP via NewPipeExtractor untuk videoId=$videoId")
            val videoUrl = "https://www.youtube.com/watch?v=$videoId"
            val streamInfo = org.schabi.newpipe.extractor.stream.StreamInfo.getInfo(
                org.schabi.newpipe.extractor.ServiceList.YouTube,
                videoUrl
            )

            val audioStreams = streamInfo.audioStreams
            if (!audioStreams.isNullOrEmpty()) {
                // Prioritaskan M4A (itag 140) yang paling optimal untuk ExoPlayer Android
                val bestAudio = audioStreams.firstOrNull { it.itag == 140 }
                    ?: audioStreams.maxByOrNull { it.averageBitrate }
                    ?: audioStreams.first()

                val directUrl = bestAudio.content
                if (!directUrl.isNullOrBlank()) {
                    Log.d(TAG, "Sukses mengekstrak audio via NewPipeExtractor! itag=${bestAudio.itag}, bitrate=${bestAudio.averageBitrate}")
                    val songEntity = item.toSongEntity(streamUrl = directUrl)
                    return@withContext Result.success(songEntity)
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "NewPipeExtractor ekstraksi gagal untuk $videoId: ${e.message}", e)
        }

        // ─── 2. Fallback: Invidious Instances ──────────────────────────────────────────
        var lastException: Exception? = null
        val instances = instanceManager.getAllInstances()
        val maxAttempts = minOf(MAX_RETRY_ATTEMPTS, instances.size)

        for (attempt in 0 until maxAttempts) {
            val base = if (attempt == 0) {
                instanceManager.getWorkingInstance()
            } else {
                instances.getOrNull(attempt) ?: break
            }

            try {
                Log.d(TAG, "Mengambil stream audio untuk videoId=$videoId dari $base (percobaan ${attempt + 1}/$maxAttempts)")
                val endpoint = "$base/api/v1/videos/$videoId?local=true"
                val details = apiService.getVideoDetails(endpoint)

                // 1. Prioritaskan adaptive format AAC m4a (itag 140)
                val m4aStream = details.adaptiveFormats?.firstOrNull {
                    it.itag == "140" || (it.container?.equals("m4a", ignoreCase = true) == true) ||
                            (it.type?.contains("audio/mp4", ignoreCase = true) == true)
                }

                // 2. Fallback ke stream audio lainnya (misal webm opus itag 251/250/249)
                val anyAudioStream = m4aStream ?: details.adaptiveFormats?.firstOrNull {
                    it.type?.startsWith("audio/", ignoreCase = true) == true
                }

                // 3. Fallback terakhir ke formatStream kombinasi (itag 18 mp4)
                val rawStreamUrl = anyAudioStream?.url
                    ?: details.formatStreams?.firstOrNull { it.url.isNotBlank() }?.url

                if (!rawStreamUrl.isNullOrBlank()) {
                    val finalStreamUrl = when {
                        rawStreamUrl.startsWith("/") -> "$base$rawStreamUrl"
                        rawStreamUrl.contains("googlevideo.com") -> {
                            // Proxy melalui Invidious /videoplayback jika URL googlevideo langsung diblokir (403 Forbidden)
                            val uri = android.net.Uri.parse(rawStreamUrl)
                            val query = uri.query
                            if (!query.isNullOrBlank()) "$base/videoplayback?$query" else rawStreamUrl
                        }
                        else -> rawStreamUrl
                    }

                    val songEntity = item.toSongEntity(streamUrl = finalStreamUrl)
                    // PENTING: JANGAN simpan ke songDao saat pemutaran biasa agar tidak mencemari Library koleksi lokal!
                    // Entri hanya disimpan ke Room jika pengguna secara eksplisit menambahkannya ke sebuah Playlist.

                    instanceManager.markSuccess(base)
                    return@withContext Result.success(songEntity)
                } else {
                    lastException = Exception("Stream audio tidak ditemukan untuk lagu ini.")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gagal mengambil stream audio dari $base: ${e.message}")
                lastException = e
                instanceManager.markFailed(base)
            }
        }

        val errorMessage = when {
            lastException?.message?.contains("500") == true ->
                "Streaming YouTube dibatasi oleh Google (PO-Token / HTTP 500). Silakan putar di tab Jamendo untuk streaming bebas hambatan."
            lastException?.message?.contains("403") == true ->
                "Akses streaming YouTube ditolak (HTTP 403 Forbidden). Silakan coba lagu lain atau putar di tab Jamendo."
            else ->
                "Streaming YouTube sedang tidak tersedia di server publik. Silakan gunakan tab Jamendo untuk musik streaming & unduh bebas hambatan."
        }
        Result.failure(Exception(errorMessage, lastException))
    }

    /**
     * Daftarkan tugas pengunduhan audio YouTube ke WorkManager agar tersimpan secara lokal dan dapat diputar offline.
     */
    suspend fun downloadTrack(track: YouTubeSearchItemDto): Result<Unit> = withContext(Dispatchers.IO) {
        val streamResult = resolveAudioStream(track)
        if (streamResult.isFailure) {
            return@withContext Result.failure(streamResult.exceptionOrNull() ?: Exception("Gagal mengekstrak stream audio YouTube"))
        }
        val song = streamResult.getOrThrow()
        val directUrl = song.contentUri
        val safeId = song.id

        val inputData = Data.Builder()
            .putLong(DownloadSongWorker.KEY_SONG_ID, safeId)
            .putString(DownloadSongWorker.KEY_TITLE, track.title)
            .putString(DownloadSongWorker.KEY_ARTIST, track.author ?: "YouTube Music")
            .putString(DownloadSongWorker.KEY_ALBUM, "YouTube Music")
            .putLong(DownloadSongWorker.KEY_DURATION, track.lengthSeconds * 1000L)
            .putString(DownloadSongWorker.KEY_DOWNLOAD_URL, directUrl)
            .putString(DownloadSongWorker.KEY_ALBUM_ART_URL, track.thumbnailUrl ?: "")
            .build()

        val isWifiOnly = settingsRepository.downloadOnlyWifi.first()
        val networkType = if (isWifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<DownloadSongWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag("download_track")
            .addTag("download_${safeId}")
            .addTag("song_${safeId}")
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
        Result.success(Unit)
    }
}
