package com.wayfind.app.domain

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TTSManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isInitialized = false

    // Queue of pending callbacks keyed by utterance ID
    private val utteranceCallbacks = mutableMapOf<String, () -> Unit>()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isInitialized = true

                // Set up utterance progress listener for callbacks
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onError(utteranceId: String?) {
                        utteranceId?.let {
                            utteranceCallbacks.remove(it)?.invoke()
                        }
                    }
                    override fun onDone(utteranceId: String?) {
                        utteranceId?.let {
                            utteranceCallbacks.remove(it)?.invoke()
                        }
                    }
                })
            } else {
                // Language not supported — still mark as a graceful degraded state
                isInitialized = false
            }
        }
    }

    /**
     * Speaks the given text. Optionally invokes [onDone] when TTS completes.
     * If TTS is not initialized, [onDone] is still called immediately to not block the flow.
     */
    fun speak(text: String, onDone: () -> Unit = {}) {
        if (!isInitialized || tts == null) {
            onDone()
            return
        }
        val utteranceId = "WAYFIND_TTS_${System.currentTimeMillis()}"
        utteranceCallbacks[utteranceId] = onDone
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)
    }

    /**
     * Stops any ongoing speech immediately.
     */
    fun stop() {
        tts?.stop()
        utteranceCallbacks.clear()
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
    }
}
