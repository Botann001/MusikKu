package com.example.musikku.util

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.util.Log
import com.example.musikku.data.preferences.AppLanguage
import java.util.Locale

/**
 * Pembantu untuk menerapkan perubahan bahasa (locale) pada runtime dan integrasi
 * per-app language Android 13+ (API 33+).
 */
object LocaleHelper {

    private const val TAG = "LocaleHelper"

    /**
     * Menerapkan locale ke sistem Android (khususnya Android 13+ LocaleManager)
     * dan memperbarui resources context.
     */
    fun applyLocaleToSystem(context: Context, language: AppLanguage) {
        val locale = resolveLocale(language)
        Locale.setDefault(locale)

        // 1. Android 13+ (API 33+) Per-App Language API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val localeManager = context.getSystemService(LocaleManager::class.java)
                val targetLocales = if (language == AppLanguage.SYSTEM) {
                    LocaleList.getEmptyLocaleList()
                } else {
                    LocaleList.forLanguageTags(language.code)
                }
                if (localeManager != null && localeManager.applicationLocales != targetLocales) {
                    localeManager.applicationLocales = targetLocales
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gagal memperbarui LocaleManager: ${e.message}")
            }
        }

        // 2. Perbarui context.resources untuk backward-compatibility & immediate effect
        try {
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        } catch (e: Exception) {
            Log.w(TAG, "Gagal updateConfiguration: ${e.message}")
        }
    }

    /**
     * Mengembalikan Context baru yang terbungkus dengan konfigurasi Locale yang dipilih.
     * Gunakan di Activity.attachBaseContext atau Compose LocalContext.
     */
    fun createLocalizedContext(context: Context, language: AppLanguage): Context {
        val locale = resolveLocale(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    /**
     * Backward-compatibility wrapper for applyLocale.
     */
    fun applyLocale(context: Context, language: AppLanguage): Context {
        return createLocalizedContext(context, language)
    }

    /**
     * Mengkonversi AppLanguage ke Locale Java.
     */
    fun resolveLocale(language: AppLanguage): Locale {
        return when (language) {
            AppLanguage.SYSTEM     -> Locale.getDefault()
            AppLanguage.INDONESIAN -> Locale.forLanguageTag("id")
            AppLanguage.ENGLISH    -> Locale.ENGLISH
            AppLanguage.JAPANESE   -> Locale.JAPANESE
            AppLanguage.KOREAN     -> Locale.KOREAN
            AppLanguage.ARABIC     -> Locale.forLanguageTag("ar")
            AppLanguage.CHINESE    -> Locale.CHINESE
            AppLanguage.SPANISH    -> Locale.forLanguageTag("es")
            AppLanguage.FRENCH     -> Locale.FRENCH
            AppLanguage.PORTUGUESE -> Locale.forLanguageTag("pt")
            AppLanguage.RUSSIAN    -> Locale.forLanguageTag("ru")
        }
    }
}
