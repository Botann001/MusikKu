package com.example.musikku.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "musikku_settings")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/** Bahasa tampilan aplikasi yang didukung. */
enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    SYSTEM("system", "System Default", "Ikuti Sistem"),
    INDONESIAN("id", "Indonesian", "Indonesia"),
    ENGLISH("en", "English", "English"),
    JAPANESE("ja", "Japanese", "日本語"),
    KOREAN("ko", "Korean", "한국어"),
    ARABIC("ar", "Arabic", "العربية"),
    CHINESE("zh", "Chinese", "中文"),
    SPANISH("es", "Spanish", "Español"),
    FRENCH("fr", "French", "Français"),
    PORTUGUESE("pt", "Portuguese", "Português"),
    RUSSIAN("ru", "Russian", "Русский")
}

/**
 * Repository pengaturan aplikasi berbasis Jetpack DataStore Preferences.
 * Mengelola preferensi unduhan dan tema aplikasi.
 */
class SettingsRepository(private val context: Context) {

    companion object {
        val KEY_DOWNLOAD_ONLY_WIFI = booleanPreferencesKey("download_only_wifi")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_RECENT_SEARCHES = stringPreferencesKey("recent_searches_ordered")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_USER_AVATAR_URI = stringPreferencesKey("user_avatar_uri")
        val KEY_USER_BIO = stringPreferencesKey("user_bio")

        val KEY_SPOTIFY_ACCESS_TOKEN = stringPreferencesKey("spotify_access_token")
        val KEY_SPOTIFY_REFRESH_TOKEN = stringPreferencesKey("spotify_refresh_token")
        val KEY_SPOTIFY_USER_ID = stringPreferencesKey("spotify_user_id")
        val KEY_SPOTIFY_USER_NAME = stringPreferencesKey("spotify_user_name")
        val KEY_SPOTIFY_USER_AVATAR = stringPreferencesKey("spotify_user_avatar")
        val KEY_SPOTIFY_CLIENT_ID = stringPreferencesKey("spotify_client_id")
        val KEY_SPOTIFY_CODE_VERIFIER = stringPreferencesKey("spotify_code_verifier")
    }

