package com.example.musikku.data.remote.youtube

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Interface Retrofit untuk berinteraksi dengan Invidious / Piped API.
 * Menggunakan @Url dinamis agar aplikasi dapat berpindah instance secara otomatis
 * jika satu instance mengalami downtime atau rate-limit.
 */
interface YouTubeApiService {

    /**
     * Cari video musik di YouTube berdasarkan kata kunci.
     */
    @GET
    suspend fun searchVideos(
        @Url endpoint: String,
        @Query("q") query: String,
        @Query("type") type: String = "video"
    ): List<YouTubeSearchItemDto>

    /**
     * Ambil video musik yang sedang trending.
     */
    @GET
    suspend fun getTrending(
        @Url endpoint: String,
        @Query("type") type: String = "music"
    ): List<YouTubeSearchItemDto>

    /**
     * Dapatkan detail video termasuk daftar URL stream audio adaptive format.
     */
    @GET
    suspend fun getVideoDetails(
        @Url endpoint: String
    ): YouTubeVideoDetailDto
}
