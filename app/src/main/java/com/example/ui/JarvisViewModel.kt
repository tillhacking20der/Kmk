package com.example.ui

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.JarvisApplication
import com.example.data.local.JarvisDatabase
import com.example.data.model.ChatMessageEntity
import com.example.data.model.CommandActionType
import com.example.data.model.CustomCommand
import com.example.data.model.InstalledAppInfo
import com.example.service.JarvisVoiceService
import com.example.state.ConversationMessage
import com.example.state.JarvisLogEntry
import com.example.state.JarvisStateHolder
import com.example.state.VoiceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as JarvisApplication
    private val repository = app.repository
    private val settingsRepository = app.settingsRepository
    private val database = JarvisDatabase.getDatabase(app)

    val voiceStatus: StateFlow<VoiceStatus> = JarvisStateHolder.voiceStatus
    val soundAmplitude: StateFlow<Float> = JarvisStateHolder.soundAmplitude
    val lastRecognizedPhrase: StateFlow<String> = JarvisStateHolder.lastRecognizedPhrase
    val lastResponse: StateFlow<String> = JarvisStateHolder.lastResponse
    val recentLogs: StateFlow<List<JarvisLogEntry>> = JarvisStateHolder.recentLogs
    val conversationHistory = JarvisStateHolder.conversationHistory

    val isJarvisEnabled: StateFlow<Boolean> = settingsRepository.isJarvisEnabled
    val isSchoolMode: StateFlow<Boolean> = settingsRepository.isSchoolMode
    val isWakeWordRequired: StateFlow<Boolean> = settingsRepository.isWakeWordRequired
    val isVoiceResponseEnabled: StateFlow<Boolean> = settingsRepository.isVoiceResponseEnabled
    val speechLanguage: StateFlow<String> = settingsRepository.speechLanguage

    // AI API Key & Chat History Persistence
    val userApiKey: StateFlow<String> = settingsRepository.userApiKey
    val saveChatHistory: StateFlow<Boolean> = settingsRepository.saveChatHistory

    // Voice & TTS Settings
    val ttsVoiceName: StateFlow<String> = settingsRepository.ttsVoiceName
    val ttsSpeed: StateFlow<Float> = settingsRepository.ttsSpeed
    val ttsPitch: StateFlow<Float> = settingsRepository.ttsPitch

    // Appearance & Customization Settings
    val backgroundImageUri: StateFlow<String?> = settingsRepository.backgroundImageUri
    val builtInBackground: StateFlow<String> = settingsRepository.builtInBackground
    val accentColorIndex: StateFlow<Int> = settingsRepository.accentColorIndex
    val isDarkMode: StateFlow<Boolean> = settingsRepository.isDarkMode
    val cardTransparency: StateFlow<Float> = settingsRepository.cardTransparency
    val fontSizeScale: StateFlow<String> = settingsRepository.fontSizeScale
    val fontStyle: StateFlow<String> = settingsRepository.fontStyle
    val buttonStyle: StateFlow<String> = settingsRepository.buttonStyle
    val animationsEnabled: StateFlow<Boolean> = settingsRepository.animationsEnabled

    // Live Voice / Phone Mode & Screen Context
    val isLiveMode: StateFlow<Boolean> = JarvisStateHolder.isLiveMode
    val isMicMuted: StateFlow<Boolean> = JarvisStateHolder.isMicMuted
    val isScreenContextEnabled: StateFlow<Boolean> = JarvisStateHolder.isScreenContextEnabled
    val liveCallDurationSeconds: StateFlow<Long> = JarvisStateHolder.liveCallDurationSeconds

    val allCommands: StateFlow<List<CustomCommand>> = repository.allCommands
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private var previewTts: TextToSpeech? = null
    private val _availableTtsVoices = MutableStateFlow<List<String>>(emptyList())
    val availableTtsVoices: StateFlow<List<String>> = _availableTtsVoices.asStateFlow()

    init {
        loadInstalledApps()
        loadChatHistoryIfEnabled()
        initVoicePreview()
    }

    private fun initVoicePreview() {
        try {
            previewTts = TextToSpeech(app) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    try {
                        val voices = previewTts?.voices?.map { it.name }?.sorted() ?: emptyList()
                        _availableTtsVoices.value = voices
                    } catch (e: Throwable) {
                        _availableTtsVoices.value = emptyList()
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun loadChatHistoryIfEnabled() {
        viewModelScope.launch {
            if (settingsRepository.saveChatHistory.value) {
                database.chatMessageDao().getAllMessages().collect { entities ->
                    if (entities.isNotEmpty() && JarvisStateHolder.conversationHistory.value.isEmpty()) {
                        val messages = entities.map {
                            ConversationMessage(
                                id = it.id,
                                isUser = it.role == "user",
                                text = it.text,
                                timestamp = it.timestamp
                            )
                        }
                        JarvisStateHolder.setConversationHistory(messages)
                        com.example.service.JarvisConversationEngine.loadHistoryFromDatabase(entities)
                    }
                }
            }
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _installedApps.value = repository.getInstalledApps()
        }
    }

    fun toggleJarvis(enabled: Boolean) {
        if (enabled) {
            if (isSchoolMode.value) {
                JarvisStateHolder.addLog("Cannot activate: School Mode is active. Turn off School Mode first.", isSystem = true)
                return
            }
            settingsRepository.setJarvisEnabled(true)
            JarvisVoiceService.startService(app)
        } else {
            settingsRepository.setJarvisEnabled(false)
            JarvisVoiceService.stopService(app)
            JarvisStateHolder.updateStatus(VoiceStatus.DISABLED)
            JarvisStateHolder.addLog("JARVIS disabled by user.", isSystem = true)
        }
    }

    fun toggleSchoolMode(active: Boolean) {
        settingsRepository.setSchoolMode(active)
        if (active) {
            JarvisVoiceService.stopService(app)
            JarvisStateHolder.updateStatus(VoiceStatus.SCHOOL_MODE)
            JarvisStateHolder.addLog("School Mode activated. Microphone and voice service stopped.", isSystem = true)
        } else {
            JarvisStateHolder.updateStatus(VoiceStatus.DISABLED)
            JarvisStateHolder.addLog("School Mode deactivated. JARVIS ready to activate.", isSystem = true)
        }
    }

    fun setWakeWordRequired(required: Boolean) {
        settingsRepository.setWakeWordRequired(required)
        JarvisStateHolder.addLog(
            if (required) "Wake word \"Jarvis\" required before commands."
            else "Direct listening mode active (no wake word required).",
            isSystem = true
        )
    }

    fun setVoiceResponseEnabled(enabled: Boolean) {
        settingsRepository.setVoiceResponseEnabled(enabled)
    }

    fun setSpeechLanguage(lang: String) {
        settingsRepository.setSpeechLanguage(lang)
        JarvisStateHolder.addLog("Speech language set to $lang.", isSystem = true)
    }

    // AI API Key and Chat History Settings
    fun setUserApiKey(key: String) {
        settingsRepository.setUserApiKey(key)
        JarvisStateHolder.addLog("Gemini API key updated.", isSystem = true)
    }

    fun setSaveChatHistory(save: Boolean) {
        settingsRepository.setSaveChatHistory(save)
    }

    // Voice / TTS Settings
    fun setTtsVoiceName(voiceName: String) {
        settingsRepository.setTtsVoiceName(voiceName)
    }

    fun setTtsSpeed(speed: Float) {
        settingsRepository.setTtsSpeed(speed)
    }

    fun setTtsPitch(pitch: Float) {
        settingsRepository.setTtsPitch(pitch)
    }

    fun testTtsVoice(sampleText: String = "Hello, all JARVIS voice systems are operational and performing at peak capacity.") {
        if (previewTts == null) {
            initVoicePreview()
        }
        previewTts?.let { tts ->
            tts.setPitch(settingsRepository.ttsPitch.value)
            tts.setSpeechRate(settingsRepository.ttsSpeed.value)
            val voiceName = settingsRepository.ttsVoiceName.value
            if (voiceName.isNotBlank()) {
                try {
                    tts.voices?.find { it.name == voiceName }?.let { tts.voice = it }
                } catch (e: Throwable) {
                    // Ignore
                }
            }
            tts.speak(sampleText, TextToSpeech.QUEUE_FLUSH, null, "test_voice_${System.currentTimeMillis()}")
        }
    }

    // Appearance Settings
    fun setBackgroundImageUri(uri: String?) {
        settingsRepository.setBackgroundImageUri(uri)
    }

    fun setBuiltInBackground(bgKey: String) {
        settingsRepository.setBuiltInBackground(bgKey)
    }

    fun setAccentColorIndex(index: Int) {
        settingsRepository.setAccentColorIndex(index)
    }

    fun setDarkMode(dark: Boolean) {
        settingsRepository.setDarkMode(dark)
    }

    fun setCardTransparency(transparency: Float) {
        settingsRepository.setCardTransparency(transparency)
    }

    fun setFontSizeScale(scale: String) {
        settingsRepository.setFontSizeScale(scale)
    }

    fun setFontStyle(style: String) {
        settingsRepository.setFontStyle(style)
    }

    fun setButtonStyle(style: String) {
        settingsRepository.setButtonStyle(style)
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        settingsRepository.setAnimationsEnabled(enabled)
    }

    fun startLiveMode() {
        if (isSchoolMode.value) {
            JarvisStateHolder.addLog("Cannot start Live Mode: School Mode is active.", isSystem = true)
            return
        }
        JarvisVoiceService.startLiveMode(app)
    }

    fun stopLiveMode() {
        JarvisVoiceService.stopLiveMode(app)
    }

    fun toggleMicMute() {
        JarvisVoiceService.toggleMicMute(app)
    }

    fun stopScreenContext() {
        com.example.service.ScreenCaptureManager.stop()
        JarvisVoiceService.stopScreenContext(app)
    }

    fun addCustomCommand(
        trigger: String,
        actionType: CommandActionType,
        targetPackage: String? = null,
        targetAppName: String? = null,
        response: String? = null
    ) {
        viewModelScope.launch {
            val newCommand = CustomCommand(
                triggerPhrase = trigger.trim(),
                actionType = actionType.name,
                targetPackage = targetPackage,
                targetAppName = targetAppName,
                responseText = response?.trim()?.ifBlank { null },
                isEnabled = true
            )
            repository.insertCommand(newCommand)
            JarvisStateHolder.addLog("Added custom command: \"$trigger\"", isSystem = true)
        }
    }

    fun updateCustomCommand(command: CustomCommand) {
        viewModelScope.launch {
            repository.updateCommand(command)
            JarvisStateHolder.addLog("Updated command: \"${command.triggerPhrase}\"", isSystem = true)
        }
    }

    fun deleteCustomCommand(command: CustomCommand) {
        viewModelScope.launch {
            repository.deleteCommand(command)
            JarvisStateHolder.addLog("Deleted command: \"${command.triggerPhrase}\"", isSystem = true)
        }
    }

    fun toggleCommandEnabled(command: CustomCommand) {
        viewModelScope.launch {
            val updated = command.copy(isEnabled = !command.isEnabled)
            repository.updateCommand(updated)
        }
    }

    fun clearLogs() {
        JarvisStateHolder.clearLogs()
    }

    fun clearConversation() {
        JarvisStateHolder.clearConversation()
        com.example.service.JarvisConversationEngine.clearHistory()
        viewModelScope.launch {
            try {
                database.chatMessageDao().clearAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun executeQuickPrompt(prompt: String) {
        if (!isJarvisEnabled.value) {
            toggleJarvis(true)
        }
        JarvisVoiceService.executeCommand(app, prompt)
    }

    override fun onCleared() {
        super.onCleared()
        try {
            previewTts?.stop()
            previewTts?.shutdown()
            previewTts = null
        } catch (e: Throwable) {
            // Safe cleanup
        }
    }
}

class JarvisViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JarvisViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return JarvisViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
