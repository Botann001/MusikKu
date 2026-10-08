package com.example.musikku.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entitas Room yang merepresentasikan satu file musik di perangkat.
 * contentUri disimpan sebagai String karena Room tidak mendukung android.net.Uri secara langsung.
 */
@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey
    val id: Long,                // MediaStore._ID
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,          // milidetik
    val contentUri: String,      // content://media/external/audio/media/<id>
    val albumArtUri: String,     // content://media/external/audio/albumart/<albumId>
    val dateAdded: Long,         // epoch second
    val size: Long = 0L,         // byte
    val source: String = "LOCAL", // sumber lagu ("LOCAL", "DOWNLOADED", atau "JAMENDO")
    val isFavorite: Boolean = false, // flag favorit lagu
    val filePath: String? = null // path absolut file lokal jika diunduh
) {
    val durationMs: Long get() = duration
    val uri: String get() = contentUri
}

/** Alias Song untuk kemudahan referensi sesuai penamaan spesifikasi */
typealias Song = SongEntity
