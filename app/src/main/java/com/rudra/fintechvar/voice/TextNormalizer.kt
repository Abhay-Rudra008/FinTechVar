package com.rudra.fintechvar.voice

class TextNormalizer {

    fun normalize(input: String): String {
        return input
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace("rupees", "")
            .replace("rs", "")
            .replace("₹", "")
            .trim()
    }
}