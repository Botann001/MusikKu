package com.example.musikku.data.repository

import android.content.Context
import android.util.Log
import com.example.musikku.data.local.dao.SongDao
import com.example.musikku.data.local.entity.SongEntity
import com.example.musikku.data.remote.youtube.YouTubeApiService
import com.example.musikku.data.remote.youtube.YouTubeSearchItemDto
import com.example.musikku.data.remote.youtube.toSongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository untuk pencarian dan pemutaran audio YouTube Music via Invidious / Piped API.
 * Mendukung pencarian lagu, musik trending, resolusi stream audio direct m4a/opus,
 * serta failover otomatis antar instance publik.
 */
class YouTubeMusicRepository(
    private val context: Context,
    private val apiService: YouTubeApiService,
    private val songDao: SongDao
) {
    companion object {
        private const val TAG = "YouTubeMusicRepo"

        /**
         * Daftar server Invidious terverifikasi yang mendukung API search dan adaptive audio streams.
         */
        val INSTANCE_CANDIDATES = listOf(
            "https://invidious.f5.si",
            "https://invidious.projectsegfau.lt",
            "https://iv.ggtyler.dev",
            "https://invidious.nerdvpn.de"
        )
    }

    private var currentInstanceIndex = 0

    @Synchronized
    private fun getCurrentInstance(): String {
        return INSTANCE_CANDIDATES[currentInstanceIndex % INSTANCE_CANDIDATES.size]
    }

    @Synchronized
    private fun rotateInstance() {
        currentInstanceIndex = (currentInstanceIndex + 1) % INSTANCE_CANDIDATES.size
        Log.w(TAG, "Berpindah ke instance YouTube berikutnya: ${getCurrentInstance()}")
    }

    /**
     * Ambil daftar musik yang sedang trending di YouTube Music.
     */
    suspend fun getTrendingMusic(): Result<List<YouTubeSearchItemDto>> = withContext(Dispatchers.IO) {
        var lastException: Exception? = null

        for (attempt in INSTANCE_CANDIDATES.indices) {
            val base = getCurrentInstance()
            try {
                Log.d(TAG, "Memuat trending musik dari: $base")
                val endpoint = "$base/api/v1/trending"
                val items = apiService.getTrending(endpoint = endpoint, type = "music")
                val validItems = items.filter { it.videoId.isNotBlank() && !it.liveNow && it.lengthSeconds >= 30 }
                if (validItems.isNotEmpty()) {
                    return@withContext Result.success(validItems)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gagal memuat trending dari $base: ${e.message}")
                lastException = e
                rotateInstance()
            }
        }

        // Jika endpoint trending gagal di semua instance, fallback ke pencarian musik populer Indonesia
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

        var lastException: Exception? = null

        for (attempt in INSTANCE_CANDIDATES.indices) {
            val base = getCurrentInstance()
            try {
                Log.d(TAG, "Mencari '$trimmed' di instance: $base")
                val endpoint = "$base/api/v1/search"
                val items = apiService.searchVideos(endpoint = endpoint, query = trimmed, type = "video")
                val validItems = items.filter { it.videoId.isNotBlank() }
                return@withContext Result.success(validItems)
            } catch (e: Exception) {
                Log.w(TAG, "Gagal mencari di $base: ${e.message}")
                lastException = e
                rotateInstance()
            }
        }

        Result.failure(lastException ?: Exception("Gagal mencari lagu di YouTube Music. Periksa koneksi internet."))
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

        var lastException: Exception? = null

        for (attempt in INSTANCE_CANDIDATES.indices) {
            val base = getCurrentInstance()
            try {
                Log.d(TAG, "Mengambil stream audio untuk videoId=$videoId dari $base")
                val endpoint = "$base/api/v1/videos/$videoId"
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
                    val finalStreamUrl = if (rawStreamUrl.startsWith("/")) {
                        "$base$rawStreamUrl"
                    } else {
                        rawStreamUrl
                    }

                    val songEntity = item.toSongEntity(streamUrl = finalStreamUrl)
                    // Simpan atau perbarui entri lagu di database Room agar bisa disimpan ke playlist lokal
                    try {
                        songDao.insertAll(listOf(songEntity))
                    } catch (e: Exception) {
                        Log.w(TAG, "Gagal menyimpan entri lagu ke Room: ${e.message}")
                    }

                    return@withContext Result.success(songEntity)
                } else {
                    lastException = Exception("Stream audio tidak ditemukan untuk lagu ini.")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gagal mengambil stream audio dari $base: ${e.message}")
                lastException = e
                rotateInstance()
            }
        }

        Result.failure(lastException ?: Exception("Gagal memutar audio dari YouTube Music. Silakan coba lagi."))
    }
}
