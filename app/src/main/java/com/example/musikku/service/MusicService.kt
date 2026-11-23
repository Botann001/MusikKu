package com.example.musikku.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.musikku.MainActivity
import com.example.musikku.appContainer
import com.example.musikku.playback.CoilBitmapLoader
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Service latar belakang yang mengelola pemutaran musik dan MediaSession.
 * Notifikasi sistem kustom bertema MusikKu (aksen hijau emerald #1DB954, 5 tombol kontrol lengkap,
 * dan artwork cover album berkualitas tinggi via Coil).
 */
class MusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "musikku_playback_channel"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val repository = appContainer.musicRepository

        // Provider notifikasi kustom MusikKu
        val notificationProvider = MusikKuNotificationProvider(
            context = this,
            isFavoriteChecker = { songId -> repository.isFavoriteCached(songId) }
        )
        setMediaNotificationProvider(notificationProvider)

        // Konfigurasi ExoPlayer: MediaSourceFactory dengan SimpleCache, audio focus, & becoming noisy
        val mediaSourceFactory = com.example.musikku.playback.MusicCacheManager.createMediaSourceFactory(this)
        val exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        val musikKuPlayer = MusikKuPlayer(exoPlayer)

        // Membuka MainActivity saat notifikasi diketuk
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val coilBitmapLoader = CoilBitmapLoader(this)

        val callback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(SessionCommand(MusikKuNotificationProvider.CUSTOM_ACTION_TOGGLE_FAVORITE, Bundle.EMPTY))
                    .add(SessionCommand(MusikKuNotificationProvider.CUSTOM_ACTION_TOGGLE_SHUFFLE, Bundle.EMPTY))
                    .build()
                val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()

                val customLayout = MusikKuNotificationProvider.buildCustomLayout(
                    session,
                    isFavoriteChecker = { songId -> repository.isFavoriteCached(songId) }
                )

                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(sessionCommands)
                    .setAvailablePlayerCommands(playerCommands)
                    .setCustomLayout(customLayout)
                    .build()
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                when (customCommand.customAction) {
                    MusikKuNotificationProvider.CUSTOM_ACTION_TOGGLE_FAVORITE -> {
                        val currentItem = session.player.currentMediaItem
                        val songId = currentItem?.mediaId?.toLongOrNull()
                        val currentSong = appContainer.musicController.playbackState.value.currentSong
                        if (songId != null && songId > 0) {
                            serviceScope.launch {
                                repository.toggleFavorite(songId, fallbackSong = currentSong)
                                updateCustomLayoutAndNotification()
                            }
                        }
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    MusikKuNotificationProvider.CUSTOM_ACTION_TOGGLE_SHUFFLE -> {
                        val currentShuffle = session.player.shuffleModeEnabled
                        session.player.shuffleModeEnabled = !currentShuffle
                        updateCustomLayoutAndNotification()
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                }
                return super.onCustomCommand(session, controller, customCommand, args)
            }
        }

        mediaSession = MediaSession.Builder(this, musikKuPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .setBitmapLoader(coilBitmapLoader)
            .setCallback(callback)
            .build()

        // Listener untuk memperbarui layout tombol saat status player berubah
        exoPlayer.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                updateCustomLayoutAndNotification()
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                updateCustomLayoutAndNotification()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateCustomLayoutAndNotification()
            }
        })

        // Pantau perubahan status favorit di repository secara reaktif
        serviceScope.launch {
            repository.favoriteSongIds.collect {
                updateCustomLayoutAndNotification()
            }
        }
    }

    private fun updateCustomLayoutAndNotification() {
        val session = mediaSession ?: return
        val customLayout = MusikKuNotificationProvider.buildCustomLayout(
            session,
            isFavoriteChecker = { songId -> appContainer.musicRepository.isFavoriteCached(songId) }
        )
        session.setCustomLayout(customLayout)
        triggerNotificationUpdate()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        // Hentikan service jika musik sedang tidak diputar saat app ditutup dari recent apps
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
