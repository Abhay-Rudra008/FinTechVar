package com.rudra.fintechvar.prefs

import android.content.Context
import androidx.core.content.edit

object AppPrefs {

    private const val PREF = "app_settings"
    private const val KEY_BIO = "biometric_enabled"

    fun isBiometricEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean(KEY_BIO, false)

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_BIO, enabled) }
    }
}

