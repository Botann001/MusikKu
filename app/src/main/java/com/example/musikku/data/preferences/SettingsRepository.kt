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

/**
 * Repository pengaturan aplikasi berbasis Jetpack DataStore Preferences.
 * Mengelola preferensi unduhan dan tema aplikasi.
 */
class SettingsRepository(private val context: Context) {

    companion object {
        val KEY_DOWNLOAD_ONLY_WIFI = booleanPreferencesKey("download_only_wifi")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
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
}
