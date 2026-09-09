package com.example.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VoiceStatus(val label: String, val subtitle: String) {
    DISABLED("Disabled", "Tap power button to activate JARVIS"),
    SCHOOL_MODE("School Mode Active", "Voice recognition & microphone strictly disabled"),
    WAITING_FOR_WAKE_WORD("Waiting for \"Jarvis\"", "Say \"Jarvis\" followed by your question or command"),
    LISTENING("Listening...", "Say your question or command now"),
    CONVERSING("In Conversation", "Back-and-forth conversation active. Speak freely."),
    LIVE_MODE("Live Phone Mode", "Continuous real-time voice call active. Speak naturally."),
    PROCESSING("Thinking...", "Processing query & generating response"),
    SPEAKING("Responding...", "Speaking response via Text-to-Speech")
}

data class ConversationMessage(
    val id: Long = System.currentTimeMillis() + (0..999).random(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class JarvisLogEntry(
    val id: Long = System.currentTimeMillis() + (0..999).random(),
    val timestamp: Long = System.currentTimeMillis(),
    val message: String,
    val isCommand: Boolean = false,
    val isSystem: Boolean = false
)

object JarvisStateHolder {
    private val _voiceStatus = MutableStateFlow(VoiceStatus.DISABLED)
    val voiceStatus: StateFlow<VoiceStatus> = _voiceStatus.asStateFlow()

    private val _soundAmplitude = MutableStateFlow(0f)
    val soundAmplitude: StateFlow<Float> = _soundAmplitude.asStateFlow()

    private val _lastRecognizedPhrase = MutableStateFlow("")
    val lastRecognizedPhrase: StateFlow<String> = _lastRecognizedPhrase.asStateFlow()

    private val _lastResponse = MutableStateFlow("")
    val lastResponse: StateFlow<String> = _lastResponse.asStateFlow()

    private val _conversationHistory = MutableStateFlow<List<ConversationMessage>>(emptyList())
    val conversationHistory: StateFlow<List<ConversationMessage>> = _conversationHistory.asStateFlow()

    private val _isLiveMode = MutableStateFlow(false)
    val isLiveMode: StateFlow<Boolean> = _isLiveMode.asStateFlow()

    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()

    private val _isScreenContextEnabled = MutableStateFlow(false)
    val isScreenContextEnabled: StateFlow<Boolean> = _isScreenContextEnabled.asStateFlow()

    private val _liveCallDurationSeconds = MutableStateFlow(0L)
    val liveCallDurationSeconds: StateFlow<Long> = _liveCallDurationSeconds.asStateFlow()

    private val _recentLogs = MutableStateFlow<List<JarvisLogEntry>>(
        listOf(
            JarvisLogEntry(
                message = "JARVIS Core Initialized. Systems nominal.",
                isSystem = true
            )
        )
    )
    val recentLogs: StateFlow<List<JarvisLogEntry>> = _recentLogs.asStateFlow()

    fun updateStatus(status: VoiceStatus) {
        _voiceStatus.value = status
    }

    fun updateAmplitude(rmsLevel: Float) {
        // Normalize RMS dB (-2 to 10 typical in Android speech recognition) to 0f..1f
        val normalized = ((rmsLevel + 2f) / 12f).coerceIn(0f, 1f)
        _soundAmplitude.value = normalized
    }

    fun setRecognizedPhrase(phrase: String) {
        _lastRecognizedPhrase.value = phrase
        if (phrase.isNotBlank()) {
            addLog("Heard: \"$phrase\"", isCommand = true)
        }
    }

    fun setResponse(response: String) {
        _lastResponse.value = response
        if (response.isNotBlank()) {
            addLog("JARVIS: \"$response\"", isSystem = true)
        }
    }

    fun recordConversationTurn(userText: String, jarvisText: String) {
        if (userText.isBlank() && jarvisText.isBlank()) return
        val current = _conversationHistory.value.toMutableList()
        if (userText.isNotBlank()) {
            current.add(ConversationMessage(isUser = true, text = userText))
        }
        if (jarvisText.isNotBlank()) {
            current.add(ConversationMessage(isUser = false, text = jarvisText))
        }
        // Retain recent 30 conversation messages
        while (current.size > 30) {
            current.removeAt(0)
        }
        _conversationHistory.value = current
    }

    fun clearConversation() {
        _conversationHistory.value = emptyList()
    }

    fun setLiveMode(active: Boolean) {
        _isLiveMode.value = active
        if (!active) {
            _liveCallDurationSeconds.value = 0L
        }
    }

    fun setMicMuted(muted: Boolean) {
        _isMicMuted.value = muted
    }

    fun setScreenContextEnabled(enabled: Boolean) {
        _isScreenContextEnabled.value = enabled
    }

    fun updateCallDuration(seconds: Long) {
        _liveCallDurationSeconds.value = seconds
    }

    fun addLog(message: String, isCommand: Boolean = false, isSystem: Boolean = false) {
        val entry = JarvisLogEntry(message = message, isCommand = isCommand, isSystem = isSystem)
        val currentList = _recentLogs.value.toMutableList()
        currentList.add(0, entry)
        // Keep max 30 recent logs
        if (currentList.size > 30) {
            currentList.removeAt(currentList.lastIndex)
        }
        _recentLogs.value = currentList
    }

    fun clearLogs() {
        _recentLogs.value = listOf(
            JarvisLogEntry(message = "Telemetry logs cleared.", isSystem = true)
        )
    }
}
