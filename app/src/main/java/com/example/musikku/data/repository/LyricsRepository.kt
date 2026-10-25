package com.example.musikku.data.repository

import android.util.Log
import com.example.musikku.data.remote.lyrics.LyricLine
import com.example.musikku.data.remote.lyrics.LrcLibApiService
import com.example.musikku.data.remote.lyrics.LrcLibTrackDto
import com.example.musikku.data.remote.lyrics.LyricsUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Repository untuk mengambil dan mem-parsing lirik berjalan sinkron (LRC) dari LRCLIB API.
 * Dilengkapi pembersih judul (title sanitizer) cerdas untuk YouTube Music dan file lokal,
 * serta caching in-memory agar tidak membebani jaringan.
 */
class LyricsRepository(
    private val apiService: LrcLibApiService
) {
    private val TAG = "LyricsRepository"

    // Cache in-memory berdasar query key "$title|$artist"
    private val memoryCache = ConcurrentHashMap<String, LyricsUiState>()

    private val NOISE_REGEX = Regex(
        """\((?:from|feat\.?|official|lyrical|video|audio|remix|mv|128k|320k|hd|4k|karaoke|full song)[^)]*\)|\[[^]]*]|\b(?:official (?:video|audio|music video)|lyrical|full song|4k video)\b""",
        RegexOption.IGNORE_CASE
    )

    private val STAMP_REGEX = Regex("""\[(\d{1,2}):(\d{2})[.:](\d{2,3})\]""")

    /**
     * Mengambil lirik untuk lagu yang sedang diputar.
     */
    suspend fun getLyrics(
        title: String,
        artist: String,
        durationMs: Long
    ): LyricsUiState = withContext(Dispatchers.IO) {
        val cleanTitle = cleanTitle(title)
        val cleanArtist = cleanArtist(artist)
        val cacheKey = "${cleanTitle.lowercase()}|${cleanArtist.lowercase()}"

        memoryCache[cacheKey]?.let { cached ->
            Log.d(TAG, "Cache hit lirik untuk: $cleanTitle - $cleanArtist")
            return@withContext cached
        }

        val seconds = if (durationMs > 0) (durationMs / 1000).toInt() else null

        // Cek apakah judul mengandung format "Artis - Judul" (sering terjadi di YouTube/MP3 lokal)
        var splitArtist: String? = null
        var splitTitle: String? = null
        if (cleanTitle.contains(" - ")) {
            val parts = cleanTitle.split(" - ", limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                splitArtist = parts[0].trim()
                splitTitle = cleanTitle(parts[1].trim())
            }
        }

        val effectiveArtist = when {
            cleanArtist.isNotBlank() -> cleanArtist
            splitArtist != null -> splitArtist
            else -> ""
        }
        val effectiveTitle = when {
            cleanArtist.isNotBlank() -> cleanTitle
            splitTitle != null -> splitTitle
            else -> cleanTitle
        }

        var matchDto: LrcLibTrackDto? = null

        // 1. Coba pencarian persis (exact match via /api/get)
        if (effectiveTitle.isNotBlank() && effectiveArtist.isNotBlank()) {
            matchDto = runCatching {
                apiService.getLyrics(
                    trackName = effectiveTitle,
                    artistName = effectiveArtist,
                    durationSeconds = seconds
                )
            }.getOrNull()
        }

        // 2. Jika gagal dan ada split candidate dari title, coba exact match lagi
        if (matchDto == null && splitTitle != null && splitArtist != null) {
            matchDto = runCatching {
                apiService.getLyrics(
                    trackName = splitTitle,
                    artistName = splitArtist,
                    durationSeconds = seconds
                )
            }.getOrNull()
        }

        // 3. Jika exact match belum berhasil, gunakan pencarian fuzzy (/api/search)
        if (matchDto == null) {
            val searchResults = runCatching {
                apiService.searchLyrics(
                    trackName = effectiveTitle,
                    artistName = effectiveArtist.ifBlank { null }
                )
            }.getOrNull() ?: emptyList()

            matchDto = pickBestSearchHit(searchResults, seconds)
        }

        // 4. Fallback terakhir: search query umum
        if (matchDto == null) {
            val generalQuery = if (effectiveArtist.isNotBlank()) "$effectiveArtist $effectiveTitle" else effectiveTitle
            val fallbackResults = runCatching {
                apiService.searchLyrics(query = generalQuery)
            }.getOrNull() ?: emptyList()

            matchDto = pickBestSearchHit(fallbackResults, seconds)
        }

        // Susun hasil
        val resultState: LyricsUiState = if (matchDto != null) {
            val syncedLrc = matchDto.syncedLyrics?.trim()
            val plainLrc = matchDto.plainLyrics?.trim()

            if (!syncedLrc.isNullOrBlank()) {
                val parsedLines = parseLrc(syncedLrc)
                if (parsedLines.isNotEmpty()) {
                    LyricsUiState.Success(
                        lines = parsedLines,
                        isSynced = true,
                        plainLyrics = plainLrc,
                        trackName = matchDto.trackName ?: effectiveTitle,
                        artistName = matchDto.artistName ?: effectiveArtist,
                        source = "LRCLIB"
                    )
                } else if (!plainLrc.isNullOrBlank()) {
                    val plainLines = plainLrc.lines().map { LyricLine(-1L, it) }
                    LyricsUiState.Success(
                        lines = plainLines,
                        isSynced = false,
                        plainLyrics = plainLrc,
                        trackName = matchDto.trackName ?: effectiveTitle,
                        artistName = matchDto.artistName ?: effectiveArtist,
                        source = "LRCLIB"
                    )
                } else {
                    LyricsUiState.Empty
                }
            } else if (!plainLrc.isNullOrBlank()) {
                val plainLines = plainLrc.lines().map { LyricLine(-1L, it) }
                LyricsUiState.Success(
                    lines = plainLines,
                    isSynced = false,
                    plainLyrics = plainLrc,
                    trackName = matchDto.trackName ?: effectiveTitle,
                    artistName = matchDto.artistName ?: effectiveArtist,
                    source = "LRCLIB"
                )
            } else {
                LyricsUiState.Empty
            }
        } else {
            LyricsUiState.Empty
        }

        memoryCache[cacheKey] = resultState
        Log.d(TAG, "Lirik berhasil diproses untuk '$effectiveTitle': status=${resultState::class.simpleName}")
        return@withContext resultState
    }

    /**
     * Memilih hasil pencarian terbaik yang memiliki lirik sinkron dan selisih durasi paling minim.
     */
    private fun pickBestSearchHit(hits: List<LrcLibTrackDto>, targetDurationSec: Int?): LrcLibTrackDto? {
        if (hits.isEmpty()) return null

        val withSynced = hits.filter { !it.syncedLyrics.isNullOrBlank() }
        if (withSynced.isNotEmpty()) {
            if (targetDurationSec != null && targetDurationSec > 0) {
                val closest = withSynced.minByOrNull { abs((it.duration ?: 0.0) - targetDurationSec) }
                if (closest != null) return closest
            }
            return withSynced.first()
        }

        val withPlain = hits.filter { !it.plainLyrics.isNullOrBlank() }
        if (withPlain.isNotEmpty()) {
            if (targetDurationSec != null && targetDurationSec > 0) {
                val closest = withPlain.minByOrNull { abs((it.duration ?: 0.0) - targetDurationSec) }
                if (closest != null) return closest
            }
            return withPlain.first()
        }

        return hits.firstOrNull()
    }

    /**
     * Parsing teks LRC bertanda waktu [mm:ss.xx] menjadi daftar [LyricLine].
     */
    fun parseLrc(lrc: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        for (rawLine in lrc.lines()) {
            val match = STAMP_REGEX.find(rawLine) ?: continue
            val (m, s, frac) = match.destructured
            val minutes = m.toLongOrNull() ?: continue
            val seconds = s.toLongOrNull() ?: continue
            val fractionMs = when (frac.length) {
                2 -> (frac.toLongOrNull() ?: 0L) * 10L
                3 -> frac.toLongOrNull() ?: 0L
                else -> 0L
            }
            val timeMs = minutes * 60_000L + seconds * 1_000L + fractionMs
            val body = rawLine.substring(match.range.last + 1)
                .replace(Regex("""<[^>]*>"""), "") // hapus kata-level stamp jika ada
                .trim()
            lines.add(LyricLine(timeMs = timeMs, text = body))
        }

        val sorted = lines.sortedBy { it.timeMs }
        // Jika intro cukup panjang (>= 3 detik) dan belum ada stamp 0, tambahkan titik awal kosong
        val first = sorted.firstOrNull()
        return if (first != null && first.timeMs >= 3000L) {
            listOf(LyricLine(0L, "•••")) + sorted
        } else {
            sorted
        }
    }

    /**
     * Membersihkan nama judul lagu dari karakter noise, tag audio/video, dan ekstensi file.
     */
    fun cleanTitle(raw: String): String {
        var s = raw.replace('_', ' ')
        // Hapus penomoran track di depan seperti "01. " atau "1 - "
        s = s.replace(Regex("""^\d{1,3}[\s.\-_]+"""), "")
        // Hapus ekstensi audio
        s = s.replace(Regex("""\.(mp3|m4a|opus|flac|wav|ogg)$""", RegexOption.IGNORE_CASE), "")
        // Hapus noise regex
        s = s.replace(NOISE_REGEX, " ")
        // Potong pemisah " | "
        s = s.substringBefore(" | ")
        s = s.replace(Regex("""\s+"""), " ").trim()
        return s.ifBlank { raw.trim() }
    }

    /**
     * Membersihkan nama artis dari channel suffix atau nilai placeholder.
     */
    fun cleanArtist(raw: String): String {
        var s = raw.replace('_', ' ')
        s = s.replace(Regex("""\b(VEVO|Topic|Official|Channel)\b""", RegexOption.IGNORE_CASE), " ")
        s = s.replace(Regex("""\s+"""), " ").trim()
        if (s.equals("Unknown", ignoreCase = true) ||
            s.equals("Unknown Artist", ignoreCase = true) ||
            s.isBlank()
        ) {
            return ""
        }
        return s
    }
}
