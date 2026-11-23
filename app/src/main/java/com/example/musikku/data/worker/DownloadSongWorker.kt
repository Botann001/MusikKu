package com.example.musikku.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.musikku.MainActivity
import com.example.musikku.appContainer
import com.example.musikku.data.local.entity.SongEntity
import com.example.musikku.security.SecurityGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Worker WorkManager untuk mengunduh lagu dari Jamendo & YouTube Music di latar belakang,
 * menampilkan notifikasi progres pengunduhan secara real-time,
 * dan mencatatnya ke Room dengan source="DOWNLOADED" agar dapat diputar offline.
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

        const val CHANNEL_ID = "musikku_download_channel"
        const val CHANNEL_NAME = "Unduhan Lagu"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val songId = inputData.getLong(KEY_SONG_ID, -1L)
        val title = inputData.getString(KEY_TITLE) ?: return@withContext Result.failure()
        val artist = inputData.getString(KEY_ARTIST) ?: "Artis"
        val album = inputData.getString(KEY_ALBUM) ?: "Musik"
        val duration = inputData.getLong(KEY_DURATION, 0L)
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL) ?: return@withContext Result.failure()
        val albumArtUrl = inputData.getString(KEY_ALBUM_ART_URL) ?: ""

        if (songId == -1L || downloadUrl.isBlank()) {
            return@withContext Result.failure()
        }

        val notificationManager = NotificationManagerCompat.from(context)
        val notificationId = (songId.hashCode() and 0x7FFFFFFF).let { if (it == 0) 1001 else it }
        val pendingIntent = createPendingIntent()

        createNotificationChannel()

        // Notifikasi Awal: Menyiapkan Unduhan
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Mengunduh Lagu")
            .setContentText("$title • $artist")
            .setSubText("0%")
            .setProgress(100, 0, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        notifySafe(notificationManager, notificationId, notificationBuilder.build())

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
                showFailureNotification(
                    notificationManager,
                    notificationId,
                    title,
                    "Penyimpanan HP penuh. Kosongkan ruang untuk mengunduh lagu.",
                    pendingIntent
                )
                return@withContext Result.failure(
                    workDataOf("error" to "Penyimpanan HP penuh. Kosongkan ruang penyimpanan untuk mengunduh lagu.")
                )
            }

            // Bersihkan karakter terlarang untuk nama file dan tentukan ekstensi file yang sesuai
            val safeFileName = title.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
            val isMp4OrM4a = downloadUrl.contains("audio/mp4") || downloadUrl.contains("itag=140") ||
                    downloadUrl.contains("itag=139") || downloadUrl.contains("googlevideo.com")
            val extension = if (isMp4OrM4a) "m4a" else "mp3"
            val targetFile = File(musicDir, "${safeFileName}_$songId.$extension")

            // Proteksi Path Traversal: Cegah penulisan di luar folder unduhan
            if (!SecurityGuard.validateSafeFilePath(musicDir, targetFile)) {
                showFailureNotification(
                    notificationManager,
                    notificationId,
                    title,
                    "Peringatan Keamanan: Percobaan manipulasi path file.",
                    pendingIntent
                )
                return@withContext Result.failure(
                    workDataOf("error" to "Peringatan Keamanan: Terdeteksi percobaan manipulasi path file.")
                )
            }

            // Unduh stream audio HTTP
            val url = URL(downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 40_000
                instanceFollowRedirects = true
                if (downloadUrl.contains("googlevideo.com") || downloadUrl.contains("youtube.com")) {
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_3) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/605.1.15")
                    setRequestProperty("Origin", "https://www.youtube.com")
                    setRequestProperty("Referer", "https://www.youtube.com/")
                }
            }
            connection.connect()

            if (connection.responseCode !in 200..299) {
                showFailureNotification(
                    notificationManager,
                    notificationId,
                    title,
                    "Gagal menghubungkan ke server unduhan (HTTP ${connection.responseCode}).",
                    pendingIntent
                )
                return@withContext Result.retry()
            }

            val totalBytes = connection.contentLengthLong
            if (totalBytes > 0 && freeBytes < totalBytes + 5 * 1024 * 1024L) {
                showFailureNotification(
                    notificationManager,
                    notificationId,
                    title,
                    "Ruang penyimpanan tidak mencukupi untuk file audio ini.",
                    pendingIntent
                )
                return@withContext Result.failure(
                    workDataOf("error" to "Ruang penyimpanan tidak mencukupi untuk file audio ini.")
                )
            }
            var bytesRead = 0L
            var lastReportedPercent = 0
            var lastMbReported = -1L

            connection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        if (isStopped) {
                            targetFile.delete()
                            cancelNotificationSafe(notificationManager, notificationId)
                            return@withContext Result.failure()
                        }
                        output.write(buffer, 0, read)
                        bytesRead += read

                        if (totalBytes > 0) {
                            val percent = ((bytesRead * 100) / totalBytes).toInt().coerceIn(0, 100)
                            if (percent >= lastReportedPercent + 5 || percent == 100) {
                                lastReportedPercent = percent
                                setProgress(workDataOf(KEY_PROGRESS to percent))

                                notificationBuilder
                                    .setSubText("$percent%")
                                    .setProgress(100, percent, false)
                                notifySafe(notificationManager, notificationId, notificationBuilder.build())
                            }
                        } else {
                            val mbUnit = bytesRead / (512 * 1024L)
                            if (mbUnit > lastMbReported) {
                                lastMbReported = mbUnit
                                val mbText = String.format(Locale.getDefault(), "%.1f MB", bytesRead / (1024.0 * 1024.0))
                                notificationBuilder
                                    .setSubText(mbText)
                                    .setProgress(0, 0, true)
                                notifySafe(notificationManager, notificationId, notificationBuilder.build())
                            }
                        }
                    }
                }
            }

            setProgress(workDataOf(KEY_PROGRESS to 100))

            // Proteksi Anti-Malware & Anti-Ransomware: Validasi Magic Bytes berkas audio
            val audioValidation = SecurityGuard.validateAudioMagicBytes(targetFile)
            if (audioValidation == SecurityGuard.AudioValidationResult.MALICIOUS_EXECUTABLE) {
                targetFile.delete()
                showFailureNotification(
                    notificationManager,
                    notificationId,
                    title,
                    "Berkas berbahaya (Malware) terdeteksi dan telah dimusnahkan.",
                    pendingIntent
                )
                return@withContext Result.failure(
                    workDataOf("error" to "Berkas terdeteksi sebagai muatan berbahaya (Malware) dan telah dimusnahkan.")
                )
            } else if (audioValidation == SecurityGuard.AudioValidationResult.CORRUPTED_OR_EMPTY) {
                targetFile.delete()
                showFailureNotification(
                    notificationManager,
                    notificationId,
                    title,
                    "Berkas audio yang diunduh rusak atau kosong.",
                    pendingIntent
                )
                return@withContext Result.failure(
                    workDataOf("error" to "Berkas audio rusak atau korup.")
                )
            }

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

            // Notifikasi Selesai: Berhasil Diunduh
            val successNotification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Unduhan Selesai")
                .setContentText("$title ($artist) berhasil diunduh")
                .setSubText(null)
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            notifySafe(notificationManager, notificationId, successNotification)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            showFailureNotification(
                notificationManager,
                notificationId,
                title,
                e.localizedMessage ?: "Terjadi kesalahan saat mengunduh",
                pendingIntent
            )
            Result.retry()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Menampilkan progres dan status unduhan lagu"
                setShowBadge(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun showFailureNotification(
        manager: NotificationManagerCompat,
        notificationId: Int,
        title: String,
        errorMsg: String,
        pendingIntent: PendingIntent
    ) {
        val failNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Gagal Mengunduh")
            .setContentText("$title: $errorMsg")
            .setSubText(null)
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notifySafe(manager, notificationId, failNotification)
    }

    private fun notifySafe(
        manager: NotificationManagerCompat,
        id: Int,
        notification: android.app.Notification
    ) {
        try {
            manager.notify(id, notification)
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }
    }

    private fun cancelNotificationSafe(
        manager: NotificationManagerCompat,
        id: Int
    ) {
        try {
            manager.cancel(id)
        } catch (_: Exception) {
        }
    }
}
