package com.example.musikku.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entitas playlist buatan pengguna.
 */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

/** Alias Playlist sesuai penamaan spesifikasi */
typealias Playlist = PlaylistEntity
