package com.rudra.fintechvar.voice

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import org.tensorflow.lite.DataType
import java.nio.ByteOrder

class MLIntentDetector(context: Context) {

    private val interpreter: Interpreter
    private val wordIndex: Map<String, Int>
    private val labels: List<String>

    init {
        val options = Interpreter.Options().apply {
            setNumThreads(2)
        }
        interpreter = Interpreter(loadModel(context), options)
        wordIndex = loadTokenizer(context)
        labels = loadLabels(context)
    }

    fun detect(text: String): IntentResult {
        val inputTensor = interpreter.getInputTensor(0)
        val outputTensor = interpreter.getOutputTensor(0)

        val inputShape = inputTensor.shape()
        val seqLen = if (inputShape.size > 1) inputShape[1] else 10
        val isInputFloat = inputTensor.dataType() == DataType.FLOAT32

        val outputShape = outputTensor.shape()
        val numClasses = if (outputShape.size > 1) outputShape[1] else outputShape[0]

        val clean = text.lowercase().replace(Regex("[^a-z ]"), "")
        val tokens = clean.split(" ").map { wordIndex[it] ?: 1 }.take(seqLen)

        val inputBuffer = ByteBuffer.allocateDirect(seqLen * 4).apply {
            order(ByteOrder.nativeOrder())
        }

        for (i in 0 until seqLen) {
            val token = tokens.getOrNull(i) ?: 0
            if (isInputFloat) {
                inputBuffer.putFloat(token.toFloat())
            } else {
                inputBuffer.putInt(token)
            }
        }
        inputBuffer.rewind()

        val outputBuffer = ByteBuffer.allocateDirect(numClasses * 4).apply {
            order(ByteOrder.nativeOrder())
        }

        interpreter.run(inputBuffer, outputBuffer)
        outputBuffer.rewind()

        val scores = FloatArray(numClasses)
        for (i in 0 until numClasses) {
            scores[i] = outputBuffer.float
        }

        val maxIndex = scores.indices.maxByOrNull { scores[it] } ?: 0
        val confidence = scores[maxIndex]

        val intentName = labels.getOrNull(maxIndex) ?: ""
        val intent =
            runCatching { VoiceIntent.valueOf(intentName) }.getOrDefault(VoiceIntent.UNKNOWN)

        return IntentResult(intent, confidence)
    }

    private fun loadModel(context: Context): ByteBuffer {
        return context.assets.open("finance_model.tflite").use { input ->
            val bytes = input.readBytes()
            ByteBuffer.allocateDirect(bytes.size).apply {
                put(bytes)
                rewind()
            }
        }
    }

    private fun loadTokenizer(context: Context): Map<String, Int> {
        val json =
            context.assets.open("tokenizer_voice.json").bufferedReader().use { it.readText() }
        val config = JSONObject(json).getJSONObject("config")
        val wordIndexObj = JSONObject(config.getString("word_index"))

        return wordIndexObj.keys().asSequence().associateWith { wordIndexObj.getInt(it) }
    }

    private fun loadLabels(context: Context): List<String> {
        val jsonString = context.assets.open("labels.json").bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(jsonString)

        val labelsList = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            labelsList.add(jsonArray.getString(i))
        }
        return labelsList
    }
}