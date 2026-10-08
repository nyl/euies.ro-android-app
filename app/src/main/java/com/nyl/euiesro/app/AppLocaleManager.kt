package com.nyl.euiesro.app

import android.content.Context
import androidx.activity.ComponentActivity
import java.util.Locale

enum class AppLocale(val code: String, val label: String) {
    ROMANIAN("ro", "Română"),
    ENGLISH("en", "English"),
    ITALIAN("it", "Italiano")
}

object AppLocaleManager {
    private const val PREFS_NAME = "euiesro_locale_prefs"
    private const val KEY_LOCALE = "selected_locale"

    fun getDefaultLocale(): AppLocale = AppLocale.ROMANIAN

    fun getCurrentLocale(context: Context): AppLocale {
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LOCALE, null)
        return AppLocale.entries.firstOrNull { it.code == saved } ?: getDefaultLocale()
    }

    fun setLocale(context: Context, locale: AppLocale) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LOCALE, locale.code).apply()

        val newLocale = Locale(locale.code)
        Locale.setDefault(newLocale)

        val config = appContext.resources.configuration
        config.setLocale(newLocale)
        config.setLayoutDirection(newLocale)
        appContext.resources.updateConfiguration(config, appContext.resources.displayMetrics)

        if (context is ComponentActivity) {
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
    }
}
