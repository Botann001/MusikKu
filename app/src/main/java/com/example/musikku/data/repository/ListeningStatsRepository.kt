package com.example.musikku.data.repository

import com.example.musikku.data.local.dao.ListeningStatsDao
import com.example.musikku.data.local.entity.DailyListeningStatEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Data waktu mendengarkan musik:
 * - [weeklySeconds]: Durasi mendengarkan musik minggu ini (Senin - Minggu).
 * - [monthlySeconds]: Durasi mendengarkan musik bulan ini (1 s.d. akhir bulan).
 * - [yearlySeconds]: Durasi mendengarkan musik tahun ini (1 Januari s.d. 31 Desember).
 * - [totalSeconds]: Total keseluruhan waktu mendengarkan.
 */
data class ListeningTimeStats(
    val weeklySeconds: Long = 0L,
    val monthlySeconds: Long = 0L,
    val yearlySeconds: Long = 0L,
    val totalSeconds: Long = 0L
) {
    /** Format dengan detik untuk durasi di bawah 1 jam (misal: "6 menit 42 detik") agar kenaikan detik terlihat live */
    val weeklyFormatted: String get() = formatDuration(weeklySeconds, includeSeconds = true)

    /** Format ringkas tanpa detik untuk menu drawer samping */
    val weeklyFormattedCompact: String get() = formatDuration(weeklySeconds, includeSeconds = false)

    val monthlyFormatted: String get() = formatDuration(monthlySeconds, includeSeconds = false)
    val yearlyFormatted: String get() = formatDuration(yearlySeconds, includeSeconds = false)
    val dailyAverageFormatted: String get() = formatDuration((weeklySeconds / 7L).coerceAtLeast(0L), includeSeconds = true)
    val totalFormatted: String get() = formatDuration(totalSeconds, includeSeconds = false)

    companion object {
        fun formatDuration(seconds: Long, includeSeconds: Boolean = false): String {
            if (seconds <= 0L) return if (includeSeconds) "0 detik" else "0 menit"
            val totalMinutes = seconds / 60L
            val remainingSeconds = seconds % 60L

            if (totalMinutes < 1L) return "$seconds detik"

            if (totalMinutes < 60L) {
                return if (includeSeconds && remainingSeconds > 0L) {
                    "$totalMinutes menit $remainingSeconds detik"
                } else {
                    "$totalMinutes menit"
                }
            }

            val hours = totalMinutes / 60L
            val remainingMinutes = totalMinutes % 60L
            return if (remainingMinutes > 0L) {
                "$hours jam $remainingMinutes menit"
            } else {
                "$hours jam"
            }
        }
    }
}

class ListeningStatsRepository(
    private val statsDao: ListeningStatsDao,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val _liveStats = MutableStateFlow(ListeningTimeStats())
    val liveStats: StateFlow<ListeningTimeStats> = _liveStats.asStateFlow()

    init {
        externalScope.launch {
            refreshStats()
        }
    }

    /**
     * Tambahkan detik pemutaran musik ke hari ini.
     * Dipanggil secara kontinu oleh MusicController saat lagu sedang diputar.
     */
    fun addListeningSeconds(seconds: Long) {
        if (seconds <= 0L) return
        externalScope.launch {
            val today = getTodayString()
            val current = statsDao.getSecondsForDate(today) ?: 0L
            val updated = current + seconds
            statsDao.insertOrUpdate(DailyListeningStatEntity(today, updated))
            refreshStats()
        }
    }

    /**
     * Hitung ulang agregasi perminggu, perbulan, dan pertahun.
     */
    suspend fun refreshStats() {
        val weekRange = getThisWeekRange()
        val monthRange = getThisMonthRange()
        val yearRange = getThisYearRange()

        val weekly = statsDao.getListeningSecondsBetweenSync(weekRange.first, weekRange.second)
        val monthly = statsDao.getListeningSecondsBetweenSync(monthRange.first, monthRange.second)
        val yearly = statsDao.getListeningSecondsBetweenSync(yearRange.first, yearRange.second)
        val total = weekly.coerceAtLeast(monthly.coerceAtLeast(yearly))

        _liveStats.value = ListeningTimeStats(
            weeklySeconds = weekly,
            monthlySeconds = monthly,
            yearlySeconds = yearly,
            totalSeconds = total
        )
    }

    private fun getTodayString(): String = dateFormat.format(Date())

    /** Rentang minggu ini: Senin s.d. Minggu */
    fun getThisWeekRange(): Pair<String, String> {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val start = dateFormat.format(cal.time)
        cal.add(Calendar.DAY_OF_WEEK, 6)
        val end = dateFormat.format(cal.time)
        return Pair(start, end)
    }

    /** Rentang bulan ini: Tanggal 1 s.d. akhir bulan */
    fun getThisMonthRange(): Pair<String, String> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val start = dateFormat.format(cal.time)
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        val end = dateFormat.format(cal.time)
        return Pair(start, end)
    }

    /** Rentang tahun ini: 1 Januari s.d. 31 Desember */
    fun getThisYearRange(): Pair<String, String> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_YEAR, 1)
        val start = dateFormat.format(cal.time)
        cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR))
        val end = dateFormat.format(cal.time)
        return Pair(start, end)
    }
}
