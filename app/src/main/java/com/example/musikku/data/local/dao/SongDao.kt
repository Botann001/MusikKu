package com.example.musikku.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.musikku.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    /** Ambil semua lagu, urut berdasarkan judul A-Z. */
    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    /** Cari lagu berdasarkan judul, artis, atau album. */
    @Query(
        """
        SELECT * FROM songs
        WHERE title LIKE '%' || :query || '%'
           OR artist LIKE '%' || :query || '%'
           OR album LIKE '%' || :query || '%'
        ORDER BY title COLLATE NOCASE ASC
        """
    )
    fun searchSongs(query: String): Flow<List<SongEntity>>

    /**
     * Sisipkan / ganti semua lagu sekaligus.
     * REPLACE strategy agar data selalu up-to-date setelah rescan.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<SongEntity>)

    /** Hapus lagu lokal yang ID-nya tidak ada di daftar terbaru (file sudah dihapus dari penyimpanan perangkat). */
    @Query("DELETE FROM songs WHERE source = 'LOCAL' AND id NOT IN (:validIds)")
    suspend fun deleteStale(validIds: List<Long>)

    /** Hapus lagu spesifik berdasarkan ID (misalnya saat pengguna menghapus lagu unduhan). */
    @Query("DELETE FROM songs WHERE id = :songId")
    suspend fun deleteSongById(songId: Long)

    /** Ambil satu entitas lagu berdasarkan ID. */
    @Query("SELECT * FROM songs WHERE id = :songId LIMIT 1")
    suspend fun getSongById(songId: Long): SongEntity?

    /** Ambil semua lagu hasil unduhan dari Jamendo. */
    @Query("SELECT * FROM songs WHERE source = 'DOWNLOADED' ORDER BY dateAdded DESC")
    fun getDownloadedSongs(): Flow<List<SongEntity>>

    /** Ambil ID semua lagu hasil unduhan dari Jamendo. */
    @Query("SELECT id FROM songs WHERE source = 'DOWNLOADED'")
    fun getDownloadedSongIds(): Flow<List<Long>>

    /** Update flag isFavorite lagu. */
    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :songId")
    suspend fun updateFavorite(songId: Long, isFavorite: Boolean)

    /** Ambil semua lagu favorit langsung dari tabel songs berdasarkan flag isFavorite. */
    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun getFavoriteSongs(): Flow<List<SongEntity>>

    /** Ambil semua ID lagu favorit. */
    @Query("SELECT id FROM songs WHERE isFavorite = 1")
    fun getFavoriteSongIds(): Flow<List<Long>>

    /** Cek status favorit lagu. */
    @Query("SELECT isFavorite FROM songs WHERE id = :songId")
    fun isFavorite(songId: Long): Flow<Boolean?>

    /** Update URI cover album lagu. */
    @Query("UPDATE songs SET albumArtUri = :albumArtUri WHERE id = :songId")
    suspend fun updateAlbumArt(songId: Long, albumArtUri: String)
}
