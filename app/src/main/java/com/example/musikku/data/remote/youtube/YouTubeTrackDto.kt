package com.example.musikku.data.remote.youtube

import com.google.gson.annotations.SerializedName

/**
 * Item hasil pencarian atau trending dari Invidious/Piped API.
 */
data class YouTubeSearchItemDto(
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("title")
    val title: String = "",
    @SerializedName("videoId")
    val videoId: String = "",
    @SerializedName("author")
    val author: String? = null,
    @SerializedName("authorId")
    val authorId: String? = null,
    @SerializedName("authorUrl")
    val authorUrl: String? = null,
    @SerializedName("videoThumbnails")
    val videoThumbnails: List<YouTubeThumbnailDto>? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("viewCount")
    val viewCount: Long? = null,
    @SerializedName("published")
    val published: Long? = null,
    @SerializedName("publishedText")
    val publishedText: String? = null,
    @SerializedName("lengthSeconds")
    val lengthSeconds: Long = 0L,
    @SerializedName("liveNow")
    val liveNow: Boolean = false,
    @SerializedName("premium")
    val premium: Boolean = false,
    @SerializedName("isUpcoming")
    val isUpcoming: Boolean = false
) {
    /**
     * Dapatkan URL thumbnail terbaik untuk ditampilkan di antarmuka.
     */
    val thumbnailUrl: String
        get() {
            // Gunakan thumbnail statis YouTube yang selalu valid dan beresolusi tinggi jika ada videoId
            if (videoId.isNotBlank()) {
                return "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
            }
            return videoThumbnails?.lastOrNull()?.url.orEmpty()
        }
}

/**
 * Objek thumbnail dari API.
 */
data class YouTubeThumbnailDto(
    @SerializedName("quality")
    val quality: String? = null,
    @SerializedName("url")
    val url: String? = null,
    @SerializedName("width")
    val width: Int? = null,
    @SerializedName("height")
    val height: Int? = null
)

/**
 * Detail video dan stream formats dari Invidious `/api/v1/videos/{videoId}`.
 */
data class YouTubeVideoDetailDto(
    @SerializedName("title")
    val title: String = "",
    @SerializedName("videoId")
    val videoId: String = "",
    @SerializedName("author")
    val author: String? = null,
    @SerializedName("authorId")
    val authorId: String? = null,
    @SerializedName("lengthSeconds")
    val lengthSeconds: Long = 0L,
    @SerializedName("videoThumbnails")
    val videoThumbnails: List<YouTubeThumbnailDto>? = null,
    @SerializedName("formatStreams")
    val formatStreams: List<YouTubeFormatStreamDto>? = null,
    @SerializedName("adaptiveFormats")
    val adaptiveFormats: List<YouTubeAdaptiveFormatDto>? = null
)

/**
 * Format kombinasi video + audio (misal itag 18 360p mp4).
 */
data class YouTubeFormatStreamDto(
    @SerializedName("url")
    val url: String = "",
    @SerializedName("itag")
    val itag: String? = null,
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("quality")
    val quality: String? = null,
    @SerializedName("container")
    val container: String? = null,
    @SerializedName("encoding")
    val encoding: String? = null
)

/**
 * Format audio adaptif terpisah (misal itag 140 m4a 128kbps AAC, atau itag 251 webm opus).
 */
data class YouTubeAdaptiveFormatDto(
    @SerializedName("url")
    val url: String = "",
    @SerializedName("itag")
    val itag: String? = null,
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("bitrate")
    val bitrate: String? = null,
    @SerializedName("container")
    val container: String? = null,
    @SerializedName("encoding")
    val encoding: String? = null,
    @SerializedName("audioQuality")
    val audioQuality: String? = null,
    @SerializedName("audioSampleRate")
    val audioSampleRate: String? = null
)
