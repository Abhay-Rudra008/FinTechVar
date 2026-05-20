package com.rudra.fintechvar.prefs

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {

    fun wrapContext(context: Context): Context {
        val lang = LanguagePref.getLanguage(context)
        val locale = Locale(lang)

        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            config.setLocale(locale)
            context.createConfigurationContext(config)
        } else {
            config.locale = locale
            context.resources.updateConfiguration(
                config,
                context.resources.displayMetrics
            )
            context
        }
    }
}
