package com.rudra.fintechvar.voice

object HindiNumberParser {

    private val numberMap = mapOf(
        "ek" to 1,
        "do" to 2,
        "teen" to 3, "tin" to 3,
        "char" to 4,
        "paanch" to 5, "pach" to 5,
        "chhe" to 6,
        "saat" to 7,
        "aath" to 8,
        "nau" to 9,
        "das" to 10,
        "gyarah" to 11,
        "barah" to 12,
        "terah" to 13,
        "chaudah" to 14,
        "pandrah" to 15,
        "solah" to 16,
        "satrah" to 17,
        "atharah" to 18,
        "unnis" to 19,
        "bees" to 20,
        "tees" to 30,
        "chalis" to 40,
        "pachas" to 50,
        "saath" to 60,
        "sattar" to 70,
        "assi" to 80,
        "nabbe" to 90
    )

    fun parseHindiNumber(text: String): Double? {
        val tokens = text.lowercase().split(" ")
        var total = 0
        var current = 0

        for (word in tokens) {
            when (word) {

                "sau" -> {
                    current *= 100
                }

                "hazar", "hajar" -> {
                    total += current * 1000
                    current = 0
                }

                "lakh" -> {
                    total += current * 100000
                    current = 0
                }

                "crore" -> {
                    total += current * 10000000
                    current = 0
                }

                else -> {
                    val value = numberMap[word]
                    if (value != null) {
                        current += value
                    }
                }
            }
        }

        return (total + current).takeIf { it > 0 }?.toDouble()
    }
}
