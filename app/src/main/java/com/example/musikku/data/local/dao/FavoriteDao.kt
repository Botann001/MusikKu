package com.example.musikku.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.musikku.data.local.entity.FavoriteEntity
import com.example.musikku.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    /** Ambil semua lagu favorit diurutkan dari yang paling baru ditambahkan. */
    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN favorites f ON s.id = f.songId
        ORDER BY f.addedAt DESC
    """)
    fun getFavoriteSongs(): Flow<List<SongEntity>>

    /** Ambil semua ID lagu favorit sebagai Flow untuk lookup cepat di UI. */
    @Query("SELECT songId FROM favorites")
    fun getFavoriteSongIds(): Flow<List<Long>>

    /** Cek apakah satu lagu berstatus favorit. */
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId = :songId)")
    fun isFavorite(songId: Long): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun removeFavorite(songId: Long)
}
