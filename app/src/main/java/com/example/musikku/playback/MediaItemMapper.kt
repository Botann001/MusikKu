package com.example.musikku.playback

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.example.musikku.data.local.entity.SongEntity

/**
 * Konversi SongEntity ke MediaItem milik Media3.
 * Metadata seperti judul, artis, album, dan artworkUri diisi agar
 * notifikasi sistem Media3 dan pemutar dapat menampilkan informasi lengkap.
 */
fun SongEntity.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(contentUri.toUri())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(albumArtUri.toUri())
                .build()
        )
        .build()
}
