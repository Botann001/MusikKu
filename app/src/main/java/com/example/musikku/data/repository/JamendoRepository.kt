package com.example.musikku.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.musikku.data.local.dao.SongDao
import com.example.musikku.data.preferences.SettingsRepository
import com.example.musikku.data.remote.JamendoApiService
import com.example.musikku.data.remote.dto.JamendoTrackDto
import com.example.musikku.data.worker.DownloadSongWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Repository untuk pencarian, streaming, dan pengunduhan lagu dari Jamendo API v3.0.
 */
class JamendoRepository(
    private val context: Context,
    private val apiService: JamendoApiService,
    private val songDao: SongDao,
    private val settingsRepository: SettingsRepository,
    private val clientId: String
) {

    private val workManager = WorkManager.getInstance(context)

    /**
     * Aliran ID lagu-lagu yang telah berhasil diunduh ke database lokal.
     */
    val downloadedSongIds: Flow<List<Long>> = songDao.getDownloadedSongIds()

    /**
     * Memantau progres unduhan yang sedang berjalan secara reaktif.
     * Mengembalikan peta: SongId -> Persentase Kemajuan (0..100).
     */
    val downloadProgressMap: Flow<Map<Long, Int>> = workManager
        .getWorkInfosByTagFlow("download_track")
        .map { workInfos ->
            val progressMap = mutableMapOf<Long, Int>()
            for (info in workInfos) {
                if (info.state == WorkInfo.State.RUNNING || info.state == WorkInfo.State.ENQUEUED) {
                    val songId = info.tags.firstOrNull { it.startsWith("song_") }
                        ?.removePrefix("song_")
                        ?.toLongOrNull()
                    if (songId != null) {
                        val progress = info.progress.getInt(DownloadSongWorker.KEY_PROGRESS, 0)
                        progressMap[songId] = progress
                    }
                }
            }
            progressMap
        }

    /**
     * Ambil daftar lagu populer dari Jamendo.
     */
    suspend fun getPopularTracks(): Result<List<JamendoTrackDto>> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.e("MusikKuJamendo", "getPopularTracks() called with clientId='$clientId'")
            val response = apiService.getPopularTracks(clientId = clientId)
            android.util.Log.e("MusikKuJamendo", "getPopularTracks() response status=${response.headers?.status}, count=${response.results.size}")
            if (response.headers?.status == "failed") {
                val errorMsg = response.headers.errorMessage ?: "API Jamendo gagal memberikan respon"
                Result.failure(Exception(errorMsg))
            } else {
                Result.success(response.results)
            }
        } catch (e: Exception) {
            android.util.Log.e("MusikKuJamendo", "getPopularTracks() exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Cari lagu berdasarkan query, atau ambil daftar populer jika query kosong.
     */
    suspend fun searchTracks(query: String): Result<List<JamendoTrackDto>> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return@withContext getPopularTracks()
        }
        try {
            android.util.Log.e("MusikKuJamendo", "searchTracks('$trimmed') called with clientId='$clientId'")
            val nameRes = apiService.searchTracks(clientId = clientId, nameSearch = trimmed)
            val results = if (nameRes.results.isNotEmpty()) {
                nameRes.results
            } else {
                val tagRes = apiService.searchTracks(clientId = clientId, fuzzyTags = trimmed)
                tagRes.results
            }
            android.util.Log.e("MusikKuJamendo", "searchTracks('$trimmed') result count=${results.size}")
            Result.success(results)
        } catch (e: Exception) {
            android.util.Log.e("MusikKuJamendo", "searchTracks('$trimmed') exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Daftarkan tugas pengunduhan latar belakang ke WorkManager.
     * Menggunakan pengaturan DataStore untuk membatasi ke jaringan Wi-Fi jika diaktifkan.
     */
    suspend fun downloadTrack(track: JamendoTrackDto) = withContext(Dispatchers.IO) {
        val downloadUrl = track.audioDownload?.takeIf { it.isNotBlank() } ?: track.audio ?: return@withContext
        val safeId = track.id.toLongOrNull() ?: (track.id.hashCode().toLong().let { if (it < 0) -it else it })

        val inputData = Data.Builder()
            .putLong(DownloadSongWorker.KEY_SONG_ID, safeId)
            .putString(DownloadSongWorker.KEY_TITLE, track.name)
            .putString(DownloadSongWorker.KEY_ARTIST, track.artistName ?: "Artis Jamendo")
            .putString(DownloadSongWorker.KEY_ALBUM, track.albumName ?: "Jamendo Music")
            .putLong(DownloadSongWorker.KEY_DURATION, track.duration * 1000L)
            .putString(DownloadSongWorker.KEY_DOWNLOAD_URL, downloadUrl)
            .putString(DownloadSongWorker.KEY_ALBUM_ART_URL, track.albumImage ?: track.image ?: "")
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

        workManager.enqueue(workRequest)
    }

    /**
     * Hapus lagu unduhan dari penyimpanan perangkat dan dari tabel Room.
     */
    suspend fun deleteDownloadedTrack(songId: Long) = withContext(Dispatchers.IO) {
        val song = songDao.getSongById(songId)
        if (song != null) {
            song.filePath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }
            songDao.deleteSongById(songId)
        }
    }
}
