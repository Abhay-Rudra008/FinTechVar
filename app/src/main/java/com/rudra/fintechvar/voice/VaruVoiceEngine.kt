package com.rudra.fintechvar.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale


class VaruVoiceEngine(
    private val context: Context,
    private val commandProcessor: CommandProcessor,
    private val speaker: VaruSpeaker
) {
    private var destroyed = false
    private var manualMode = false
    private var resultCallback: (() -> Unit)? = null
    private var errorCallback: (() -> Unit)? = null

    private var speechRecognizer: SpeechRecognizer? = null

    init {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    }

    fun startManualListening(
        onResult: () -> Unit,
        onError: () -> Unit
    ) {
        if (destroyed) return

        manualMode = true
        resultCallback = onResult
        errorCallback = onError

        startListening()
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull()

                if (!spokenText.isNullOrEmpty()) {
                    commandProcessor.process(spokenText)
                    resultCallback?.invoke()
                } else {
                    errorCallback?.invoke()
                }
            }

            override fun onError(error: Int) {
                errorCallback?.invoke()
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }


    fun destroy() {
        destroyed = true
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}