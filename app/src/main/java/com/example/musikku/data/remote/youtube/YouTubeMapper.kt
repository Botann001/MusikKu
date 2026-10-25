package com.example.musikku.data.remote.youtube

import com.example.musikku.data.local.entity.SongEntity

/**
 * Konversi YouTubeSearchItemDto ke SongEntity milik MusikKu.
 * contentUri akan diisi dengan URL audio stream langsung googlevideo/invidious.
 */
val YouTubeSearchItemDto.songId: Long
    get() = (videoId.hashCode().toLong().let { if (it < 0) -it else it })

fun YouTubeSearchItemDto.toSongEntity(streamUrl: String = ""): SongEntity {
    return SongEntity(
        id = songId,
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
        filePath = videoId
    )
}

/**
 * Konversi balik dari SongEntity ke YouTubeSearchItemDto untuk keperluan resolusi stream dan download.
 */
fun SongEntity.toYouTubeSearchItemDto(): YouTubeSearchItemDto {
    val vid = filePath?.removePrefix("yt:").orEmpty()
    return YouTubeSearchItemDto(
        videoId = vid,
        title = title,
        author = artist,
        lengthSeconds = duration / 1000L
    )
}
