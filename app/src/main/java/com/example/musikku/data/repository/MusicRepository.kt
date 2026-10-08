package com.example.musikku.data.repository

import com.example.musikku.data.local.dao.FavoriteDao
import com.example.musikku.data.local.dao.PlaylistDao
import com.example.musikku.data.local.dao.PlaylistWithCount
import com.example.musikku.data.local.dao.SongDao
import com.example.musikku.data.local.entity.FavoriteEntity
import com.example.musikku.data.local.entity.PlaylistEntity
import com.example.musikku.data.local.entity.PlaylistSongEntity
import com.example.musikku.data.local.entity.SongEntity
import com.example.musikku.data.scanner.MusicScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Single source of truth untuk data lagu, favorit, dan playlist.
 * Menggabungkan MusicScanner (MediaStore) → Room (cache).
 */
class MusicRepository(
    private val songDao: SongDao,
    private val favoriteDao: FavoriteDao,
    private val playlistDao: PlaylistDao,
    private val musicScanner: MusicScanner
) {

    /** Reactive stream semua lagu dari Room. */
    val allSongs: Flow<List<SongEntity>> = songDao.getAllSongs()

    /** Cari lagu berdasarkan judul, artis, atau album. */
    fun searchSongs(query: String): Flow<List<SongEntity>> = songDao.searchSongs(query)

    /**
     * Scan MediaStore → simpan ke Room → hapus entri yang sudah tidak ada.
     * Dijalankan di Dispatchers.IO karena query MediaStore bisa lambat.
     */
    suspend fun refreshFromDevice() = withContext(Dispatchers.IO) {
        val scanned = musicScanner.scanAll()
        // Ambil daftar favorit dan custom album art yang ada agar tidak terhapus saat rescan
        val currentFavIds = songDao.getFavoriteSongIds().first().toSet()
        val existingSongs = songDao.getAllSongs().first().associateBy { it.id }
        val songsToInsert = scanned.map { song ->
            val existing = existingSongs[song.id]
            var updated = if (currentFavIds.contains(song.id)) song.copy(isFavorite = true) else song
            if (existing != null && existing.albumArtUri.startsWith("file://")) {
                updated = updated.copy(albumArtUri = existing.albumArtUri)
            }
            updated
        }
        songDao.insertAll(songsToInsert)
        // Hapus baris lama yang file-nya sudah dihapus user
        val validIds = scanned.map { it.id }
        songDao.deleteStale(validIds)
    }

    /** Update URI album cover lagu di database. */
    suspend fun updateSongAlbumArt(songId: Long, albumArtUri: String) = withContext(Dispatchers.IO) {
        songDao.updateAlbumArt(songId, albumArtUri)
    }

    // ─── Favorit ────────────────────────────────────────────────────────────────

    val favoriteSongs: Flow<List<SongEntity>> = songDao.getFavoriteSongs()

    val favoriteSongIds: Flow<List<Long>> = songDao.getFavoriteSongIds()

    fun isFavorite(songId: Long): Flow<Boolean> =
        songDao.isFavorite(songId).map { it == true }

    suspend fun toggleFavorite(songId: Long) = withContext(Dispatchers.IO) {
        val isFav = songDao.getFavoriteSongIds().first().contains(songId)
        val newStatus = !isFav
        // Perbarui flag favorit di tabel songs
        songDao.updateFavorite(songId, newStatus)
        // Sinkronkan juga ke tabel favorites untuk konsistensi relasional
        if (newStatus) {
            favoriteDao.addFavorite(FavoriteEntity(songId = songId))
        } else {
            favoriteDao.removeFavorite(songId)
        }
    }

    // ─── Playlist ───────────────────────────────────────────────────────────────

    val playlistsWithCount: Flow<List<PlaylistWithCount>> = playlistDao.getPlaylistsWithCount()

    fun getSongsForPlaylist(playlistId: Long): Flow<List<SongEntity>> =
        playlistDao.getSongsForPlaylist(playlistId)

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylist(PlaylistEntity(name = name.trim()))
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) = withContext(Dispatchers.IO) {
        playlistDao.renamePlaylist(playlistId, newName.trim())
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylistCascade(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        val nextOrder = playlistDao.getNextOrderIndex(playlistId)
        playlistDao.addSongToPlaylist(
            PlaylistSongEntity(
                playlistId = playlistId,
                songId = songId,
                orderIndex = nextOrder
            )
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }
}

/** Alias SongRepository untuk kesesuaian dengan penamaan spesifikasi */
typealias SongRepository = MusicRepository
