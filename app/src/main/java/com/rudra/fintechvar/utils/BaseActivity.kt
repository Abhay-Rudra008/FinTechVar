package com.rudra.fintechvar.utils

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.rudra.fintechvar.prefs.FontPref
import com.rudra.fintechvar.prefs.LocaleHelper

open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val localizedContext = LocaleHelper.wrapContext(newBase)
        super.attachBaseContext(localizedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(FontPref.getFontTheme(this))
        super.onCreate(savedInstanceState)
    }
}
