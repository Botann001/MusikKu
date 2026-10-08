package com.example.musikku.data.scanner

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.musikku.data.local.entity.SongEntity

/**
 * Membaca semua file audio dari MediaStore.
 * Hanya mengambil musik (IS_MUSIC = 1), minimal durasi 5 detik untuk
 * menghindari nada dering dan notifikasi pendek.
 */
class MusicScanner(private val context: Context) {

    companion object {
        private const val MIN_DURATION_MS = 5_000L
    }

    fun scanAll(): List<SongEntity> {
        val songs = mutableListOf<SongEntity>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE
        )

        // Filter: hanya musik, durasi >= 5 detik
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} = 1 AND " +
                "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(MIN_DURATION_MS.toString())
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val collection = MediaStore.Audio.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL
        )

        context.contentResolver.query(
            collection, projection, selection, selectionArgs, sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val albumId = cursor.getLong(albumIdCol)

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                ).toString()

                // URI standar untuk album art
                val albumArtUri = ContentUris.withAppendedId(
                    "content://media/external/audio/albumart".let { android.net.Uri.parse(it) },
                    albumId
                ).toString()

                songs += SongEntity(
                    id = id,
                    title = cursor.getString(titleCol) ?: "Unknown",
                    artist = cursor.getString(artistCol) ?: "Unknown Artist",
                    album = cursor.getString(albumCol) ?: "Unknown Album",
                    duration = cursor.getLong(durationCol),
                    contentUri = contentUri,
                    albumArtUri = albumArtUri,
                    dateAdded = cursor.getLong(dateAddedCol),
                    size = cursor.getLong(sizeCol)
                )
            }
        }
        return songs
    }
}
