package com.example.musikku.data.worker

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.musikku.appContainer
import com.example.musikku.data.local.entity.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Worker WorkManager untuk mengunduh lagu dari Jamendo di latar belakang,
 * memantau kemajuan unduhan, dan mencatatnya ke Room dengan source="DOWNLOADED"
 * dan filePath lokal agar dapat diputar secara offline.
 */
class DownloadSongWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_SONG_ID = "key_song_id"
        const val KEY_TITLE = "key_title"
        const val KEY_ARTIST = "key_artist"
        const val KEY_ALBUM = "key_album"
        const val KEY_DURATION = "key_duration"
        const val KEY_DOWNLOAD_URL = "key_download_url"
        const val KEY_ALBUM_ART_URL = "key_album_art_url"
        const val KEY_PROGRESS = "progress"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val songId = inputData.getLong(KEY_SONG_ID, -1L)
        val title = inputData.getString(KEY_TITLE) ?: return@withContext Result.failure()
        val artist = inputData.getString(KEY_ARTIST) ?: "Jamendo Artist"
        val album = inputData.getString(KEY_ALBUM) ?: "Jamendo Music"
        val duration = inputData.getLong(KEY_DURATION, 0L)
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL) ?: return@withContext Result.failure()
        val albumArtUrl = inputData.getString(KEY_ALBUM_ART_URL) ?: ""

        if (songId == -1L || downloadUrl.isBlank()) {
            return@withContext Result.failure()
        }

        try {
            setProgress(workDataOf(KEY_PROGRESS to 0))

            // Direktori unduhan musik lokal aplikasi di penyimpanan internal/eksternal app
            val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?: context.filesDir
            if (!musicDir.exists()) {
                musicDir.mkdirs()
            }

            // Validasi ruang penyimpanan perangkat (storage penuh)
            val freeBytes = musicDir.usableSpace
            if (freeBytes < 15 * 1024 * 1024L) {
                return@withContext Result.failure(
                    workDataOf("error" to "Penyimpanan HP penuh. Kosongkan ruang penyimpanan untuk mengunduh lagu.")
                )
            }

            // Bersihkan karakter terlarang untuk nama file
            val safeFileName = title.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
            val targetFile = File(musicDir, "${safeFileName}_$songId.mp3")

            // Unduh stream audio HTTP
            val url = URL(downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
            }
            connection.connect()

            if (connection.responseCode !in 200..299) {
                return@withContext Result.retry()
            }

            val totalBytes = connection.contentLengthLong
            if (totalBytes > 0 && freeBytes < totalBytes + 5 * 1024 * 1024L) {
                return@withContext Result.failure(
                    workDataOf("error" to "Ruang penyimpanan tidak mencukupi untuk file audio ini.")
                )
            }
            var bytesRead = 0L
            var lastReportedPercent = 0

            connection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        if (isStopped) {
                            targetFile.delete()
                            return@withContext Result.failure()
                        }
                        output.write(buffer, 0, read)
                        bytesRead += read
                        if (totalBytes > 0) {
                            val percent = ((bytesRead * 100) / totalBytes).toInt().coerceIn(0, 100)
                            if (percent >= lastReportedPercent + 5 || percent == 100) {
                                lastReportedPercent = percent
                                setProgress(workDataOf(KEY_PROGRESS to percent))
                            }
                        }
                    }
                }
            }

            setProgress(workDataOf(KEY_PROGRESS to 100))

            // Simpan entri file yang berhasil diunduh ke Room lokal melalui AppContainer database
            val database = context.appContainer.database

            val downloadedSong = SongEntity(
                id = songId,
                title = title,
                artist = artist,
                album = album,
                duration = duration,
                contentUri = Uri.fromFile(targetFile).toString(), // Menggunakan URI file offline lokal
                albumArtUri = albumArtUrl,
                dateAdded = System.currentTimeMillis() / 1000,
                size = targetFile.length(),
                source = "DOWNLOADED",
                isFavorite = false,
                filePath = targetFile.absolutePath
            )

            database.songDao().insertAll(listOf(downloadedSong))

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
