package com.rudra.fintechvar.voice

import com.rudra.fintechvar.db.dao.SynonymDao
import com.rudra.fintechvar.db.modal.ParsedTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object TransactionExtractor {

    private val stopWords = setOf(
        "mein", "me", "kiya", "hua", "ke", "liye", "par", "ko", "se",
        "ka", "ki", "diye", "gaye", "ho", "gaya", "aaj", "kal", "bas",
        "kharch", "rupaye", "rupees", "rs", "tha", "thi", "the", "hai",
        "lag", "aaya", "aaye", "order", "mila", "pay", "karo", "wala"
    )

    private val categoryMap = mutableMapOf<String, String>()
    private lateinit var synonymDao: SynonymDao

    fun initDatabase(dao: SynonymDao) {
        synonymDao = dao

        GlobalScope.launch(Dispatchers.IO) {
            val allSynonyms = synonymDao.getAllSynonyms()
            allSynonyms.forEach { entity ->
                categoryMap[entity.rawWord] = entity.categoryName
            }
        }
    }


    fun extract(cmd: String): ParsedTransaction? {
        val amount = AmountExtractor.extractAmount(cmd) ?: return null

        val words = cmd.lowercase().split(" ")
            .filter { it.toDoubleOrNull() == null }
            .filter { it.length > 2 }
            .filter { it !in stopWords }

        val rawWord = words.firstOrNull() ?: return null

        val normalizedCategory = categoryMap[rawWord] ?: rawWord

        return ParsedTransaction(amount, normalizedCategory.replaceFirstChar { it.uppercase() })
    }
}