package com.example.musikku.data.repository

import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.musikku.data.remote.youtube.YouTubeSearchItemDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.regex.Pattern

enum class SpotifyLinkType {
    TRACK,
    ALBUM,
    PLAYLIST,
    UNKNOWN
}

data class SpotifyUserProfile(
    val id: String,
    val displayName: String,
    val avatarUrl: String?,
    val product: String? = null
)

data class SpotifyUserPlaylist(
    val id: String,
    val name: String,
    val description: String,
    val coverUrl: String?,
    val trackCount: Int,
    val isPublic: Boolean = true
)

data class SpotifyTrack(
    val id: String,
    val title: String,
    val artist: String,
    val durationMs: Long = 0L,
    val coverArtUri: String? = null,
    val spotifyUri: String? = null
)

data class SpotifyImportResult(
    val type: SpotifyLinkType,
    val id: String,
    val title: String,
    val subtitle: String,
    val coverArtUri: String?,
    val tracks: List<SpotifyTrack>,
    val originalUrl: String
)

/**
 * Repository untuk mem-parse dan mengambil metadata publik dari tautan Spotify
 * (Lagu/Track, Album, dan Playlist publik) tanpa memerlukan login pengguna.
 * Menggunakan embed metadata Spotify dan mencocokkan ke YouTube Music untuk audio streaming.
 */
