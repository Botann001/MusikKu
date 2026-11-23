package com.example.musikku

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.musikku.data.worker.DownloadSongWorker
import com.example.musikku.di.AppContainer

class MusikKuApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val downloadChannel = NotificationChannel(
                DownloadSongWorker.CHANNEL_ID,
                DownloadSongWorker.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Menampilkan progres dan status unduhan lagu"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(downloadChannel)
        }
    }
}

/** Extension untuk akses AppContainer dari Context mana pun. */
val android.content.Context.appContainer: AppContainer
    get() = (applicationContext as MusikKuApp).container
