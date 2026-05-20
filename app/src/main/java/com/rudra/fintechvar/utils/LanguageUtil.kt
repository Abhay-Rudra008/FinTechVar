package com.rudra.fintechvar.utils

import com.rudra.fintechvar.db.modal.LanguageItemBottom


object LanguageUtil {

    val supportedLanguages: List<LanguageItemBottom> = listOf(
        LanguageItemBottom(name = "English", code = "en"),
        LanguageItemBottom(name = "हिन्दी", code = "hi"),
        LanguageItemBottom(name = "ગુજરાતી", code = "gu"),
        LanguageItemBottom(name = "বাংলা", code = "bn")
    )
}