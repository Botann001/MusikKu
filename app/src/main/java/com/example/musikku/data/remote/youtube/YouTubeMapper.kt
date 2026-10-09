package com.example.musikku.data.remote.youtube

import com.example.musikku.data.local.entity.SongEntity

/**
 * Konversi YouTubeSearchItemDto ke SongEntity milik MusikKu.
 * contentUri akan diisi dengan URL audio stream langsung googlevideo/invidious.
 */
fun YouTubeSearchItemDto.toSongEntity(streamUrl: String = ""): SongEntity {
    val safeId = (videoId.hashCode().toLong().let { if (it < 0) -it else it })
    return SongEntity(
        id = safeId,
        title = title.ifBlank { "Lagu YouTube" },
        artist = author?.ifBlank { "YouTube Music" } ?: "YouTube Music",
        album = "YouTube Music",
        duration = lengthSeconds * 1000L,
        contentUri = streamUrl,
        albumArtUri = thumbnailUrl,
        dateAdded = System.currentTimeMillis() / 1000,
        size = 0L,
        source = "YOUTUBE",
        isFavorite = false,
        filePath = null
    )
}
