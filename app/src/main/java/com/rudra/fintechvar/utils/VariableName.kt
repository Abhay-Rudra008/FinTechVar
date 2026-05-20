package com.rudra.fintechvar.utils

import com.rudra.fintechvar.R
import com.rudra.fintechvar.db.modal.CurrencyItem
import com.rudra.fintechvar.db.modal.FontItem

object VariableName {
    val PREDEFINED_CURRENCY = listOf(
        CurrencyItem("US", "$", "US Dollar"),
        CurrencyItem("IN", "₹", "Indian Rupee"),
        CurrencyItem("GB", "£", "British Pound"),
        CurrencyItem("EU", "€", "Euro"),
        CurrencyItem("JP", "¥", "Japanese Yen"),
        CurrencyItem("CN", "¥", "Chinese Yuan"),
        CurrencyItem("AU", "$", "Australian Dollar"),
        CurrencyItem("CA", "$", "Canadian Dollar"),
        CurrencyItem("CH", "CHF", "Swiss Franc"),
        CurrencyItem("AE", "د.إ", "UAE Dirham"),
        CurrencyItem("SG", "$", "Singapore Dollar"),
        CurrencyItem("HK", "$", "Hong Kong Dollar"),
        CurrencyItem("KR", "₩", "South Korean Won"),
        CurrencyItem("ZA", "R", "South African Rand"),
        CurrencyItem("BR", "R$", "Brazilian Real")
    )


    val FONTS = listOf(
        FontItem("Adamina", "adamina", R.style.Theme_FinTechVar_Adamina),
        FontItem("Besley SemiBold", "besley_semibold", R.style.Theme_FinTechVar_BesleySemiBold),
        FontItem(
            "Poppins Italic", "poppins_medium_italic", R.style.Theme_FinTechVar_PoppinsMediumItalic
        ),
        FontItem(
            "Roboto Black", "roboto_condensed_black", R.style.Theme_FinTechVar_RobotoCondensedBlack
        ),
        FontItem(
            "Roboto Extra Bold",
            "roboto_condensed_extra_bold",
            R.style.Theme_FinTechVar_RobotoCondensedExtraBold
        ),
        FontItem(
            "Roboto Extra Bold Italic",
            "roboto_condensed_extra_bold_italic",
            R.style.Theme_FinTechVar_RobotoCondensedExtraBoldItalic
        )
    )

}