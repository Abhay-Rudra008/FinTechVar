package com.rudra.fintechvar.prefs

import android.content.Context
import androidx.core.content.edit
import com.rudra.fintechvar.db.modal.CurrencyItem

object CurrencyPref {

    private const val PREF = "app_settings"
    private const val KEY_COUNTRY = "currency_country"
    private const val KEY_SYMBOL = "currency_symbol"
    private const val KEY_NAME = "currency_name"

    fun saveCurrency(context: Context, item: CurrencyItem) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_COUNTRY, item.countryCode)
                putString(KEY_SYMBOL, item.symbol)
                putString(KEY_NAME, item.name)
            }
    }

    fun getCurrency(context: Context): CurrencyItem {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

        return CurrencyItem(
            countryCode = prefs.getString(KEY_COUNTRY, "US") ?: "US",
            symbol = prefs.getString(KEY_SYMBOL, "$") ?: "$",
            name = prefs.getString(KEY_NAME, "US Dollar") ?: "US Dollar"
        )
    }
}