class SpotifyRepository(
    private val okHttpClient: OkHttpClient,
    private val youTubeMusicRepository: YouTubeMusicRepository
) {
    companion object {
        private const val TAG = "SpotifyRepository"
        private val NEXT_DATA_PATTERN = Pattern.compile(
            "<script id=\"__NEXT_DATA__\" type=\"application/json\">(.*?)</script>",
            Pattern.DOTALL
        )
        private val SPOTIFY_URL_PATTERN = Pattern.compile(
            "(?:https?://(?:open|play)\\.spotify\\.com/(?:intl-[a-zA-Z-]+/)?(?:user/[^/]+/)?|spotify:)(track|playlist|album)[:/]([a-zA-Z0-9]+)",
            Pattern.CASE_INSENSITIVE
        )
    }

    /**
     * Memeriksa apakah sebuah string adalah link atau URI Spotify yang valid.
     */
    fun isSpotifyLink(input: String): Boolean {
        return SPOTIFY_URL_PATTERN.matcher(input.trim()).find()
    }

    /**
     * Mengekstrak tipe dan ID dari tautan Spotify.
     */
    fun parseSpotifyLink(input: String): Pair<SpotifyLinkType, String>? {
        val matcher = SPOTIFY_URL_PATTERN.matcher(input.trim())
        if (matcher.find()) {
            val typeStr = matcher.group(1)?.lowercase() ?: return null
            val id = matcher.group(2) ?: return null
            val type = when (typeStr) {
                "track" -> SpotifyLinkType.TRACK
                "playlist" -> SpotifyLinkType.PLAYLIST
                "album" -> SpotifyLinkType.ALBUM
                else -> SpotifyLinkType.UNKNOWN
            }
            if (type != SpotifyLinkType.UNKNOWN && id.isNotBlank()) {
                return Pair(type, id)
            }
        }
        return null
    }

    /**
     * Mengambil metadata publik Spotify (Lagu, Album, Playlist) via embed endpoint.
     */
    suspend fun fetchSpotifyMetadata(urlOrUri: String): Result<SpotifyImportResult> = withContext(Dispatchers.IO) {
        val parsed = parseSpotifyLink(urlOrUri)
            ?: return@withContext Result.failure(IllegalArgumentException("Tautan Spotify tidak dikenali atau tidak valid."))

        val (type, id) = parsed
        val typeStr = when (type) {
            SpotifyLinkType.TRACK -> "track"
            SpotifyLinkType.ALBUM -> "album"
            SpotifyLinkType.PLAYLIST -> "playlist"
            SpotifyLinkType.UNKNOWN -> return@withContext Result.failure(IllegalArgumentException("Tipe konten Spotify tidak didukung."))
        }

        val embedUrl = "https://open.spotify.com/embed/$typeStr/$id"
        Log.d(TAG, "Fetching embedUrl: $embedUrl for input: $urlOrUri")

        try {
            val request = Request.Builder()
                .url(embedUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Gagal mengambil data dari Spotify (HTTP ${response.code}). Pastikan link publik."))
            }

            val html = response.body?.string().orEmpty()
            val matcher = NEXT_DATA_PATTERN.matcher(html)
            if (!matcher.find()) {
                return@withContext Result.failure(Exception("Data konten Spotify tidak ditemukan pada halaman embed."))
            }

            val jsonStr = matcher.group(1) ?: return@withContext Result.failure(Exception("Gagal membaca struktur JSON Spotify."))
            val rootJson = JSONObject(jsonStr)

            val pageProps = rootJson.optJSONObject("props")?.optJSONObject("pageProps")
            val stateData = pageProps?.optJSONObject("state")?.optJSONObject("data")
            val entity = stateData?.optJSONObject("entity")

            if (entity == null) {
                if (type == SpotifyLinkType.TRACK) {
                    val oembedResult = fetchTrackViaOEmbed(id, urlOrUri)
                    if (oembedResult != null) return@withContext Result.success(oembedResult)
                }
                return@withContext Result.failure(Exception("Konten Spotify tidak ditemukan atau link tidak valid/privat."))
            }

            val result = when (type) {
                SpotifyLinkType.TRACK -> parseTrackEntity(entity, id, urlOrUri)
                SpotifyLinkType.ALBUM -> parseAlbumEntity(entity, id, urlOrUri)
                SpotifyLinkType.PLAYLIST -> parsePlaylistEntity(entity, id, urlOrUri)
                else -> return@withContext Result.failure(Exception("Tipe tidak didukung"))
            }

            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal fetch metadata Spotify: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun parseTrackEntity(entity: JSONObject, id: String, originalUrl: String): SpotifyImportResult {
        val title = entity.optString("title").ifBlank { entity.optString("name", "Tanpa Judul") }
        val duration = entity.optLong("duration", 0L)

        val artistsArray = entity.optJSONArray("artists")
        val artistsList = mutableListOf<String>()
        if (artistsArray != null) {
            for (i in 0 until artistsArray.length()) {
                val artistObj = artistsArray.optJSONObject(i)
                val artistName = artistObj?.optString("name")
                if (!artistName.isNullOrBlank()) artistsList.add(artistName)
            }
        }
        val artist = if (artistsList.isNotEmpty()) artistsList.joinToString(", ")
                     else entity.optString("subtitle", "Artis Tidak Diketahui")

        val coverArtUri = extractCoverArt(entity)
        val spotifyUri = entity.optString("uri", "spotify:track:$id")

        val track = SpotifyTrack(
            id = id,
            title = title,
            artist = artist,
            durationMs = duration,
            coverArtUri = coverArtUri,
            spotifyUri = spotifyUri
        )

        return SpotifyImportResult(
            type = SpotifyLinkType.TRACK,
            id = id,
            title = title,
            subtitle = artist,
            coverArtUri = coverArtUri,
            tracks = listOf(track),
            originalUrl = originalUrl
        )
    }

    private fun parseAlbumEntity(entity: JSONObject, id: String, originalUrl: String): SpotifyImportResult {
        val title = entity.optString("title").ifBlank { entity.optString("name", "Album Spotify") }
        val subtitle = entity.optString("subtitle", "Artis Tidak Diketahui")
        val coverArtUri = extractCoverArt(entity)

        val tracks = extractTrackList(entity, coverArtUri)

        return SpotifyImportResult(
            type = SpotifyLinkType.ALBUM,
            id = id,
            title = title,
            subtitle = "$subtitle • ${tracks.size} lagu",
            coverArtUri = coverArtUri,
            tracks = tracks,
            originalUrl = originalUrl
        )
    }

    private fun parsePlaylistEntity(entity: JSONObject, id: String, originalUrl: String): SpotifyImportResult {
        val title = entity.optString("name").ifBlank { entity.optString("title", "Playlist Spotify") }
        val subtitle = entity.optString("subtitle", "Spotify Playlist")
        val coverArtUri = extractCoverArt(entity)

        val tracks = extractTrackList(entity, coverArtUri)

        return SpotifyImportResult(
            type = SpotifyLinkType.PLAYLIST,
            id = id,
            title = title,
            subtitle = "$subtitle • ${tracks.size} lagu",
            coverArtUri = coverArtUri,
            tracks = tracks,
            originalUrl = originalUrl
        )
    }

    private fun extractTrackList(entity: JSONObject, defaultCover: String?): List<SpotifyTrack> {
        val trackListJson = entity.optJSONArray("trackList") ?: return emptyList()
        val result = mutableListOf<SpotifyTrack>()

        for (i in 0 until trackListJson.length()) {
            val item = trackListJson.optJSONObject(i) ?: continue
            val trackTitle = item.optString("title", "Lagu ${i + 1}")
            val trackSubtitle = item.optString("subtitle", "Artis Tidak Diketahui")
            val duration = item.optLong("duration", 0L)
            val uri = item.optString("uri", "")
            val trackId = if (uri.startsWith("spotify:track:")) uri.substringAfter("spotify:track:") else "$i"

            result.add(
                SpotifyTrack(
                    id = trackId,
                    title = trackTitle,
                    artist = trackSubtitle,
                    durationMs = duration,
                    coverArtUri = defaultCover,
                    spotifyUri = uri
                )
            )
        }
        return result
    }

    private fun extractCoverArt(entity: JSONObject): String? {
        val visualIdentity = entity.optJSONObject("visualIdentity") ?: return null
        val images = visualIdentity.optJSONArray("image") ?: return null
        var bestUrl: String? = null
        var maxDimension = 0

        for (i in 0 until images.length()) {
            val img = images.optJSONObject(i) ?: continue
            val width = img.optInt("maxWidth", 0)
            val height = img.optInt("maxHeight", 0)
            val dim = maxOf(width, height)
            val url = img.optString("url")
            if (url.isNotBlank() && dim >= maxDimension) {
                maxDimension = dim
                bestUrl = url
            }
        }
        return bestUrl
    }

    private fun fetchTrackViaOEmbed(id: String, originalUrl: String): SpotifyImportResult? {
        val oembedUrl = "https://open.spotify.com/oembed?url=https://open.spotify.com/track/$id"
        try {
            val req = Request.Builder()
                .url(oembedUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            val resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string().orEmpty()
                val json = JSONObject(body)
                val fullTitle = json.optString("title", "Lagu Spotify")
                val thumbnail = json.optString("thumbnail_url", "")
                val (title, artist) = if (fullTitle.contains(" by ")) {
                    val p = fullTitle.split(" by ")
                    Pair(p[0].trim(), p.getOrNull(1)?.trim() ?: "Artis Spotify")
                } else {
                    Pair(fullTitle, "Artis Spotify")
                }
                val track = SpotifyTrack(
                    id = id,
                    title = title,
                    artist = artist,
                    durationMs = 0L,
                    coverArtUri = thumbnail.ifBlank { null },
                    spotifyUri = "spotify:track:$id"
                )
                return SpotifyImportResult(
                    type = SpotifyLinkType.TRACK,
                    id = id,
                    title = title,
                    subtitle = artist,
                    coverArtUri = thumbnail.ifBlank { null },
                    tracks = listOf(track),
                    originalUrl = originalUrl
                )
            }
        } catch (_: Exception) {}
        return null
    }

    /**
     * Mencocokkan SpotifyTrack ke YouTubeSearchItemDto via YouTubeMusicRepository.
     */
    suspend fun matchTrackToYouTube(track: SpotifyTrack): YouTubeSearchItemDto? = withContext(Dispatchers.IO) {
        val searchQuery = "${track.artist} - ${track.title}"
        val searchResult = youTubeMusicRepository.searchMusic(searchQuery)
        val items = searchResult.getOrNull().orEmpty()
        return@withContext items.firstOrNull()
    }

    // ─── OAuth PKCE & Web API Akun Pengguna (Tahap 3) ───────────────────────

    fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(64)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }

    fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(Charsets.US_ASCII)
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val digest = messageDigest.digest(bytes)
        return Base64.encodeToString(
            digest,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }

    fun buildAuthUri(clientId: String, codeChallenge: String, redirectUri: String = "musikku://spotify-callback"): Uri {
        return Uri.parse("https://accounts.spotify.com/authorize").buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", codeChallenge)
            .appendQueryParameter("scope", "user-library-read playlist-read-private playlist-read-collaborative user-read-private")
            .build()
    }

    suspend fun exchangeCodeForToken(
        code: String,
        verifier: String,
        clientId: String,
        redirectUri: String = "musikku://spotify-callback"
    ): Result<Pair<String, String?>> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", redirectUri)
                .add("client_id", clientId)
                .add("code_verifier", verifier)
                .build()

            val req = Request.Builder()
                .url("https://accounts.spotify.com/api/token")
                .post(formBody)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                return@withContext Result.failure(Exception("Gagal menukar token: HTTP ${resp.code} - $body"))
            }

            val json = JSONObject(body)
            val accessToken = json.getString("access_token")
            val refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() }
            Result.success(Pair(accessToken, refreshToken))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchCurrentUserProfile(accessToken: String): Result<SpotifyUserProfile> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://api.spotify.com/v1/me")
                .header("Authorization", "Bearer $accessToken")
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                return@withContext Result.failure(Exception("Gagal mengambil profil Spotify (HTTP ${resp.code})"))
            }

            val json = JSONObject(body)
            val id = json.getString("id")
            val displayName = json.optString("display_name").ifBlank { id }
            val product = json.optString("product", "free")
            val images = json.optJSONArray("images")
            val avatarUrl = images?.optJSONObject(0)?.optString("url")

            Result.success(SpotifyUserProfile(id = id, displayName = displayName, avatarUrl = avatarUrl, product = product))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchCurrentUserLikedSongs(accessToken: String, limit: Int = 50, offset: Int = 0): Result<List<SpotifyTrack>> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://api.spotify.com/v1/me/tracks?limit=$limit&offset=$offset")
                .header("Authorization", "Bearer $accessToken")
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val errorMsg = if (resp.code == 403 && body.contains("premium", ignoreCase = true)) {
                    "Kebijakan Spotify: Pembuat App wajib memiliki akun Spotify Premium aktif untuk sinkronisasi otomatis. Untuk akun Gratis, gunakan fitur Impor Link Playlist."
                } else {
                    "Gagal mengambil Lagu yang Disukai (HTTP ${resp.code})"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val json = JSONObject(body)
            val items = json.optJSONArray("items") ?: return@withContext Result.success(emptyList())
            val tracks = mutableListOf<SpotifyTrack>()

            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val trackObj = item.optJSONObject("track") ?: continue
                val id = trackObj.optString("id")
                val title = trackObj.optString("name")
                val artistsArr = trackObj.optJSONArray("artists")
                val artist = if (artistsArr != null && artistsArr.length() > 0) {
                    (0 until artistsArr.length()).mapNotNull { idx ->
                        artistsArr.optJSONObject(idx)?.optString("name")
                    }.joinToString(", ")
                } else "Unknown Artist"
                val durationMs = trackObj.optLong("duration_ms", 0L)
                val albumObj = trackObj.optJSONObject("album")
                val images = albumObj?.optJSONArray("images")
                val coverUrl = images?.optJSONObject(0)?.optString("url")

                if (id.isNotBlank() && title.isNotBlank()) {
                    tracks.add(
                        SpotifyTrack(
                            id = id,
                            title = title,
                            artist = artist,
                            durationMs = durationMs,
                            coverArtUri = coverUrl,
                            spotifyUri = "spotify:track:$id"
                        )
                    )
                }
            }

            Result.success(tracks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchCurrentUserPlaylists(accessToken: String, limit: Int = 50): Result<List<SpotifyUserPlaylist>> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://api.spotify.com/v1/me/playlists?limit=$limit")
                .header("Authorization", "Bearer $accessToken")
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val errorMsg = if (resp.code == 403 && body.contains("premium", ignoreCase = true)) {
                    "Kebijakan Spotify: Pembuat App wajib memiliki akun Spotify Premium aktif untuk membaca playlist via API. Silakan gunakan fitur Impor Link Playlist."
                } else {
                    "Gagal mengambil playlist Spotify (HTTP ${resp.code})"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val json = JSONObject(body)
            val items = json.optJSONArray("items") ?: return@withContext Result.success(emptyList())
            val playlists = mutableListOf<SpotifyUserPlaylist>()

            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val id = item.optString("id")
                val name = item.optString("name")
                val desc = item.optString("description")
                val isPublic = item.optBoolean("public", true)
                val images = item.optJSONArray("images")
                val coverUrl = images?.optJSONObject(0)?.optString("url")
                val tracksCount = item.optJSONObject("tracks")?.optInt("total", 0) ?: 0

                if (id.isNotBlank() && name.isNotBlank()) {
                    playlists.add(
                        SpotifyUserPlaylist(
                            id = id,
                            name = name,
                            description = desc,
                            coverUrl = coverUrl,
                            trackCount = tracksCount,
                            isPublic = isPublic
                        )
                    )
                }
            }

            Result.success(playlists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchUserPlaylistTracks(accessToken: String, playlistId: String): Result<List<SpotifyTrack>> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://api.spotify.com/v1/playlists/$playlistId/tracks?limit=100")
                .header("Authorization", "Bearer $accessToken")
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                return@withContext Result.failure(Exception("Gagal mengambil daftar lagu playlist (HTTP ${resp.code})"))
            }

            val json = JSONObject(body)
            val items = json.optJSONArray("items") ?: return@withContext Result.success(emptyList())
            val tracks = mutableListOf<SpotifyTrack>()

            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val trackObj = item.optJSONObject("track") ?: continue
                val id = trackObj.optString("id")
                val title = trackObj.optString("name")
                val artistsArr = trackObj.optJSONArray("artists")
                val artist = if (artistsArr != null && artistsArr.length() > 0) {
                    (0 until artistsArr.length()).mapNotNull { idx ->
                        artistsArr.optJSONObject(idx)?.optString("name")
                    }.joinToString(", ")
                } else "Unknown Artist"
                val durationMs = trackObj.optLong("duration_ms", 0L)
                val albumObj = trackObj.optJSONObject("album")
                val images = albumObj?.optJSONArray("images")
                val coverUrl = images?.optJSONObject(0)?.optString("url")

                if (id.isNotBlank() && title.isNotBlank()) {
                    tracks.add(
                        SpotifyTrack(
                            id = id,
                            title = title,
                            artist = artist,
                            durationMs = durationMs,
                            coverArtUri = coverUrl,
                            spotifyUri = "spotify:track:$id"
                        )
                    )
                }
            }

            Result.success(tracks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
