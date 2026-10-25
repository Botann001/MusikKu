package com.example.musikku.data.remote.lyrics

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Data model respons dari LRCLIB API (https://lrclib.net).
 */
data class LrcLibTrackDto(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("trackName")
    val trackName: String? = null,

    @SerializedName("artistName")
    val artistName: String? = null,

    @SerializedName("albumName")
    val albumName: String? = null,

    @SerializedName("duration")
    val duration: Double? = null,

    @SerializedName("instrumental")
    val instrumental: Boolean? = null,

    @SerializedName("plainLyrics")
    val plainLyrics: String? = null,

    @SerializedName("syncedLyrics")
    val syncedLyrics: String? = null
)

/**
 * Retrofit Service untuk LRCLIB — API database lirik lagu gratis dan tanpa API key.
 */
interface LrcLibApiService {

    /**
     * Mengambil lirik lagu spesifik berdasarkan judul, artis, dan durasi (opsional).
     */
    @GET("api/get")
    suspend fun getLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String,
        @Query("duration") durationSeconds: Int? = null,
        @Query("album_name") albumName: String? = null
    ): LrcLibTrackDto

    /**
     * Pencarian alternatif dengan query atau filter artis + judul ketika pencarian persis gagal.
     */
    @GET("api/search")
    suspend fun searchLyrics(
        @Query("track_name") trackName: String? = null,
        @Query("artist_name") artistName: String? = null,
        @Query("q") query: String? = null
    ): List<LrcLibTrackDto>

    companion object {
        const val BASE_URL = "https://lrclib.net/"
    }
}
