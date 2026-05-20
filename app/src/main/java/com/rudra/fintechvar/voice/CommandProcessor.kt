package com.rudra.fintechvar.voice


import android.content.Context
import android.util.Log
import android.widget.Toast
import com.rudra.fintechvar.db.repository.ExpenseRepository
import com.rudra.fintechvar.db.utils.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.google.android.material.dialog.MaterialAlertDialogBuilder


class CommandProcessor(
    private val context: Context,
    private val repo: ExpenseRepository,
    private val scope: CoroutineScope,
    private val speaker: VaruSpeaker
) {

    private val normalizer = TextNormalizer()
    private val intentDetector = MLIntentDetector(context)

    fun process(input: String) {
        val normalized = normalizer.normalize(input)
        val result = intentDetector.detect(normalized)
        Log.d("VaruVoice", "2. Normalized Text: [$normalized]") // Add this!
        when {
            result.confidence >= 0.75f -> executeIntent(result.intent, normalized)
            result.confidence >= 0.50f -> askForConfirmation(result.intent, result.confidence, normalized)
            else -> {
                toast("Sorry, I didn't understand that 😕")
                speaker.speak("Please say it again")
            }
        }
    }

    private fun addCategory(cmd: String) {
        val name = cmd.replace("add", "", true).replace("category", "", true).trim()

        if (name.isBlank()) {
            toast("Category name missing")
            speaker.speak("Please tell the category name")
            return
        }

        confirm("Add category \"$name\"?") {
            scope.launch(Dispatchers.IO) {
                val success = repo.addCategoryByName(name.lowercase(), TransactionType.EXPENSE.toString())

                withContext(Dispatchers.Main) {
                    if (success) {
                        toast("Category added")
                        speaker.speak("Category added successfully")
                    } else {
                        toast("Category already exists")
                        speaker.speak("Category already exists")
                    }
                }
            }
        }
    }

    private fun deleteCategory(cmd: String) {
        val name = cmd.removePrefix("delete category").trim()
        if (name.isBlank()) return toast("Category name missing")

        confirm("Delete category \"$name\"?") {
            scope.launch(Dispatchers.IO) {
                val success = repo.deleteCategoryByName(name)
                withContext(Dispatchers.Main) {
                    toast(if (success) "Category deleted" else "Category not found")
                }
            }
        }
    }

    private fun addTransaction(cmd: String, type: TransactionType) {
        val parsed = TransactionExtractor.extract(cmd)
        Log.d("VaruVoice", "4. Extraction Result: $parsed") // Add this!
        if (parsed == null) {
            toast("Could not detect amount or category")
            speaker.speak("I could not detect amount or category")
            return
        }

        scope.launch(Dispatchers.IO) {
            val categoryName = parsed.categoryRaw.trim().lowercase()
            val categoryExists = repo.categoryExists(categoryName)

            if (!categoryExists) {
                withContext(Dispatchers.Main) {
                    confirm("Category $categoryName not found. Add it?") {
                        scope.launch(Dispatchers.IO) {
                            repo.addCategoryByName(categoryName, type.toString())
                            val success = repo.addTransactionByVoice(parsed.amount, type, categoryName, cmd)

                            withContext(Dispatchers.Main) {
                                if (success) {
                                    toast("Transaction added")
                                    speaker.speak("Transaction added successfully")
                                } else {
                                    toast("Failed to add transaction")
                                }
                            }
                        }
                    }
                }
                return@launch
            }

            val success = repo.addTransactionByVoice(parsed.amount, type, categoryName, cmd)
            withContext(Dispatchers.Main) {
                if (success) {
                    toast("Transaction added")
                    speaker.speak("Transaction added successfully")
                } else {
                    toast("Failed to add transaction")
                }
            }
        }
    }

    private fun executeIntent(intent: VoiceIntent, cmd: String) {
        when (intent) {
            VoiceIntent.ADD_EXPENSE -> addTransaction(cmd, TransactionType.EXPENSE)
            VoiceIntent.ADD_INCOME -> addTransaction(cmd, TransactionType.INCOME)
            VoiceIntent.ADD_CATEGORY -> addCategory(cmd)
            VoiceIntent.DELETE_CATEGORY -> deleteCategory(cmd)
            else -> toast("Unknown command")
        }
    }

    private fun askForConfirmation(intent: VoiceIntent, confidence: Float, cmd: String) {
        val intentName = intent.name.replace("_", " ").lowercase()
        speaker.speak("Did you mean to $intentName?")

        // Replaced generic AlertDialog with MaterialAlertDialogBuilder
        MaterialAlertDialogBuilder(context)
            .setTitle("Confirm")
            .setMessage("I think you want to $intentName\nConfidence: ${(confidence * 100).toInt()}%")
            .setPositiveButton("Yes") { _, _ -> executeIntent(intent, cmd) }
            .setNegativeButton("No") { _, _ -> speaker.speak("Okay, please repeat") }
            .show()
    }

    private fun confirm(msg: String, yes: () -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Confirm")
            .setMessage(msg)
            .setPositiveButton("Yes") { _, _ -> yes() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}