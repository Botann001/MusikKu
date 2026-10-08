package com.example.musikku.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entitas lagu favorit. Terpisah dari SongEntity agar tidak tertimpa saat rescan MediaStore.
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val songId: Long,
    val addedAt: Long = System.currentTimeMillis()
)
