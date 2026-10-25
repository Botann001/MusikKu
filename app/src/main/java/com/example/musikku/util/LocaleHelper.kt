package com.example.musikku.util

import android.content.Context
import android.content.res.Configuration
import com.example.musikku.data.preferences.AppLanguage
import java.util.Locale

/**
 * Pembantu untuk menerapkan perubahan bahasa (locale) pada runtime.
 * Memanggil updateLocale() dari Activity.attachBaseContext untuk memastikan
 * semua string resource dimuat dalam bahasa yang dipilih.
 */
object LocaleHelper {

    /**
     * Mengembalikan Context yang terlampir dengan locale yang dipilih.
     * Gunakan di Activity.attachBaseContext.
     */
    fun applyLocale(context: Context, language: AppLanguage): Context {
        val locale = resolveLocale(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
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
        }
    }
}
