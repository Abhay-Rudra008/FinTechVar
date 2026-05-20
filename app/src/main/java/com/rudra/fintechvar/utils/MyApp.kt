package com.rudra.fintechvar.utils

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.rudra.fintechvar.db.database.AppDatabase
import com.rudra.fintechvar.db.modal.CategoryEntity
import com.rudra.fintechvar.db.modal.SynonymEntity
import com.rudra.fintechvar.prefs.DarkModePref
import com.rudra.fintechvar.voice.TransactionExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()


        val db = AppDatabase.getDatabase(this)

        CoroutineScope(Dispatchers.IO).launch {
            val categoryDao = db.categoryDao()
            val synonymDao = db.synonymDao()

            if (categoryDao.count() == 0) {
                categoryDao.insert(
                    CategoryEntity(
                        categoryName = "Shopping",
                        categoryType = "EXPENSE",
                        categoryImage = "ic_shopping"
                    )
                )
                categoryDao.insert(CategoryEntity(categoryName = "Salary", categoryType = "INCOME", categoryImage = "ic_salary"))
                categoryDao.insert(CategoryEntity(categoryName = "Food", categoryType = "EXPENSE", categoryImage = "ic_food"))
                categoryDao.insert(CategoryEntity(categoryName = "Travel", categoryType = "EXPENSE", categoryImage = "ic_travel"))
                categoryDao.insert(CategoryEntity(categoryName = "Rent", categoryType = "EXPENSE", categoryImage = "ic_rent"))
                categoryDao.insert(CategoryEntity(categoryName = "Insurance", categoryType = "EXPENSE", categoryImage = "ic_insurance"))

            }

            if (synonymDao.getAllSynonyms().isEmpty()) {

                val defaultMappings = mapOf(
                    "Food" to listOf(
                        "khana", "khane", "bhojan", "lunch", "dinner", "breakfast",
                        "nashta", "zomato", "swiggy", "coffee", "chai", "grocery",
                        "sabzi", "ration"
                    ),
                    "Travel" to listOf(
                        "petrol", "diesel", "cng", "bus", "flight", "cab", "uber",
                        "ola", "auto", "metro", "ticket", "transit", "journey"
                    ),
                    "Shopping" to listOf(
                        "shoes", "clothes", "kapde", "amazon", "flipkart", "myntra", "shopping"
                    ),
                    "Rent" to listOf("rent", "kiraya", "room rent", "flat rent", "landlord", "pg"),
                    "Insurance" to listOf(
                        "insurance", "policy", "premium", "lic", "mediclaim", "term plan"
                    ),
                    "Salary" to listOf("salary", "tankha", "bonus", "freelance", "income", "profit")
                )

                val synonymList = mutableListOf<SynonymEntity>()

                for ((category, words) in defaultMappings) {
                    for (word in words) {
                        synonymList.add(SynonymEntity(rawWord = word, categoryName = category))
                    }
                }

                synonymDao.insertAll(synonymList)
            }

            TransactionExtractor.initDatabase(synonymDao)
        }

        // Setup Dark Mode
        AppCompatDelegate.setDefaultNightMode(
            if (DarkModePref.isDarkMode(this))
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}