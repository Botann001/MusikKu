package com.example.musikku.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Model respon dari Jamendo API v3.0 /tracks/
 */
data class JamendoResponse(
    @SerializedName("headers")
    val headers: JamendoHeaderDto? = null,
    @SerializedName("results")
    val results: List<JamendoTrackDto> = emptyList()
)

data class JamendoHeaderDto(
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("code")
    val code: Int? = null,
    @SerializedName("error_message")
    val errorMessage: String? = null
)

data class JamendoTrackDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("duration")
    val duration: Long = 0L, // dalam detik
    @SerializedName("artist_id")
    val artistId: String? = null,
    @SerializedName("artist_name")
    val artistName: String? = null,
    @SerializedName("album_name")
    val albumName: String? = null,
    @SerializedName("album_image")
    val albumImage: String? = null,
    @SerializedName("image")
    val image: String? = null,
    @SerializedName("audio")
    val audio: String? = null, // URL streaming mp3
    @SerializedName("audiodownload")
    val audioDownload: String? = null, // URL unduhan
    @SerializedName("audiodownload_allowed")
    val audioDownloadAllowed: Boolean = true
)
