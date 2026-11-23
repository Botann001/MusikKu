package com.example.musikku.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entitas penyimpan durasi pemutaran musik per hari (detik).
 * Format [date]: "yyyy-MM-dd" (contoh: "2026-11-16").
 */
@Entity(tableName = "daily_listening_stats")
data class DailyListeningStatEntity(
    @PrimaryKey
    val date: String,
    val seconds: Long
)
