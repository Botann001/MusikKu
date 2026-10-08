package com.example.musikku.data.scanner

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.musikku.data.local.entity.SongEntity

/**
 * Membaca semua file audio dari MediaStore.
 * Hanya mengambil musik yang valid, menyaring file rekaman, audio WhatsApp,
 * Telegram, notifikasi, dan nada dering.
 */
class MusicScanner(private val context: Context) {

    companion object {
        // Durasi minimum 15 detik untuk menghindari SFX dan notifikasi pendek
        private const val MIN_DURATION_MS = 15_000L

        // Folder non-musik yang harus diabaikan dari pustaka lagu
        private val EXCLUDED_PATH_KEYWORDS = listOf(
            "whatsapp",
            "com.whatsapp",
            "telegram",
            "org.telegram",
            "voice recorder",
            "voicenote",
            "voice note",
            "sound_recorder",
            "call_recordings",
            "callrecordings",
            "recordings",
            "notifications",
            "ringtones",
            "alarms"
        )
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
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            @Suppress("DEPRECATION")
            MediaStore.Audio.Media.DATA
        )

        // Filter: hanya musik, durasi >= 15 detik
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
            val displayNameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
            @Suppress("DEPRECATION")
            val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val title = cursor.getString(titleCol) ?: "Unknown"
                val duration = cursor.getLong(durationCol)
                val filePath = if (dataCol >= 0) cursor.getString(dataCol) else null
                val displayName = if (displayNameCol >= 0) cursor.getString(displayNameCol) else null

                // Abaikan audio chat WhatsApp, Telegram, rekaman suara, dll.
                if (isExcluded(title, displayName, filePath, duration)) {
                    continue
                }

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
                    title = title,
                    artist = cursor.getString(artistCol) ?: "Unknown Artist",
                    album = cursor.getString(albumCol) ?: "Unknown Album",
                    duration = duration,
                    contentUri = contentUri,
                    albumArtUri = albumArtUri,
                    dateAdded = cursor.getLong(dateAddedCol),
                    size = cursor.getLong(sizeCol),
                    filePath = filePath
                )
            }
        }
        return songs
    }

    /**
     * Memeriksa apakah file audio merupakan pesan suara WhatsApp, Telegram,
     * rekaman telepon, nada dering, atau file non-musik lainnya.
     */
    private fun isExcluded(title: String, displayName: String?, filePath: String?, duration: Long): Boolean {
        if (duration < MIN_DURATION_MS) return true

        val lowerPath = (filePath ?: "").lowercase()
        val lowerTitle = title.lowercase()
        val lowerName = (displayName ?: "").lowercase()

        // 1. Abaikan path folder aplikasi pesan dan rekaman
        if (EXCLUDED_PATH_KEYWORDS.any { lowerPath.contains(it) }) {
            return true
        }

        // 2. Abaikan pola penamaan berkas WhatsApp: AUD-YYYYMMDD-WA... atau PTT-YYYYMMDD-WA...
        if (lowerTitle.startsWith("aud-") && lowerTitle.contains("-wa")) return true
        if (lowerTitle.startsWith("ptt-") && lowerTitle.contains("-wa")) return true
        if (lowerName.startsWith("aud-") && lowerName.contains("-wa")) return true
        if (lowerName.startsWith("ptt-") && lowerName.contains("-wa")) return true

        // 3. Abaikan format recorder umum
        if (lowerTitle.startsWith("rec_") || lowerTitle.startsWith("voice_") || lowerTitle.startsWith("call_")) return true
        if (lowerName.startsWith("rec_") || lowerName.startsWith("voice_") || lowerName.startsWith("call_")) return true

        return false
    }
}
