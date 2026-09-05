package com.wayfind.app.domain

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.wayfind.app.data.model.Room
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    PERMISSION_DENIED,
    UNAVAILABLE
}

class VoiceAssistantManager(
    private val context: Context,
    private val ttsManager: TTSManager
) {

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState

    private val _statusMessage = MutableStateFlow("Double-tap anywhere to speak your destination.")
    val statusMessage: StateFlow<String> = _statusMessage

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText

    private var speechRecognizer: SpeechRecognizer? = null
    private var onDestinationResolved: ((Room) -> Unit)? = null
    private var availableRooms: List<Room> = emptyList()

    // Internal coroutine scope for delay management
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Track consecutive errors to avoid spam
    private var consecutiveErrors = 0
    private val MAX_CONSECUTIVE_ERRORS = 3

    // Flag so we don't allow double-tap while already processing
    private var isActive = false

    fun initialize(rooms: List<Room>) {
        availableRooms = rooms
    }

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Called from the UI after permission has been confirmed granted.
     */
    fun onDoubleTap(onDestinationResolved: (Room) -> Unit) {
        if (isActive) return // Debounce: ignore double taps while processing

        this.onDestinationResolved = onDestinationResolved

        when {
            !SpeechRecognizer.isRecognitionAvailable(context) -> handleUnavailable()
            _voiceState.value == VoiceState.LISTENING -> stopListening()
            else -> startListeningSequence()
        }
    }

    private fun startListeningSequence() {
        isActive = true
        destroyRecognizer() // Clean up any previous recognizer

        _voiceState.value = VoiceState.SPEAKING
        _statusMessage.value = "Listening... Say a room name."
        _recognizedText.value = ""

        // Stop any ongoing TTS first, then wait for audio to fully clear before launching recognizer
        ttsManager.stop()

        scope.launch {
            // Announce then wait safely - use TTS speak with callback pattern
            // If TTS is unavailable, this resolves immediately via onDone
            var ttsDone = false
            ttsManager.speak("Listening. Say the room name now.") {
                ttsDone = true
            }

            // Wait for TTS to confirm it's done, or timeout after 3 seconds
            val timeoutMs = 3000L
            val startMs = System.currentTimeMillis()
            while (!ttsDone && (System.currentTimeMillis() - startMs) < timeoutMs) {
                delay(50)
            }

            // Extra buffer so audio hardware resets after TTS output
            delay(400)

            // Only launch recognizer if we're still in a valid state
            if (_voiceState.value != VoiceState.IDLE) {
                launchRecognizer()
            }
        }
    }

    private fun launchRecognizer() {
        try {
            _voiceState.value = VoiceState.LISTENING
            _statusMessage.value = "Microphone open — speak now."

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(buildRecognitionListener())

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                // Give the user at least 5 seconds of silence before timeout
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            handleError("Could not start speech recognition. Please try again.", VoiceState.IDLE)
        }
    }

    private fun buildRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                _voiceState.value = VoiceState.LISTENING
                _statusMessage.value = "🎙️ Microphone open — speak clearly now."
            }

            override fun onBeginningOfSpeech() {
                _statusMessage.value = "Hearing you..."
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _voiceState.value = VoiceState.PROCESSING
                _statusMessage.value = "Processing your request..."
            }

            override fun onResults(results: Bundle?) {
                consecutiveErrors = 0
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                if (matches.isNullOrEmpty()) {
                    handleError("I did not catch that. Please double-tap and try again.", VoiceState.IDLE)
                    return
                }

                val spokenText = matches.first()
                _recognizedText.value = "You said: \"$spokenText\""
                _voiceState.value = VoiceState.PROCESSING

                val matchedRoom = resolveRoomFromSpeech(spokenText, matches)
                if (matchedRoom != null) {
                    handleSuccess(matchedRoom)
                } else {
                    val suggestions = buildSuggestionString()
                    handleError(
                        "Sorry, I could not find \"$spokenText\". Try saying: $suggestions. Double-tap to try again.",
                        VoiceState.IDLE
                    )
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (!partial.isNullOrBlank()) {
                    _statusMessage.value = "Hearing: \"$partial\"..."
                }
            }

            override fun onError(error: Int) {
                consecutiveErrors++

                // ERROR_CLIENT (code 5) often fires if recognizer was called too quickly
                // after TTS — we now handle this by the 400ms delay above, but keep
                // a specific message here in case it still occurs
                val message = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH ->
                        "I could not understand that. Please speak clearly and try again."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                        "No speech detected. Double-tap and speak after the tone."
                    SpeechRecognizer.ERROR_AUDIO ->
                        "Microphone error. Please ensure nothing else is using the mic and try again."
                    SpeechRecognizer.ERROR_NETWORK ->
                        "Network error. Speech recognition may require internet on this device."
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                        "Network timeout. Please check your connection and try again."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                        "Microphone permission is required for voice navigation."
                    SpeechRecognizer.ERROR_CLIENT ->
                        "Audio system conflict. Please wait a moment then double-tap again."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                        "Speech recognizer is busy. Please wait a moment and try again."
                    SpeechRecognizer.ERROR_SERVER ->
                        "Speech server error. Please try again."
                    else -> "An unexpected error occurred. Please double-tap to try again."
                }

                if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                    consecutiveErrors = 0
                    handleError(
                        "Multiple errors in a row. Please double-tap to restart voice navigation.",
                        VoiceState.IDLE
                    )
                } else {
                    handleError(message, VoiceState.IDLE)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun resolveRoomFromSpeech(primary: String, allHypotheses: List<String>): Room? {
        for (hypothesis in allHypotheses) {
            val lower = hypothesis.lowercase().trim()

            // 1. Exact match
            val exact = availableRooms.find { it.name.lowercase().trim() == lower }
            if (exact != null) return exact

            // 2. Contains match
            val partial = availableRooms.find { room ->
                lower.contains(room.name.lowercase().trim()) ||
                room.name.lowercase().trim().contains(lower)
            }
            if (partial != null) return partial

            // 3. Word overlap match
            val spokenWords = lower.split(" ").filter { it.length > 2 }
            val wordMatch = availableRooms.find { room ->
                val roomWords = room.name.lowercase().split(" ").filter { it.length > 2 }
                spokenWords.any { word -> roomWords.any { it.contains(word) || word.contains(it) } }
            }
            if (wordMatch != null) return wordMatch
        }
        return null
    }

    private fun handleSuccess(room: Room) {
        isActive = false
        _voiceState.value = VoiceState.SPEAKING
        val message = "Route calculated to ${room.name}. Follow the navigation instructions."
        _statusMessage.value = "Navigating to: ${room.name}"
        ttsManager.speak(message) {
            _voiceState.value = VoiceState.IDLE
            _statusMessage.value = "Double-tap to change destination."
            onDestinationResolved?.invoke(room)
        }
    }

    private fun handleError(message: String, nextState: VoiceState) {
        isActive = false
        destroyRecognizer()
        _voiceState.value = VoiceState.SPEAKING
        _statusMessage.value = message
        ttsManager.speak(message) {
            _voiceState.value = nextState
        }
    }

    private fun handleUnavailable() {
        isActive = false
        _voiceState.value = VoiceState.UNAVAILABLE
        _statusMessage.value = "Speech recognition unavailable on this device."
        ttsManager.speak("Voice navigation is not available on this device.") {
            _voiceState.value = VoiceState.IDLE
        }
    }

    private fun buildSuggestionString(): String {
        return availableRooms.take(4).joinToString(", ") { it.name }
    }

    fun announceStep(stepInstruction: String) {
        ttsManager.speak(stepInstruction)
    }

    fun greetUser() {
        _voiceState.value = VoiceState.SPEAKING
        ttsManager.speak("Welcome to Wayfind voice navigation. Double-tap anywhere on the screen to tell me where you want to go.") {
            _voiceState.value = VoiceState.IDLE
            _statusMessage.value = "Double-tap anywhere to say your destination."
        }
    }

    fun announceMicPermissionDenied() {
        _voiceState.value = VoiceState.PERMISSION_DENIED
        _statusMessage.value = "Microphone permission denied. Please enable it in Settings."
        ttsManager.speak("Microphone access was denied. Please go to your device settings, find the Wayfind app, and enable the microphone permission.") {
            _voiceState.value = VoiceState.IDLE
        }
    }

    private fun stopListening() {
        isActive = false
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) { /* ignore */ }
        _voiceState.value = VoiceState.IDLE
        _statusMessage.value = "Stopped. Double-tap to speak again."
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) { /* ignore */ }
    }

    fun shutdown() {
        scope.cancel()
        destroyRecognizer()
    }
}
