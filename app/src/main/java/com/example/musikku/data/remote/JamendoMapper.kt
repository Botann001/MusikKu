package com.example.musikku.data.remote

import com.example.musikku.data.local.entity.SongEntity
import com.example.musikku.data.remote.dto.JamendoTrackDto

/**
 * Konversi lagu dari Jamendo ke SongEntity.
 * contentUri berisi URL streaming MP3 online yang langsung dapat diputar oleh ExoPlayer.
 */
fun JamendoTrackDto.toSongEntity(): SongEntity {
    val safeId = id.toLongOrNull() ?: (id.hashCode().toLong().let { if (it < 0) -it else it })
    return SongEntity(
        id = safeId,
        title = name,
        artist = artistName ?: "Artis Jamendo",
        album = albumName ?: "Jamendo Music",
        duration = duration * 1000L,
        contentUri = audio ?: "",
        albumArtUri = albumImage ?: image ?: "",
        dateAdded = System.currentTimeMillis() / 1000,
        size = 0L,
        source = "JAMENDO",
        isFavorite = false,
        filePath = null
    )
}
