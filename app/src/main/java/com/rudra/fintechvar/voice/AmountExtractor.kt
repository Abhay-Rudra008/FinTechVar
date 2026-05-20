package com.rudra.fintechvar.voice

object AmountExtractor {

    private val amountRegex = Regex("\\d+(\\.\\d+)?")

    fun extractAmount(cmd: String): Double? {

        val numericMatch = amountRegex.find(cmd)
        if (numericMatch != null) {
            return numericMatch.value.toDouble()
        }

        return HindiNumberParser.parseHindiNumber(cmd)
    }
}