    /**
     * Aliran data boolean: true jika unduhan diizinkan hanya ketika terhubung Wi-Fi.
     * Default adalah true untuk menghemat kuota seluler pengguna.
     */
    val downloadOnlyWifi: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_DOWNLOAD_ONLY_WIFI] ?: true
    }

    suspend fun setDownloadOnlyWifi(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DOWNLOAD_ONLY_WIFI] = enabled
        }
    }

    /**
     * Preferensi tema tampilan aplikasi (Sistem, Terang, Gelap).
     */
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val saved = preferences[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(saved)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode.name
        }
    }

    /** Preferensi bahasa tampilan aplikasi. */
    val appLanguage: Flow<AppLanguage> = context.dataStore.data.map { preferences ->
        val saved = preferences[KEY_APP_LANGUAGE] ?: AppLanguage.SYSTEM.name
        try {
            AppLanguage.valueOf(saved)
        } catch (e: Exception) {
            AppLanguage.SYSTEM
        }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.dataStore.edit { preferences ->
            preferences[KEY_APP_LANGUAGE] = language.name
        }
    }

    /** Riwayat pencarian musik terbaru (kronologis, unik, maksimal 15). */
    val recentSearches: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val raw = preferences[KEY_RECENT_SEARCHES] ?: ""
        if (raw.isBlank()) emptyList()
        else raw.split("\n").map { it.trim() }.filter { it.isNotBlank() }
    }

    suspend fun addRecentSearch(query: String) {
        val q = query.trim()
        if (q.isBlank()) return
        context.dataStore.edit { preferences ->
            val raw = preferences[KEY_RECENT_SEARCHES] ?: ""
            val currentList = if (raw.isBlank()) mutableListOf() else raw.split("\n").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
            currentList.remove(q)
            currentList.add(0, q)
            val trimmed = currentList.take(15)
            preferences[KEY_RECENT_SEARCHES] = trimmed.joinToString("\n")
        }
    }

    suspend fun removeRecentSearch(query: String) {
        val q = query.trim()
        context.dataStore.edit { preferences ->
            val raw = preferences[KEY_RECENT_SEARCHES] ?: ""
            val currentList = if (raw.isBlank()) mutableListOf() else raw.split("\n").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
            currentList.remove(q)
            preferences[KEY_RECENT_SEARCHES] = currentList.joinToString("\n")
        }
    }

    suspend fun clearRecentSearches() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_RECENT_SEARCHES)
        }
    }

    /** Profil Pengguna (Nama, Avatar, Bio) */
    val userName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_NAME] ?: ""
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_NAME] = name.trim()
        }
    }

    val userAvatarUri: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_AVATAR_URI] ?: ""
    }

    suspend fun setUserAvatarUri(uri: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_AVATAR_URI] = uri
        }
    }

    val userBio: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_BIO] ?: ""
    }

    suspend fun setUserBio(bio: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_BIO] = bio.trim()
        }
    }

    // ─── Integrasi Akun Spotify (Tahap 3) ──────────────────────────────────
    val spotifyAuth: Flow<SpotifyAuthData> = context.dataStore.data.map { prefs ->
        SpotifyAuthData(
            accessToken = prefs[KEY_SPOTIFY_ACCESS_TOKEN] ?: "",
            refreshToken = prefs[KEY_SPOTIFY_REFRESH_TOKEN] ?: "",
            userId = prefs[KEY_SPOTIFY_USER_ID] ?: "",
            userName = prefs[KEY_SPOTIFY_USER_NAME] ?: "",
            userAvatar = prefs[KEY_SPOTIFY_USER_AVATAR] ?: "",
            clientId = prefs[KEY_SPOTIFY_CLIENT_ID] ?: "",
            codeVerifier = prefs[KEY_SPOTIFY_CODE_VERIFIER] ?: ""
        )
    }

    suspend fun saveSpotifyAuth(
        accessToken: String,
        refreshToken: String = "",
        userId: String = "",
        userName: String = "",
        userAvatar: String = ""
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SPOTIFY_ACCESS_TOKEN] = accessToken.trim()
            if (refreshToken.isNotBlank()) prefs[KEY_SPOTIFY_REFRESH_TOKEN] = refreshToken.trim()
            if (userId.isNotBlank()) prefs[KEY_SPOTIFY_USER_ID] = userId.trim()
            if (userName.isNotBlank()) prefs[KEY_SPOTIFY_USER_NAME] = userName.trim()
            if (userAvatar.isNotBlank()) prefs[KEY_SPOTIFY_USER_AVATAR] = userAvatar.trim()
        }
    }

    suspend fun saveSpotifyCodeVerifier(verifier: String, clientId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SPOTIFY_CODE_VERIFIER] = verifier
            if (clientId.isNotBlank()) prefs[KEY_SPOTIFY_CLIENT_ID] = clientId
        }
    }

    suspend fun clearSpotifyAuth() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_SPOTIFY_ACCESS_TOKEN)
            prefs.remove(KEY_SPOTIFY_REFRESH_TOKEN)
            prefs.remove(KEY_SPOTIFY_USER_ID)
            prefs.remove(KEY_SPOTIFY_USER_NAME)
            prefs.remove(KEY_SPOTIFY_USER_AVATAR)
            prefs.remove(KEY_SPOTIFY_CODE_VERIFIER)
        }
    }
}

/**
 * Data status autentikasi akun Spotify pengguna.
 */
data class SpotifyAuthData(
    val accessToken: String = "",
    val refreshToken: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "",
    val clientId: String = "",
    val codeVerifier: String = ""
) {
    val isConnected: Boolean get() = accessToken.isNotBlank()
}
