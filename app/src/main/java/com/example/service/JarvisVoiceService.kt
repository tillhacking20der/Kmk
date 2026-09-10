package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.JarvisDatabase
import com.example.data.model.CommandActionType
import com.example.data.repository.JarvisRepository
import com.example.data.repository.SettingsRepository
import com.example.state.JarvisStateHolder
import com.example.state.VoiceStatus
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale

class JarvisVoiceService : Service(), TextToSpeech.OnInitListener {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private lateinit var settingsRepo: SettingsRepository
    private lateinit var jarvisRepo: JarvisRepository

    private var isServiceRunning = false
    private var isSpeaking = false
    private var isConversationActive = false
    private var isLiveMode = false
    private var isMicMuted = false
    private var callDurationSeconds = 0L
    private var callTimerRunnable: Runnable? = null
    private var conversationTimeoutRunnable: Runnable? = null
    private var consecutiveErrorCount = 0

    override fun onCreate() {
        super.onCreate()
        settingsRepo = SettingsRepository.getInstance(this)
        val database = JarvisDatabase.getDatabase(this)
        jarvisRepo = JarvisRepository(this, database.customCommandDao())

        createNotificationChannel()
        initTextToSpeech()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_EXECUTE_COMMAND -> {
                val commandText = intent?.getStringExtra(EXTRA_COMMAND)
                if (!commandText.isNullOrBlank()) {
                    JarvisStateHolder.setRecognizedPhrase(commandText)
                    handleRecognizedSpeech(commandText)
                }
                return START_NOT_STICKY
            }
            ACTION_STOP -> {
                stopVoiceService()
                return START_NOT_STICKY
            }
            ACTION_START_LIVE_MODE -> {
                startLiveModeSession()
                return START_STICKY
            }
            ACTION_STOP_LIVE_MODE -> {
                endLiveMode(speakEnding = true)
                return START_STICKY
            }
            ACTION_TOGGLE_MIC_MUTE -> {
                toggleMicMute()
                return START_STICKY
            }
            ACTION_STOP_SCREEN_CONTEXT -> {
                stopScreenContextSession()
                return START_STICKY
            }
            ACTION_TOGGLE_SCHOOL_MODE -> {
                val currentSchoolMode = settingsRepo.isSchoolMode.value
                val newSchoolMode = !currentSchoolMode
                settingsRepo.setSchoolMode(newSchoolMode)
                if (newSchoolMode) {
                    JarvisStateHolder.updateStatus(VoiceStatus.SCHOOL_MODE)
                    JarvisStateHolder.addLog("School Mode activated. Microphone strictly stopped.", isSystem = true)
                    speakResponse("School Mode activated.") {
                        stopVoiceService()
                    }
                } else {
                    JarvisStateHolder.addLog("School Mode deactivated.", isSystem = true)
                    speakResponse("School Mode deactivated.")
                }
                return START_NOT_STICKY
            }
            ACTION_START -> {
                if (settingsRepo.isSchoolMode.value) {
                    JarvisStateHolder.updateStatus(VoiceStatus.SCHOOL_MODE)
                    JarvisStateHolder.addLog("Cannot start: School Mode is active.", isSystem = true)
                    stopSelf()
                    return START_NOT_STICKY
                }

                if (!hasAudioPermission()) {
                    JarvisStateHolder.updateStatus(VoiceStatus.DISABLED)
                    JarvisStateHolder.addLog("Microphone permission required to run JARVIS.", isSystem = true)
                    stopSelf()
                    return START_NOT_STICKY
                }

                startInForeground()
                isServiceRunning = true
                settingsRepo.setJarvisEnabled(true)
                consecutiveErrorCount = 0

                JarvisStateHolder.updateStatus(
                    if (settingsRepo.isWakeWordRequired.value) VoiceStatus.WAITING_FOR_WAKE_WORD
                    else VoiceStatus.LISTENING
                )
                JarvisStateHolder.addLog("JARVIS Voice Assistant online. Listening active.", isSystem = true)

                mainHandler.postDelayed({
                    initializeAndStartSpeechRecognizer()
                }, 300)

                return START_STICKY
            }
        }

