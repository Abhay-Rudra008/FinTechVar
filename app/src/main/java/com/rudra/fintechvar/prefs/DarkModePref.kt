package com.rudra.fintechvar.prefs

import android.content.Context
import androidx.core.content.edit

object DarkModePref {

    private const val PREF_NAME = "dark_mode_pref"
    private const val KEY_DARK_MODE = "dark_mode"

    fun isDarkMode(context: Context): Boolean {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return pref.getBoolean(KEY_DARK_MODE, false)
    }

    fun setDarkMode(context: Context, enabled: Boolean) {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        pref.edit { putBoolean(KEY_DARK_MODE, enabled) }
    }


}
