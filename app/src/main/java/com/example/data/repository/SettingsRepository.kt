package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Master & Operational switches
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

    // AI API Key & Chat History Persistence
    private val _userApiKey = MutableStateFlow(prefs.getString(KEY_USER_API_KEY, "") ?: "")
    val userApiKey: StateFlow<String> = _userApiKey.asStateFlow()

    private val _saveChatHistory = MutableStateFlow(prefs.getBoolean(KEY_SAVE_CHAT_HISTORY, true))
    val saveChatHistory: StateFlow<Boolean> = _saveChatHistory.asStateFlow()

    // Voice & TTS Settings
    private val _ttsVoiceName = MutableStateFlow(prefs.getString(KEY_TTS_VOICE_NAME, "") ?: "")
    val ttsVoiceName: StateFlow<String> = _ttsVoiceName.asStateFlow()

    private val _ttsSpeed = MutableStateFlow(prefs.getFloat(KEY_TTS_SPEED, 1.0f))
    val ttsSpeed: StateFlow<Float> = _ttsSpeed.asStateFlow()

    private val _ttsPitch = MutableStateFlow(prefs.getFloat(KEY_TTS_PITCH, 1.0f))
    val ttsPitch: StateFlow<Float> = _ttsPitch.asStateFlow()

    // Appearance & Customization Settings
    private val _backgroundImageUri = MutableStateFlow(prefs.getString(KEY_BG_IMAGE_URI, null))
    val backgroundImageUri: StateFlow<String?> = _backgroundImageUri.asStateFlow()

    private val _builtInBackground = MutableStateFlow(prefs.getString(KEY_BUILTIN_BG, "arc_core") ?: "arc_core")
    val builtInBackground: StateFlow<String> = _builtInBackground.asStateFlow()

    private val _accentColorIndex = MutableStateFlow(prefs.getInt(KEY_ACCENT_COLOR_INDEX, 0))
    val accentColorIndex: StateFlow<Int> = _accentColorIndex.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean(KEY_DARK_MODE, true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _cardTransparency = MutableStateFlow(prefs.getFloat(KEY_CARD_TRANSPARENCY, 0.85f))
    val cardTransparency: StateFlow<Float> = _cardTransparency.asStateFlow()

    private val _fontSizeScale = MutableStateFlow(prefs.getString(KEY_FONT_SIZE_SCALE, "standard") ?: "standard")
    val fontSizeScale: StateFlow<String> = _fontSizeScale.asStateFlow()

    private val _fontStyle = MutableStateFlow(prefs.getString(KEY_FONT_STYLE, "monospace") ?: "monospace")
    val fontStyle: StateFlow<String> = _fontStyle.asStateFlow()

    private val _buttonStyle = MutableStateFlow(prefs.getString(KEY_BUTTON_STYLE, "glowing") ?: "glowing")
    val buttonStyle: StateFlow<String> = _buttonStyle.asStateFlow()

    private val _animationsEnabled = MutableStateFlow(prefs.getBoolean(KEY_ANIMATIONS_ENABLED, true))
    val animationsEnabled: StateFlow<Boolean> = _animationsEnabled.asStateFlow()

    // Updaters
    fun setJarvisEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_JARVIS_ENABLED, enabled).apply()
        _isJarvisEnabled.value = enabled
    }

    fun setSchoolMode(active: Boolean) {
        prefs.edit().putBoolean(KEY_SCHOOL_MODE, active).apply()
        _isSchoolMode.value = active
        if (active) {
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

    fun setUserApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString(KEY_USER_API_KEY, trimmed).apply()
        _userApiKey.value = trimmed
    }

    fun setSaveChatHistory(save: Boolean) {
        prefs.edit().putBoolean(KEY_SAVE_CHAT_HISTORY, save).apply()
        _saveChatHistory.value = save
    }

    fun setTtsVoiceName(voiceName: String) {
        prefs.edit().putString(KEY_TTS_VOICE_NAME, voiceName).apply()
        _ttsVoiceName.value = voiceName
    }

    fun setTtsSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        prefs.edit().putFloat(KEY_TTS_SPEED, clamped).apply()
        _ttsSpeed.value = clamped
    }

    fun setTtsPitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.5f, 2.0f)
        prefs.edit().putFloat(KEY_TTS_PITCH, clamped).apply()
        _ttsPitch.value = clamped
    }

    fun setBackgroundImageUri(uriString: String?) {
        prefs.edit().putString(KEY_BG_IMAGE_URI, uriString).apply()
        _backgroundImageUri.value = uriString
    }

    fun setBuiltInBackground(bgKey: String) {
        prefs.edit().putString(KEY_BUILTIN_BG, bgKey).apply()
        _builtInBackground.value = bgKey
    }

    fun setAccentColorIndex(index: Int) {
        prefs.edit().putInt(KEY_ACCENT_COLOR_INDEX, index).apply()
        _accentColorIndex.value = index
    }

    fun setDarkMode(dark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, dark).apply()
        _isDarkMode.value = dark
    }

    fun setCardTransparency(transparency: Float) {
        val clamped = transparency.coerceIn(0.3f, 1.0f)
        prefs.edit().putFloat(KEY_CARD_TRANSPARENCY, clamped).apply()
        _cardTransparency.value = clamped
    }

    fun setFontSizeScale(scale: String) {
        prefs.edit().putString(KEY_FONT_SIZE_SCALE, scale).apply()
        _fontSizeScale.value = scale
    }

    fun setFontStyle(style: String) {
        prefs.edit().putString(KEY_FONT_STYLE, style).apply()
        _fontStyle.value = style
    }

    fun setButtonStyle(style: String) {
        prefs.edit().putString(KEY_BUTTON_STYLE, style).apply()
        _buttonStyle.value = style
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ANIMATIONS_ENABLED, enabled).apply()
        _animationsEnabled.value = enabled
    }

    companion object {
        private const val PREFS_NAME = "jarvis_settings"
        private const val KEY_JARVIS_ENABLED = "jarvis_enabled"
        private const val KEY_SCHOOL_MODE = "school_mode"
        private const val KEY_WAKE_WORD_REQUIRED = "wake_word_required"
        private const val KEY_VOICE_RESPONSE_ENABLED = "voice_response_enabled"
        private const val KEY_SPEECH_LANGUAGE = "speech_language"
        private const val KEY_USER_API_KEY = "user_gemini_api_key"
        private const val KEY_SAVE_CHAT_HISTORY = "save_chat_history"
        private const val KEY_TTS_VOICE_NAME = "tts_voice_name"
        private const val KEY_TTS_SPEED = "tts_speed"
        private const val KEY_TTS_PITCH = "tts_pitch"
        private const val KEY_BG_IMAGE_URI = "bg_image_uri"
        private const val KEY_BUILTIN_BG = "builtin_bg"
        private const val KEY_ACCENT_COLOR_INDEX = "accent_color_index"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_CARD_TRANSPARENCY = "card_transparency"
        private const val KEY_FONT_SIZE_SCALE = "font_size_scale"
        private const val KEY_FONT_STYLE = "font_style"
        private const val KEY_BUTTON_STYLE = "button_style"
        private const val KEY_ANIMATIONS_ENABLED = "animations_enabled"

        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