        return START_STICKY
    }

    private fun startInForeground() {
        updateForegroundServiceType()
    }

    private fun initTextToSpeech() {
        try {
            textToSpeech = TextToSpeech(this, this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.let { tts ->
                val selectedLang = settingsRepo.speechLanguage.value
                val locale = getLocaleForLanguage(selectedLang)
                val langResult = tts.setLanguage(locale)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.language = Locale.US
                }
                tts.setPitch(0.95f)
                tts.setSpeechRate(1.05f)
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeaking = true
                        JarvisStateHolder.updateStatus(VoiceStatus.SPEAKING)
                    }

                    override fun onDone(utteranceId: String?) {
                        isSpeaking = false
                        mainHandler.post {
                            if (isServiceRunning && !settingsRepo.isSchoolMode.value) {
                                if (isLiveMode) {
                                    JarvisStateHolder.updateStatus(VoiceStatus.LIVE_MODE)
                                    if (!isMicMuted) {
                                        restartListeningSafely(delayMs = 200)
                                    }
                                } else {
                                    JarvisStateHolder.updateStatus(
                                        if (isConversationActive) VoiceStatus.CONVERSING
                                        else if (settingsRepo.isWakeWordRequired.value) VoiceStatus.WAITING_FOR_WAKE_WORD
                                        else VoiceStatus.LISTENING
                                    )
                                    if (isConversationActive) {
                                        startConversationTimeout(10000L)
                                    }
                                    restartListeningSafely(delayMs = 250)
                                }
                            }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        isSpeaking = false
                        mainHandler.post {
                            if (isServiceRunning && !settingsRepo.isSchoolMode.value) {
                                if (isLiveMode) {
                                    JarvisStateHolder.updateStatus(VoiceStatus.LIVE_MODE)
                                    if (!isMicMuted) {
                                        restartListeningSafely(delayMs = 200)
                                    }
                                } else {
                                    if (isConversationActive) {
                                        startConversationTimeout(10000L)
                                    }
                                    restartListeningSafely(delayMs = 250)
                                }
                            }
                        }
                    }
                })
                isTtsReady = true
            }
        }
    }

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun initializeAndStartSpeechRecognizer() {
        if (!isServiceRunning || settingsRepo.isSchoolMode.value || !hasAudioPermission()) {
            return
        }

        mainHandler.post {
            try {
                if (speechRecognizer != null) {
                    try {
                        speechRecognizer?.destroy()
                    } catch (e: Exception) {
                        // Safe destroy
                    }
                    speechRecognizer = null
                }

                if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                    JarvisStateHolder.addLog("Speech recognition is not available on this device.", isSystem = true)
                    JarvisStateHolder.updateStatus(VoiceStatus.DISABLED)
                    stopVoiceService()
                    return@post
                }

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                    setRecognitionListener(JarvisRecognitionListener())
                }

                beginListeningIntent()
            } catch (e: Exception) {
                e.printStackTrace()
                JarvisStateHolder.addLog("Speech recognizer init error: ${e.localizedMessage}", isSystem = true)
                restartListeningSafely(delayMs = 2000)
            }
        }
    }

    private fun beginListeningIntent() {
        if (!isServiceRunning || settingsRepo.isSchoolMode.value || isSpeaking || isMicMuted) {
            return
        }

        try {
            val lang = settingsRepo.speechLanguage.value
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            restartListeningSafely(delayMs = 1000)
        }
    }

    private fun restartListeningSafely(delayMs: Long = 300) {
        if (!isServiceRunning || settingsRepo.isSchoolMode.value || isMicMuted) return

        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (isServiceRunning && !settingsRepo.isSchoolMode.value && !isSpeaking && !isMicMuted) {
                try {
                    beginListeningIntent()
                } catch (e: Exception) {
                    initializeAndStartSpeechRecognizer()
                }
            }
        }, delayMs)
    }

    private inner class JarvisRecognitionListener : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            consecutiveErrorCount = 0
            JarvisStateHolder.updateAmplitude(0f)
            if (!isSpeaking) {
                JarvisStateHolder.updateStatus(
                    if (isLiveMode) VoiceStatus.LIVE_MODE
                    else if (isConversationActive) VoiceStatus.CONVERSING
                    else if (settingsRepo.isWakeWordRequired.value) VoiceStatus.WAITING_FOR_WAKE_WORD
                    else VoiceStatus.LISTENING
                )
            }
        }

        override fun onBeginningOfSpeech() {
            cancelConversationTimeout()
            JarvisStateHolder.updateStatus(VoiceStatus.LISTENING)
        }

        override fun onRmsChanged(rmsdB: Float) {
            if (!isSpeaking) {
                JarvisStateHolder.updateAmplitude(rmsdB)
            }
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            JarvisStateHolder.updateAmplitude(0f)
            JarvisStateHolder.updateStatus(VoiceStatus.PROCESSING)
        }

        override fun onError(error: Int) {
            JarvisStateHolder.updateAmplitude(0f)

            if (!isServiceRunning || settingsRepo.isSchoolMode.value) return

            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    // Normal idle timeout while waiting for wake word, quietly restart
                    restartListeningSafely(delayMs = 200)
                }
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                SpeechRecognizer.ERROR_CLIENT -> {
                    consecutiveErrorCount++
                    val backoff = if (consecutiveErrorCount > 5) 2000L else 500L
                    mainHandler.postDelayed({
                        initializeAndStartSpeechRecognizer()
                    }, backoff)
                }
                SpeechRecognizer.ERROR_AUDIO,
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                SpeechRecognizer.ERROR_SERVER -> {
                    consecutiveErrorCount++
                    restartListeningSafely(delayMs = 1200)
                }
                else -> {
                    restartListeningSafely(delayMs = 500)
                }
            }
        }

        override fun onResults(results: Bundle?) {
            JarvisStateHolder.updateAmplitude(0f)
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedText = matches?.firstOrNull()?.trim() ?: ""

            if (recognizedText.isNotBlank()) {
                JarvisStateHolder.setRecognizedPhrase(recognizedText)
                handleRecognizedSpeech(recognizedText)
            } else {
                restartListeningSafely(delayMs = 200)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partialText = partialMatches?.firstOrNull()?.trim() ?: ""
            if (partialText.isNotBlank()) {
                // If wake word detected in partial stream, give instant visual feedback
                if (partialText.contains("jarvis", ignoreCase = true)) {
                    JarvisStateHolder.updateStatus(VoiceStatus.LISTENING)
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun handleRecognizedSpeech(rawText: String) {
        // In Live Mode, user can speak continuously without saying "Jarvis"
        if (isLiveMode) {
            if (isMicMuted) return
            val norm = CommandParser.normalize(rawText)
            if (isEndLiveModeCommand(norm)) {
                endLiveMode(speakEnding = true)
                return
            }
            if (norm == "mute microphone" || norm == "mute mic") {
                toggleMicMute()
                return
            }
            if (norm.isBlank()) {
                restartListeningSafely(delayMs = 200)
                return
            }
            processParsedCommand(norm, rawText)
            return
        }

        val parseResult = CommandParser.parse(rawText)
        val wakeWordRequired = settingsRepo.isWakeWordRequired.value

        // Check wake word condition: if wake word is required, we are not already conversing,
        // and wake word wasn't spoken, safely resume waiting
        if (wakeWordRequired && !isConversationActive && !parseResult.hasWakeWord) {
            restartListeningSafely(delayMs = 250)
            return
        }

        // Cancel pending conversation timeout while actively processing
        cancelConversationTimeout()

        // If user said only the wake word "Jarvis"
        if (parseResult.isWakeWordOnly) {
            isConversationActive = true
            JarvisStateHolder.updateStatus(VoiceStatus.CONVERSING)
            val response = "Yes?"
            JarvisStateHolder.setResponse(response)
            JarvisStateHolder.recordConversationTurn(rawText, response)
            updateForegroundNotification("In Conversation - Listening")
            speakResponse(response) {
                startConversationTimeout(10000L)
            }
            return
        }

        val command = parseResult.normalizedCommand
        if (command.isBlank()) {
            if (isConversationActive) {
                startConversationTimeout(10000L)
            }
            restartListeningSafely(delayMs = 200)
            return
        }

        processParsedCommand(command, rawText)
    }

    private fun startConversationTimeout(timeoutMs: Long = 10000L) {
        cancelConversationTimeout()
        conversationTimeoutRunnable = Runnable {
            if (isConversationActive) {
                isConversationActive = false
                if (isServiceRunning && !settingsRepo.isSchoolMode.value) {
                    JarvisStateHolder.updateStatus(
                        if (settingsRepo.isWakeWordRequired.value) VoiceStatus.WAITING_FOR_WAKE_WORD
                        else VoiceStatus.LISTENING
                    )
                    updateForegroundNotification("Online - Waiting for \"Jarvis\"")
                    restartListeningSafely(delayMs = 200)
                }
            }
        }
        mainHandler.postDelayed(conversationTimeoutRunnable!!, timeoutMs)
    }

    private fun cancelConversationTimeout() {
        conversationTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        conversationTimeoutRunnable = null
    }

    private fun processParsedCommand(command: String, rawSpeech: String) {
        serviceScope.launch {
            JarvisStateHolder.updateStatus(VoiceStatus.PROCESSING)

            // 0. Live Mode termination commands
            if (isLiveMode && isEndLiveModeCommand(command)) {
                endLiveMode(speakEnding = true)
                return@launch
            }

            // 1. Built-in: Stop listening / Sleep commands
            if (command == "stop listening" ||
                command == "go to sleep" ||
                command == "sleep" ||
                command == "stop"
            ) {
                isConversationActive = false
                cancelConversationTimeout()
                val response = if (command.contains("sleep")) {
                    "Going to sleep. Say 'Jarvis' whenever you need me."
                } else {
                    "Stopped listening. Say 'Jarvis' to speak again."
                }
                JarvisStateHolder.setResponse(response)
                JarvisStateHolder.recordConversationTurn(rawSpeech, response)
                JarvisStateHolder.updateStatus(VoiceStatus.WAITING_FOR_WAKE_WORD)
                updateForegroundNotification("Online - Waiting for \"Jarvis\"")
                speakResponse(response)
                return@launch
            }

            // 2. Built-in: Turn off JARVIS completely
            if (command == "turn off jarvis" ||
                command == "deactivate jarvis" ||
                command == "shutdown" ||
                command == "exit jarvis"
            ) {
                isConversationActive = false
                cancelConversationTimeout()
                settingsRepo.setJarvisEnabled(false)
                val response = "Deactivating voice assistant."
                JarvisStateHolder.setResponse(response)
                JarvisStateHolder.recordConversationTurn(rawSpeech, response)
                speakResponse(response) {
                    stopVoiceService()
                }
                return@launch
            }

            // 3. Built-in: School Mode commands
            if (command == "activate school mode" ||
                command == "enable school mode" ||
                command == "turn on school mode" ||
                command == "school mode on" ||
                command == "school mode"
            ) {
                isConversationActive = false
                cancelConversationTimeout()
                settingsRepo.setSchoolMode(true)
                JarvisStateHolder.updateStatus(VoiceStatus.SCHOOL_MODE)
                val response = "School Mode activated."
                JarvisStateHolder.setResponse(response)
                JarvisStateHolder.recordConversationTurn(rawSpeech, response)
                speakResponse(response) {
                    stopVoiceService()
                }
                return@launch
            }

            if (command == "deactivate school mode" ||
                command == "disable school mode" ||
                command == "turn off school mode" ||
                command == "school mode off"
            ) {
                settingsRepo.setSchoolMode(false)
                val response = "School Mode deactivated."
                JarvisStateHolder.setResponse(response)
                JarvisStateHolder.recordConversationTurn(rawSpeech, response)
                speakResponse(response)
                return@launch
            }

            // 4. Built-in: Go Home
            if (command == "go home" ||
                command == "home screen" ||
                command == "open home" ||
                command == "home"
            ) {
                isConversationActive = false
                cancelConversationTimeout()
                val launched = launchHomeScreen()
                val response = if (launched) "Going home." else "I couldn't navigate home."
                JarvisStateHolder.setResponse(response)
                JarvisStateHolder.recordConversationTurn(rawSpeech, response)
                speakResponse(response)
                return@launch
            }

            // 5. Custom Commands from local Room Database
            val customCommands = jarvisRepo.getEnabledCommandsList()
            val matchedCustom = customCommands.firstOrNull { cmd ->
                val cleanTrigger = CommandParser.normalize(cmd.triggerPhrase)
                cleanTrigger.isNotBlank() && (command == cleanTrigger || command.contains(cleanTrigger))
            }

            if (matchedCustom != null) {
                isConversationActive = false
                cancelConversationTimeout()
                executeCustomCommand(matchedCustom)
                return@launch
            }

            // 6. Dynamic App Launching: "open [app]", "launch [app]", "start [app]", "run [app]", "play [app]"
            val appQuery = CommandParser.extractAppQuery(command)
            if (appQuery != null) {
                isConversationActive = false
                cancelConversationTimeout()
                if (appQuery.isBlank()) {
                    val response = "Which application would you like me to open?"
                    JarvisStateHolder.setResponse(response)
                    JarvisStateHolder.recordConversationTurn(rawSpeech, response)
                    speakResponse(response)
                    return@launch
                }

                // Explicit requirement: "Jarvis, open Google" -> open Google Chrome or default browser
                if (appQuery == "google") {
                    handleOpenGoogle()
                    return@launch
                }

                // Dynamic search for matching launchable app
                val foundApp = jarvisRepo.findAppByName(appQuery)
                if (foundApp != null) {
                    val launched = launchAppByPackage(foundApp.packageName)
                    val response = if (launched) "Opening ${foundApp.label}." else "I couldn't open ${foundApp.label}."
                    JarvisStateHolder.setResponse(response)
                    JarvisStateHolder.recordConversationTurn(rawSpeech, response)
                    speakResponse(response)
                } else {
                    val response = "I couldn't find an installed application named $appQuery."
                    JarvisStateHolder.setResponse(response)
                    JarvisStateHolder.recordConversationTurn(rawSpeech, response)
                    speakResponse(response)
                }
                return@launch
            }

            // 7. Conversational AI Assistant (Device Telemetry, Multi-turn History, Knowledge & Gemini)
            if (!isLiveMode) {
                isConversationActive = true
                updateForegroundNotification("In Conversation - Thinking")
            } else {
                updateForegroundServiceType()
            }
            val answer = JarvisConversationEngine.processQuery(this@JarvisVoiceService, rawSpeech)
            JarvisStateHolder.setResponse(answer)
            JarvisStateHolder.recordConversationTurn(rawSpeech, answer)
            if (!isLiveMode) {
                updateForegroundNotification("In Conversation - Responding")
            }
            speakResponse(answer) {
                if (isServiceRunning && !settingsRepo.isSchoolMode.value) {
                    if (isLiveMode) {
                        JarvisStateHolder.updateStatus(VoiceStatus.LIVE_MODE)
                        if (!isMicMuted) {
                            restartListeningSafely(delayMs = 200)
                        }
                    } else {
                        isConversationActive = true
                        startConversationTimeout(10000L)
                    }
                }
            }
        }
    }

    private fun handleOpenGoogle() {
        val launched = launchGoogleOrBrowser()
        if (launched) {
            val response = "Opening Google."
            JarvisStateHolder.setResponse(response)
            speakResponse(response)
        } else {
            val response = "I couldn't find that command."
            JarvisStateHolder.setResponse(response)
            speakResponse(response)
        }
    }

    private fun launchGoogleOrBrowser(): Boolean {
        // 1. Try Google Chrome or Google QuickSearch app
        val googlePackages = listOf(
            "com.android.chrome",
            "com.google.android.googlequicksearchbox",
            "com.google.android.apps.searchlite",
            "com.chrome.beta",
            "com.mi.globalbrowser"
        )
        for (pkg in googlePackages) {
            if (launchAppByPackage(pkg)) return true
        }

        // 2. Try any installed browser app
        val browserApp = runBlocking(Dispatchers.IO) {
            jarvisRepo.getInstalledApps().firstOrNull {
                it.label.contains("Chrome", ignoreCase = true) ||
                it.label.contains("Browser", ignoreCase = true) ||
                it.packageName.contains("chrome") ||
                it.packageName.contains("browser")
            }
        }
        if (browserApp != null && launchAppByPackage(browserApp.packageName)) {
            return true
        }

        // 3. Fallback: Launch default browser with google.com
        return try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(browserIntent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun executeCustomCommand(command: com.example.data.model.CustomCommand) {
        when (command.actionType) {
            CommandActionType.OPEN_APP.name -> {
                val pkg = command.targetPackage
                if (!pkg.isNullOrBlank()) {
                    val launched = launchAppByPackage(pkg)
                    val response = command.responseText ?: if (launched) "Opening ${command.targetAppName ?: "app"}." else "I couldn't find that command."
                    JarvisStateHolder.setResponse(response)
                    speakResponse(response)
                } else {
                    val response = "I couldn't find that command."
                    JarvisStateHolder.setResponse(response)
                    speakResponse(response)
                }
            }
            CommandActionType.GO_HOME.name -> {
                val launched = launchHomeScreen()
                val response = command.responseText ?: if (launched) "Going home." else "I couldn't find that command."
                JarvisStateHolder.setResponse(response)
                speakResponse(response)
            }
            CommandActionType.ACTIVATE_SCHOOL_MODE.name -> {
                settingsRepo.setSchoolMode(true)
                val response = command.responseText ?: "School Mode activated."
                JarvisStateHolder.setResponse(response)
                speakResponse(response) {
                    stopVoiceService()
                }
            }
            CommandActionType.DEACTIVATE_SCHOOL_MODE.name -> {
                settingsRepo.setSchoolMode(false)
                val response = command.responseText ?: "School Mode deactivated."
                JarvisStateHolder.setResponse(response)
                speakResponse(response)
            }
            CommandActionType.SPEAK_TEXT.name -> {
                val response = command.responseText ?: "Command executed."
                JarvisStateHolder.setResponse(response)
                speakResponse(response)
            }
            else -> {
                val response = "I couldn't find that command."
                JarvisStateHolder.setResponse(response)
                speakResponse(response)
            }
        }
    }

    private fun launchAppByPackage(packageName: String): Boolean {
        return try {
            val pm = packageManager
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                )
                startActivity(launchIntent)
                true
            } else {
                // Secondary check: search activities with CATEGORY_LAUNCHER
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setPackage(packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val resolveInfos = pm.queryIntentActivities(intent, 0)
                if (resolveInfos.isNotEmpty()) {
                    val actInfo = resolveInfos[0].activityInfo
                    val explicitIntent = Intent().apply {
                        setClassName(actInfo.packageName, actInfo.name)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(explicitIntent)
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // PendingIntent fallback for restricted background launches
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    val pending = PendingIntent.getActivity(
                        this,
                        (0..9999).random(),
                        launchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    pending.send()
                    return true
                }
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
            false
        }
    }

    private fun launchHomeScreen(): Boolean {
        return try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun speakResponse(text: String, onFinished: (() -> Unit)? = null) {
        val voiceResponseEnabled = settingsRepo.isVoiceResponseEnabled.value

        if (!voiceResponseEnabled || text.isBlank()) {
            onFinished?.invoke()
            if (isServiceRunning && !settingsRepo.isSchoolMode.value) {
                restartListeningSafely(delayMs = 400)
            }
            return
        }

        try {
            // Stop speech recognizer temporarily so JARVIS does not listen to its own voice
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                // Safe ignore
            }

            isSpeaking = true
            JarvisStateHolder.updateStatus(VoiceStatus.SPEAKING)

            textToSpeech?.let { tts ->
                tts.setPitch(settingsRepo.ttsPitch.value)
                tts.setSpeechRate(settingsRepo.ttsSpeed.value)
                val voiceName = settingsRepo.ttsVoiceName.value
                if (voiceName.isNotBlank()) {
                    try {
                        val targetVoice = tts.voices?.find { it.name == voiceName }
                        if (targetVoice != null) {
                            tts.voice = targetVoice
                        }
                    } catch (e: Throwable) {
                        // Safe fallback to default voice
                    }
                }
            }

            if (isTtsReady && textToSpeech != null) {
                val utteranceId = "jarvis_tts_${System.currentTimeMillis()}"
                textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            } else {
                mainHandler.postDelayed({
                    if (isTtsReady && textToSpeech != null) {
                        val utteranceId = "jarvis_tts_${System.currentTimeMillis()}"
                        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                    }
                }, 750)
            }

            // Fallback timer if onFinished callback provided
            if (onFinished != null) {
                val estimatedDurationMs = ((text.length * 80L) + 600L).coerceAtLeast(1200L)
                mainHandler.postDelayed({
                    onFinished.invoke()
                }, estimatedDurationMs)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isSpeaking = false
            onFinished?.invoke()
            restartListeningSafely(delayMs = 400)
        }
    }

    private fun stopVoiceService() {
        if (isLiveMode) {
            endLiveMode(speakEnding = false)
        }
        ScreenCaptureManager.stop()
        stopCallDurationTimer()

        isServiceRunning = false
        settingsRepo.setJarvisEnabled(false)
        JarvisStateHolder.updateStatus(
            if (settingsRepo.isSchoolMode.value) VoiceStatus.SCHOOL_MODE else VoiceStatus.DISABLED
        )
        JarvisStateHolder.updateAmplitude(0f)
        cancelConversationTimeout()
        mainHandler.removeCallbacksAndMessages(null)

        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            // Safe cleanup
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        if (isLiveMode) {
            endLiveMode(speakEnding = false)
        }
        ScreenCaptureManager.stop()
        stopCallDurationTimer()
        cancelConversationTimeout()
        mainHandler.removeCallbacksAndMessages(null)
        serviceScope.cancel()

        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            // Safe cleanup
        }

        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
        } catch (e: Exception) {
            // Safe cleanup
        }

        JarvisStateHolder.updateAmplitude(0f)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ==========================================
    // Live Mode & Screen Context Management
    // ==========================================

    private fun startLiveModeSession() {
        if (settingsRepo.isSchoolMode.value) {
            JarvisStateHolder.updateStatus(VoiceStatus.SCHOOL_MODE)
            JarvisStateHolder.addLog("Cannot start Live Mode: School Mode is active.", isSystem = true)
            return
        }

        if (!hasAudioPermission()) {
            JarvisStateHolder.addLog("Microphone permission required for Live Mode.", isSystem = true)
            return
        }

        if (!isServiceRunning) {
            isServiceRunning = true
            settingsRepo.setJarvisEnabled(true)
        }

        isLiveMode = true
        isMicMuted = false
        isConversationActive = false
        cancelConversationTimeout()

        JarvisStateHolder.setLiveMode(true)
        JarvisStateHolder.setMicMuted(false)
        JarvisStateHolder.updateStatus(VoiceStatus.LIVE_MODE)
        JarvisStateHolder.addLog("Live Conversation Mode active. Continuous phone dialogue initiated.", isSystem = true)

        startCallDurationTimer()
        updateForegroundServiceType()

        speakResponse("Live Conversation Mode active. I am listening, sir.") {
            restartListeningSafely(delayMs = 200)
        }
    }

    private fun endLiveMode(speakEnding: Boolean = true) {
        if (!isLiveMode) return
        isLiveMode = false
        isMicMuted = false
        JarvisStateHolder.setLiveMode(false)
        JarvisStateHolder.setMicMuted(false)
        stopCallDurationTimer()

        // Privacy rule: Stop screen capture immediately when Live Mode ends
        ScreenCaptureManager.stop()
        JarvisStateHolder.setScreenContextEnabled(false)
        JarvisStateHolder.addLog("Live Mode ended.", isSystem = true)

        if (speakEnding) {
            speakResponse("Live call ended. At your service, sir.") {
                if (isServiceRunning && !settingsRepo.isSchoolMode.value) {
                    JarvisStateHolder.updateStatus(
                        if (settingsRepo.isWakeWordRequired.value) VoiceStatus.WAITING_FOR_WAKE_WORD
                        else VoiceStatus.LISTENING
                    )
                    updateForegroundServiceType()
                    restartListeningSafely(delayMs = 250)
                }
            }
        } else {
            if (isServiceRunning && !settingsRepo.isSchoolMode.value) {
                JarvisStateHolder.updateStatus(
                    if (settingsRepo.isWakeWordRequired.value) VoiceStatus.WAITING_FOR_WAKE_WORD
                    else VoiceStatus.LISTENING
                )
                updateForegroundServiceType()
                restartListeningSafely(delayMs = 250)
            }
        }
    }

    private fun toggleMicMute() {
        if (!isLiveMode) return
        isMicMuted = !isMicMuted
        JarvisStateHolder.setMicMuted(isMicMuted)

        if (isMicMuted) {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {}
            JarvisStateHolder.addLog("Live Mode: Microphone muted.", isSystem = true)
            updateForegroundServiceType()
            speakResponse("Microphone muted.")
        } else {
            JarvisStateHolder.addLog("Live Mode: Microphone unmuted.", isSystem = true)
            updateForegroundServiceType()
            speakResponse("Microphone active.") {
                restartListeningSafely(delayMs = 150)
            }
        }
    }

    private fun stopScreenContextSession() {
        ScreenCaptureManager.stop()
        JarvisStateHolder.setScreenContextEnabled(false)
        JarvisStateHolder.addLog("Screen Context disabled. Resources released.", isSystem = true)
        updateForegroundServiceType()
        speakResponse("Screen sharing stopped.")
    }

    private fun isEndLiveModeCommand(command: String): Boolean {
        val lower = command.lowercase(Locale.ROOT).trim()
        return lower == "jarvis end conversation" ||
                lower == "end conversation" ||
                lower == "end call" ||
                lower == "end live mode" ||
                lower == "stop live mode" ||
                lower == "exit live mode" ||
                lower == "hang up" ||
                lower == "disconnect" ||
                lower == "stop live call"
    }

    private fun startCallDurationTimer() {
        stopCallDurationTimer()
        callDurationSeconds = 0L
        JarvisStateHolder.updateCallDuration(0L)
        callTimerRunnable = object : Runnable {
            override fun run() {
                if (isLiveMode) {
                    callDurationSeconds++
                    JarvisStateHolder.updateCallDuration(callDurationSeconds)
                    mainHandler.postDelayed(this, 1000L)
                }
            }
        }
        mainHandler.postDelayed(callTimerRunnable!!, 1000L)
    }

    private fun stopCallDurationTimer() {
        callTimerRunnable?.let { mainHandler.removeCallbacks(it) }
        callTimerRunnable = null
        callDurationSeconds = 0L
        JarvisStateHolder.updateCallDuration(0L)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "JARVIS Voice Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground voice listening and wake word detection"
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateForegroundNotification(statusText: String) {
        if (!isServiceRunning) return
        try {
            val notification = buildForegroundNotification(statusText)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // Non-fatal notification update error
        }
    }

    private fun updateForegroundServiceType() {
        val statusText = if (isLiveMode) {
            val micStatus = if (isMicMuted) "Mic Muted" else "Mic Active"
            val screenStatus = if (ScreenCaptureManager.isScreenCaptureActive.value) " • 🖥️ Screen Active" else ""
            "🔴 LIVE CALL • 🎙️ $micStatus$screenStatus"
        } else if (isConversationActive) {
            "In Conversation - Listening"
        } else {
            "Online - Waiting for \"Jarvis\""
        }

        val notification = buildForegroundNotification(statusText)
        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            var flags = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && ScreenCaptureManager.isScreenCaptureActive.value) {
                flags = flags or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            }
            flags
        } else {
            0
        }

        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, foregroundType)
        } catch (e: Exception) {
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (ex: Exception) {
                val manager = getSystemService(NotificationManager::class.java)
                manager?.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun buildForegroundNotification(statusText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (isLiveMode) {
            val stopLiveIntent = Intent(this, JarvisVoiceService::class.java).apply {
                action = ACTION_STOP_LIVE_MODE
            }
            val pendingStopLive = PendingIntent.getService(
                this,
                10,
                stopLiveIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val toggleMicIntent = Intent(this, JarvisVoiceService::class.java).apply {
                action = ACTION_TOGGLE_MIC_MUTE
            }
            val pendingToggleMic = PendingIntent.getService(
                this,
                11,
                toggleMicIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val micLabel = if (isMicMuted) "Unmute Mic" else "Mute Mic"

            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🔴 JARVIS Live Mode Active")
                .setContentText(statusText)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingOpenApp)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "End Call", pendingStopLive)
                .addAction(android.R.drawable.ic_btn_speak_now, micLabel, pendingToggleMic)
                .build()
        }

        val stopIntent = Intent(this, JarvisVoiceService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val schoolModeIntent = Intent(this, JarvisVoiceService::class.java).apply {
            action = ACTION_TOGGLE_SCHOOL_MODE
        }
        val pendingSchoolMode = PendingIntent.getService(
            this,
            2,
            schoolModeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val liveModeIntent = Intent(this, JarvisVoiceService::class.java).apply {
            action = ACTION_START_LIVE_MODE
        }
        val pendingLiveMode = PendingIntent.getService(
            this,
            3,
            liveModeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JARVIS AI Assistant")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingOpenApp)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(android.R.drawable.ic_btn_speak_now, "Live Call", pendingLiveMode)
            .addAction(android.R.drawable.ic_media_pause, "Turn OFF", pendingStop)
            .addAction(android.R.drawable.ic_lock_idle_lock, "School Mode", pendingSchoolMode)
            .build()
    }

    private fun getLocaleForLanguage(code: String): Locale {
        return try {
            Locale.forLanguageTag(code)
        } catch (e: Exception) {
            Locale.getDefault()
        }
    }

    companion object {
        const val CHANNEL_ID = "jarvis_voice_service_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.jarvis.START"
        const val ACTION_STOP = "com.example.jarvis.STOP"
        const val ACTION_TOGGLE_SCHOOL_MODE = "com.example.jarvis.TOGGLE_SCHOOL_MODE"
        const val ACTION_EXECUTE_COMMAND = "com.example.jarvis.EXECUTE_COMMAND"
        const val EXTRA_COMMAND = "extra_command"

        const val ACTION_START_LIVE_MODE = "com.example.jarvis.START_LIVE_MODE"
        const val ACTION_STOP_LIVE_MODE = "com.example.jarvis.STOP_LIVE_MODE"
        const val ACTION_TOGGLE_MIC_MUTE = "com.example.jarvis.TOGGLE_MIC_MUTE"
        const val ACTION_STOP_SCREEN_CONTEXT = "com.example.jarvis.STOP_SCREEN_CONTEXT"

        fun startService(context: Context) {
            val intent = Intent(context, JarvisVoiceService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, JarvisVoiceService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun startLiveMode(context: Context) {
            val intent = Intent(context, JarvisVoiceService::class.java).apply {
                action = ACTION_START_LIVE_MODE
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopLiveMode(context: Context) {
            val intent = Intent(context, JarvisVoiceService::class.java).apply {
                action = ACTION_STOP_LIVE_MODE
            }
            context.startService(intent)
        }

        fun toggleMicMute(context: Context) {
            val intent = Intent(context, JarvisVoiceService::class.java).apply {
                action = ACTION_TOGGLE_MIC_MUTE
            }
            context.startService(intent)
        }

        fun stopScreenContext(context: Context) {
            val intent = Intent(context, JarvisVoiceService::class.java).apply {
                action = ACTION_STOP_SCREEN_CONTEXT
            }
            context.startService(intent)
        }

        fun toggleSchoolMode(context: Context) {
            val intent = Intent(context, JarvisVoiceService::class.java).apply {
                action = ACTION_TOGGLE_SCHOOL_MODE
            }
            context.startService(intent)
        }

        fun executeCommand(context: Context, command: String) {
            val intent = Intent(context, JarvisVoiceService::class.java).apply {
                action = ACTION_EXECUTE_COMMAND
                putExtra(EXTRA_COMMAND, command)
            }
            context.startService(intent)
        }
    }
}
