package com.nyl.euiesro.app

import android.content.Context

object PreferencesManager {
    private const val PREFS_NAME = "euiesro_prefs"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"
    private const val KEY_REMEMBER = "remember"

    fun saveCredentials(context: Context, user: String, pass: String, remember: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (remember) {
                putString(KEY_USERNAME, user)
                putString(KEY_PASSWORD, pass)
            } else {
                remove(KEY_USERNAME)
                remove(KEY_PASSWORD)
            }
            putBoolean(KEY_REMEMBER, remember)
            apply()
        }
    }

    fun getSavedUsername(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_USERNAME, null)

    fun getSavedPassword(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_PASSWORD, null)

    fun isRememberMeEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_REMEMBER, false)

    fun clearCredentials(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            remove(KEY_USERNAME)
            remove(KEY_PASSWORD)
            putBoolean(KEY_REMEMBER, false)
            apply()
        }
    }
}
