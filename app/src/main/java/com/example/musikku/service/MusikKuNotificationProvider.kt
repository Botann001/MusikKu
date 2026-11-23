package com.example.musikku.service

import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import com.example.musikku.R
import com.google.common.collect.ImmutableList

/**
 * Provider notifikasi kustom MusikKu.
 * Menerapkan tema warna aksen hijau emerald (#1DB954), tata letak kontrol 5 tombol lengkap
 * (❤️ Suka, ⏮️ Sebelumnya, ⏯️ Putar/Jeda, ⏭️ Selanjutnya, 🔀 Acak),
 * serta memastikan tombol Selanjutnya selalu tersedia dan aktif.
 */
@OptIn(UnstableApi::class)
class MusikKuNotificationProvider(
    private val context: Context,
    private val isFavoriteChecker: (Long) -> Boolean
) : DefaultMediaNotificationProvider(
    context,
    { DEFAULT_NOTIFICATION_ID },
    MusicService.NOTIFICATION_CHANNEL_ID,
    R.string.playback_channel_name
) {

    companion object {
        const val CUSTOM_ACTION_TOGGLE_FAVORITE = "com.example.musikku.ACTION_TOGGLE_FAVORITE"
        const val CUSTOM_ACTION_TOGGLE_SHUFFLE = "com.example.musikku.ACTION_TOGGLE_SHUFFLE"

        /** Aksen warna hijau MusikKu (#1DB954) */
        const val ACCENT_COLOR = 0xFF1DB954.toInt()

        /**
         * Membangun daftar tombol custom untuk MediaSession.setCustomLayout (Android 13/14 SystemUI).
         */
        fun buildCustomLayout(
            session: MediaSession,
            isFavoriteChecker: (Long) -> Boolean
        ): ImmutableList<CommandButton> {
            val player = session.player
            val currentSongId = player.currentMediaItem?.mediaId?.toLongOrNull() ?: -1L
            val isFav = if (currentSongId > 0) isFavoriteChecker(currentSongId) else false
            val isShuffle = player.shuffleModeEnabled

            val favIcon = if (isFav) {
                androidx.media3.session.R.drawable.media3_icon_heart_filled
            } else {
                androidx.media3.session.R.drawable.media3_icon_heart_unfilled
            }
            val favTitle = if (isFav) "Hapus dari Favorit" else "Tambah ke Favorit"

            val favButton = CommandButton.Builder()
                .setSessionCommand(SessionCommand(CUSTOM_ACTION_TOGGLE_FAVORITE, Bundle.EMPTY))
                .setIconResId(favIcon)
                .setDisplayName(favTitle)
                .setEnabled(currentSongId > 0)
                .build()

            val shuffleIcon = if (isShuffle) {
                androidx.media3.session.R.drawable.media3_icon_shuffle_on
            } else {
                androidx.media3.session.R.drawable.media3_icon_shuffle_off
            }
            val shuffleTitle = if (isShuffle) "Acak: Aktif" else "Acak: Nonaktif"

            val shuffleButton = CommandButton.Builder()
                .setSessionCommand(SessionCommand(CUSTOM_ACTION_TOGGLE_SHUFFLE, Bundle.EMPTY))
                .setIconResId(shuffleIcon)
                .setDisplayName(shuffleTitle)
                .setEnabled(true)
                .build()

            return ImmutableList.of(favButton, shuffleButton)
        }
    }

    override fun getMediaButtons(
        session: MediaSession,
        playerCommands: Player.Commands,
        customLayout: ImmutableList<CommandButton>,
        showPauseButton: Boolean
    ): ImmutableList<CommandButton> {
        val player = session.player
        val currentSongId = player.currentMediaItem?.mediaId?.toLongOrNull() ?: -1L
        val isFav = if (currentSongId > 0) isFavoriteChecker(currentSongId) else false
        val isShuffle = player.shuffleModeEnabled

        val builder = ImmutableList.builder<CommandButton>()

        // 1. Tombol Suka / Favorit
        val favIcon = if (isFav) {
            androidx.media3.session.R.drawable.media3_icon_heart_filled
        } else {
            androidx.media3.session.R.drawable.media3_icon_heart_unfilled
        }
        val favTitle = if (isFav) "Hapus dari Favorit" else "Tambah ke Favorit"
        builder.add(
            CommandButton.Builder()
                .setSessionCommand(SessionCommand(CUSTOM_ACTION_TOGGLE_FAVORITE, Bundle.EMPTY))
                .setIconResId(favIcon)
                .setDisplayName(favTitle)
                .setEnabled(currentSongId > 0)
                .build()
        )

        // 2. Tombol Sebelumnya (Previous)
        builder.add(
            CommandButton.Builder()
                .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS)
                .setIconResId(androidx.media3.session.R.drawable.media3_icon_previous)
                .setDisplayName("Sebelumnya")
                .setEnabled(true)
                .build()
        )

        // 3. Tombol Putar / Jeda (Play/Pause)
        if (showPauseButton) {
            builder.add(
                CommandButton.Builder()
                    .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
                    .setIconResId(androidx.media3.session.R.drawable.media3_icon_pause)
                    .setDisplayName("Jeda")
                    .setEnabled(true)
                    .build()
            )
        } else {
            builder.add(
                CommandButton.Builder()
                    .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
                    .setIconResId(androidx.media3.session.R.drawable.media3_icon_play)
                    .setDisplayName("Putar")
                    .setEnabled(true)
                    .build()
            )
        }

        // 4. Tombol Selanjutnya (Next) - SELALU AKTIF
        builder.add(
            CommandButton.Builder()
                .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT)
                .setIconResId(androidx.media3.session.R.drawable.media3_icon_next)
                .setDisplayName("Selanjutnya")
                .setEnabled(true)
                .build()
        )

        // 5. Tombol Acak (Shuffle)
        val shuffleIcon = if (isShuffle) {
            androidx.media3.session.R.drawable.media3_icon_shuffle_on
        } else {
            androidx.media3.session.R.drawable.media3_icon_shuffle_off
        }
        val shuffleTitle = if (isShuffle) "Acak: Aktif" else "Acak: Nonaktif"
        builder.add(
            CommandButton.Builder()
                .setSessionCommand(SessionCommand(CUSTOM_ACTION_TOGGLE_SHUFFLE, Bundle.EMPTY))
                .setIconResId(shuffleIcon)
                .setDisplayName(shuffleTitle)
                .setEnabled(true)
                .build()
        )

        return builder.build()
    }

    override fun addNotificationActions(
        session: MediaSession,
        mediaButtons: ImmutableList<CommandButton>,
        builder: NotificationCompat.Builder,
        actionFactory: MediaNotification.ActionFactory
    ): IntArray {
        // Terapkan tema warna MusikKu dan ikon notifikasi
        builder.setColor(ACCENT_COLOR)
        builder.setColorized(true)
        builder.setSmallIcon(R.mipmap.ic_launcher)

        super.addNotificationActions(session, mediaButtons, builder, actionFactory)

        // Di compact view (tampilan ringkas notifikasi / lockscreen), tampilkan Previous (1), Play/Pause (2), Next (3)
        return if (mediaButtons.size >= 4) {
            intArrayOf(1, 2, 3)
        } else {
            intArrayOf(0, 1, 2)
        }
    }
}
