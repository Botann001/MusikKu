package com.example.musikku.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.musikku.data.local.entity.DailyListeningStatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ListeningStatsDao {

    @Query("SELECT COALESCE(SUM(seconds), 0) FROM daily_listening_stats WHERE date >= :startDate AND date <= :endDate")
    fun getListeningSecondsBetween(startDate: String, endDate: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(seconds), 0) FROM daily_listening_stats WHERE date >= :startDate AND date <= :endDate")
    suspend fun getListeningSecondsBetweenSync(startDate: String, endDate: String): Long

    @Query("SELECT COALESCE(SUM(seconds), 0) FROM daily_listening_stats")
    fun getTotalListeningSeconds(): Flow<Long>

    @Query("SELECT seconds FROM daily_listening_stats WHERE date = :date")
    suspend fun getSecondsForDate(date: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stat: DailyListeningStatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stats: List<DailyListeningStatEntity>)

    @Query("SELECT COUNT(*) FROM daily_listening_stats")
    suspend fun getRecordCount(): Int
}
