package com.example.musikku.service

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player

/**
 * ForwardingPlayer kustom yang memastikan perintah Previous & Next selalu aktif
 * di sistem (SystemUI Android 13/14 dan DefaultMediaNotificationProvider),
 * serta menangani looping saat berada di ujung playlist atau jika playlist hanya berisi 1 lagu.
 */
class MusikKuPlayer(player: Player) : ForwardingPlayer(player) {

    override fun getAvailableCommands(): Player.Commands {
        return super.getAvailableCommands().buildUpon()
            .add(Player.COMMAND_SEEK_TO_NEXT)
            .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .build()
    }

    override fun isCommandAvailable(command: Int): Boolean {
        if (command == Player.COMMAND_SEEK_TO_NEXT ||
            command == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM ||
            command == Player.COMMAND_SEEK_TO_PREVIOUS ||
            command == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
        ) {
            return true
        }
        return super.isCommandAvailable(command)
    }

    override fun seekToNext() {
        if (hasNextMediaItem()) {
            super.seekToNext()
        } else if (mediaItemCount > 0) {
            seekToDefaultPosition(0)
            play()
        }
    }

    override fun seekToNextMediaItem() {
        if (hasNextMediaItem()) {
            super.seekToNextMediaItem()
        } else if (mediaItemCount > 0) {
            seekToDefaultPosition(0)
            play()
        }
    }

    override fun seekToPrevious() {
        if (hasPreviousMediaItem()) {
            super.seekToPrevious()
        } else if (mediaItemCount > 0) {
            if (currentPosition > 3000L) {
                seekTo(0)
            } else {
                seekToDefaultPosition(mediaItemCount - 1)
            }
            play()
        }
    }

    override fun seekToPreviousMediaItem() {
        if (hasPreviousMediaItem()) {
            super.seekToPreviousMediaItem()
        } else if (mediaItemCount > 0) {
            seekToDefaultPosition(mediaItemCount - 1)
            play()
        }
    }
}
