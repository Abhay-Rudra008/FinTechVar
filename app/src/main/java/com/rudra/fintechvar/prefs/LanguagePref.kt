package com.rudra.fintechvar.prefs

import android.content.Context
import androidx.core.content.edit

object LanguagePref {

    private const val PREF_NAME = "language_pref"
    private const val KEY_LANG = "app_language"

    fun getLanguage(context: Context): String {
        return context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANG, "en") ?: "en"
    }

    fun setLanguage(context: Context, lang: String) {
        context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_LANG, lang)
            }
    }
}
