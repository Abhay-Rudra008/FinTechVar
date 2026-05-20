package com.rudra.fintechvar.prefs

import android.content.Context
import androidx.core.content.edit
import com.rudra.fintechvar.R

object FontPref {

    private const val PREF = "font_pref"
    private const val KEY_FONT_THEME = "font_theme"
    private const val KEY_FONT_NAME = "font_name"

    private  val DEFAULT_FONT_THEME =
        R.style.Theme_FinTechVar_PoppinsMediumItalic
    private const val DEFAULT_FONT_NAME = "Poppins Italic"

    fun saveFont(context: Context, themeRes: Int, name: String) {
        val prefs = context.applicationContext
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)

        prefs.edit(commit = true) {
            putInt(KEY_FONT_THEME, themeRes)
                .putString(KEY_FONT_NAME, name)
        }
    }


    fun getFontTheme(context: Context): Int =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getInt(KEY_FONT_THEME, DEFAULT_FONT_THEME)

    fun getFontName(context: Context): String =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_FONT_NAME, DEFAULT_FONT_NAME)!!
}
