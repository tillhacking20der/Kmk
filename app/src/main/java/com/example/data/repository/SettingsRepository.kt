package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isJarvisEnabled = MutableStateFlow(prefs.getBoolean(KEY_JARVIS_ENABLED, false))
    val isJarvisEnabled: StateFlow<Boolean> = _isJarvisEnabled.asStateFlow()

    private val _isSchoolMode = MutableStateFlow(prefs.getBoolean(KEY_SCHOOL_MODE, false))
    val isSchoolMode: StateFlow<Boolean> = _isSchoolMode.asStateFlow()

    private val _isWakeWordRequired = MutableStateFlow(prefs.getBoolean(KEY_WAKE_WORD_REQUIRED, true))
    val isWakeWordRequired: StateFlow<Boolean> = _isWakeWordRequired.asStateFlow()

    private val _isVoiceResponseEnabled = MutableStateFlow(prefs.getBoolean(KEY_VOICE_RESPONSE_ENABLED, true))
    val isVoiceResponseEnabled: StateFlow<Boolean> = _isVoiceResponseEnabled.asStateFlow()

    private val _speechLanguage = MutableStateFlow(prefs.getString(KEY_SPEECH_LANGUAGE, "en-US") ?: "en-US")
    val speechLanguage: StateFlow<String> = _speechLanguage.asStateFlow()

    fun setJarvisEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_JARVIS_ENABLED, enabled).apply()
        _isJarvisEnabled.value = enabled
    }

    fun setSchoolMode(active: Boolean) {
        prefs.edit().putBoolean(KEY_SCHOOL_MODE, active).apply()
        _isSchoolMode.value = active
        if (active) {
            // Activating school mode automatically disables active voice listening
            setJarvisEnabled(false)
        }
    }

    fun setWakeWordRequired(required: Boolean) {
        prefs.edit().putBoolean(KEY_WAKE_WORD_REQUIRED, required).apply()
        _isWakeWordRequired.value = required
    }

    fun setVoiceResponseEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VOICE_RESPONSE_ENABLED, enabled).apply()
        _isVoiceResponseEnabled.value = enabled
    }

    fun setSpeechLanguage(lang: String) {
        prefs.edit().putString(KEY_SPEECH_LANGUAGE, lang).apply()
        _speechLanguage.value = lang
    }

    companion object {
        private const val PREFS_NAME = "jarvis_settings"
        private const val KEY_JARVIS_ENABLED = "jarvis_enabled"
        private const val KEY_SCHOOL_MODE = "school_mode"
        private const val KEY_WAKE_WORD_REQUIRED = "wake_word_required"
        private const val KEY_VOICE_RESPONSE_ENABLED = "voice_response_enabled"
        private const val KEY_SPEECH_LANGUAGE = "speech_language"

        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
