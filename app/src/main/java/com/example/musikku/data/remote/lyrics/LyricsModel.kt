package com.example.musikku.data.remote.lyrics

/**
 * Mewakili satu baris lirik lagu dengan timestamp waktu (milidetik).
 */
data class LyricLine(
    val timeMs: Long,
    val text: String
)

/**
 * State UI untuk tampilan lirik berjalan (karaoke-style lyrics).
 */
sealed interface LyricsUiState {
    object Idle : LyricsUiState
    object Loading : LyricsUiState
    data class Success(
        val lines: List<LyricLine>,
        val isSynced: Boolean,
        val plainLyrics: String? = null,
        val trackName: String,
        val artistName: String,
        val source: String = "LRCLIB"
    ) : LyricsUiState
    object Empty : LyricsUiState
    data class Error(val message: String) : LyricsUiState
}
