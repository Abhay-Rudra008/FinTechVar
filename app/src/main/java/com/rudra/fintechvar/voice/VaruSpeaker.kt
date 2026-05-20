package com.rudra.fintechvar.voice

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class VaruSpeaker(private val context: Context) {

    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
            }
        }
    }

    private var onSpeakingChanged: ((Boolean) -> Unit)? = null

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        onSpeakingChanged?.invoke(true)

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                Handler(Looper.getMainLooper()).post {
                    onSpeakingChanged?.invoke(false)
                    onDone?.invoke()
                }
            }

            override fun onError(utteranceId: String?) {
                Handler(Looper.getMainLooper()).post {
                    onSpeakingChanged?.invoke(false)
                    onDone?.invoke()
                }
            }
        })

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "VARU_TTS")
        }

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "VARU_TTS")
    }

    fun release() {
        tts.stop()
        tts.shutdown()
    }
}
