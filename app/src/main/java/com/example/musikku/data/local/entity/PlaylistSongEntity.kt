package com.example.musikku.data.local.entity

import androidx.room.Entity

/**
 * Tabel relasi Many-to-Many antara Playlist dan Lagu.
 */
@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"]
)
data class PlaylistSongEntity(
    val playlistId: Long,
    val songId: Long,
    val orderIndex: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

/** Alias PlaylistSong sesuai penamaan spesifikasi */
typealias PlaylistSong = PlaylistSongEntity
