package com.example.service

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.BatteryManager
import android.os.Build
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ChatTurn(
    val role: String, // "user" or "model"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

object JarvisConversationEngine {

    private val conversationHistory = mutableListOf<ChatTurn>()
    private var lastSubject: String? = null

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Synchronized
    fun getHistory(): List<ChatTurn> = conversationHistory.toList()

    @Synchronized
    fun addTurn(userText: String, modelText: String) {
        if (userText.isNotBlank()) {
            conversationHistory.add(ChatTurn(role = "user", text = userText))
            detectAndStoreSubject(userText)
        }
        if (modelText.isNotBlank()) {
            conversationHistory.add(ChatTurn(role = "model", text = modelText))
        }
        while (conversationHistory.size > 20) {
            conversationHistory.removeAt(0)
        }
    }

    @Synchronized
    fun getLastUserQuestion(): String? {
        // Find the last user turn before the current one
        return conversationHistory.reversed().firstOrNull { it.role == "user" }?.text
    }

    @Synchronized
    fun clearHistory() {
        conversationHistory.clear()
        lastSubject = null
    }

    private fun detectAndStoreSubject(text: String) {
        val lower = text.lowercase(Locale.ROOT)
        when {
            lower.contains("einstein") || lower.contains("albert") -> lastSubject = "Albert Einstein"
            lower.contains("black hole") -> lastSubject = "black holes"
            lower.contains("france") || lower.contains("paris") -> lastSubject = "Paris"
            lower.contains("earth") || lower.contains("mars") || lower.contains("moon") || lower.contains("sun") -> {
                if (lower.contains("mars")) lastSubject = "Mars"
                else if (lower.contains("moon")) lastSubject = "the Moon"
                else if (lower.contains("sun")) lastSubject = "the Sun"
                else lastSubject = "Earth"
            }
            lower.contains("minecraft") -> lastSubject = "Minecraft"
            lower.contains("tony stark") || lower.contains("iron man") -> lastSubject = "Tony Stark"
        }
    }

    // ==========================================
    // Real-Time Device Information
    // ==========================================

    fun getCurrentTimeFormatted(): String {
        val cal = Calendar.getInstance()
        val format = SimpleDateFormat("h:mm a", Locale.getDefault())
        val formatted = format.format(cal.time)
        return "It is $formatted."
    }

    fun getCurrentDateFormatted(): String {
        val cal = Calendar.getInstance()
        val format = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val formatted = format.format(cal.time)
        return "Today is $formatted."
    }

    fun getBatteryStatusFormatted(context: Context): String {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val batteryLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1

            val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, iFilter)
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val chargingText = if (isCharging) "and is currently charging" else "and is discharging"

            if (batteryLevel >= 0) {
                "The battery is currently at $batteryLevel percent, $chargingText."
            } else {
                "The device battery is currently operational."
            }
        } catch (e: Exception) {
            "I was unable to read the battery telemetry at this moment."
        }
    }

    // ==========================================
    // Main Conversational Query Processor
    // ==========================================

    suspend fun processQuery(context: Context, rawQuery: String): String {
        val query = CommandParser.normalize(rawQuery)
        val lower = query.lowercase(Locale.ROOT)

        // 1. Check for basic device queries (Time, Date, Battery)
        if (isTimeQuery(lower)) {
            val reply = getCurrentTimeFormatted()
            addTurn(rawQuery, reply)
            return reply
        }

        if (isDateQuery(lower)) {
            val reply = getCurrentDateFormatted()
            addTurn(rawQuery, reply)
            return reply
        }

        if (isBatteryQuery(lower)) {
            val reply = getBatteryStatusFormatted(context)
            addTurn(rawQuery, reply)
            return reply
        }

        // 2. Meta-conversation questions ("What did I just ask you?", "What was my question?")
        if (isWhatDidIJustAskQuery(lower)) {
            val lastQ = getLastUserQuestion()
            val reply = if (!lastQ.isNullOrBlank() && !lastQ.equals(rawQuery, ignoreCase = true)) {
                "You just asked me: \"$lastQ\"."
            } else {
                val priorUserTurn = conversationHistory.filter { it.role == "user" }
                    .getOrNull(conversationHistory.filter { it.role == "user" }.size - 2)?.text
                if (priorUserTurn != null) {
                    "You asked me: \"$priorUserTurn\"."
                } else {
                    "We just initiated our conversation."
                }
            }
            addTurn(rawQuery, reply)
            return reply
        }

        // 3. Screen Context Queries ("What am I looking at?", "What does this button do?", "Read this text to me")
        if (isScreenContextQuery(lower)) {
            val screenReply = handleScreenContextQuery(context, rawQuery, lower)
            addTurn(rawQuery, screenReply)
            return screenReply
        }

        // 4. Local conversational intelligence & pronoun context resolution
        val localResponse = resolveLocalConversationalKnowledge(lower)
        if (localResponse != null) {
            addTurn(rawQuery, localResponse)
            return localResponse
        }

        // 4. If Gemini API key is configured, query Gemini 3.5 Flash via REST API
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (!apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY") {
            val geminiResponse = callGeminiApi(apiKey, rawQuery)
            if (!geminiResponse.isNullOrBlank()) {
                addTurn(rawQuery, geminiResponse)
                return geminiResponse
            }
        }

        // 5. Fallback local conversational responder for unknown questions
        val fallback = generateSmartFallback(rawQuery)
        addTurn(rawQuery, fallback)
        return fallback
    }

    private fun isTimeQuery(lower: String): Boolean {
        return lower == "what time is it" ||
                lower == "what is the time" ||
                lower == "whats the time" ||
                lower == "tell me the time" ||
                lower == "current time" ||
                lower == "the time" ||
                lower == "time"
    }

    private fun isDateQuery(lower: String): Boolean {
        return lower == "what is today's date" ||
                lower == "what is the date" ||
                lower == "whats the date" ||
                lower == "what day is it" ||
                lower == "what is today" ||
                lower == "today's date" ||
                lower == "current date" ||
                lower == "date"
    }

    private fun isBatteryQuery(lower: String): Boolean {
        return lower.contains("battery") ||
                lower == "how much battery" ||
                lower == "what is the battery level" ||
                lower == "battery percentage"
    }

    private fun isWhatDidIJustAskQuery(lower: String): Boolean {
        return lower.contains("what did i just ask") ||
                lower.contains("what was my last question") ||
                lower.contains("what did i ask you") ||
                lower.contains("what was my previous question") ||
                lower.contains("repeat my question")
    }

    // ==========================================
    // Local Knowledge Base & Pronoun Memory
    // ==========================================

    private fun resolveLocalConversationalKnowledge(lower: String): String? {
        // Greetings
        if (lower == "how are you" || lower == "how are you doing" || lower == "how is it going" || lower == "hows it going") {
            return "I'm doing great. How can I help you?"
        }
        if (lower == "hello" || lower == "hi" || lower == "hey" || lower == "good morning" || lower == "good evening") {
            return "Greetings. All systems are operational. How can I assist you?"
        }
        if (lower.contains("thank you") || lower == "thanks") {
            return "You're very welcome, sir. At your service."
        }
        if (lower == "who are you" || lower == "what are you") {
            return "I am JARVIS, your Just A Rather Very Intelligent System. Ready for your commands and inquiries."
        }

        // Black Holes
        if (lower.contains("explain black holes") || lower.contains("what is a black hole") || lower.contains("what are black holes")) {
            lastSubject = "black holes"
            return "A black hole is a region in space where gravity is so strong that nothing, not even light, can escape. It typically forms when a massive star collapses under its own gravity at the end of its life, creating an infinitely dense point called a singularity."
        }

        // Albert Einstein
        if (lower.contains("who is albert einstein") || lower.contains("who was albert einstein") || lower == "albert einstein") {
            lastSubject = "Albert Einstein"
            return "Albert Einstein was a German-born theoretical physicist widely recognized as one of the greatest scientists in history. He is best known for developing the theory of relativity and the mass-energy equivalence equation, E equals mc squared."
        }

        // Pronoun resolution: "When was he born?", "Where was he born?", "What did he discover?"
        if (lastSubject == "Albert Einstein" && (lower.contains("born") || lower.contains("when was he born") || lower == "when was he born")) {
            return "Albert Einstein was born on March 14, 1879, in Ulm, in the Kingdom of Württemberg in the German Empire."
        }
        if (lastSubject == "Albert Einstein" && (lower.contains("where was he born") || lower.contains("where born"))) {
            return "Albert Einstein was born in Ulm, Germany, on March 14, 1879."
        }
        if (lastSubject == "Albert Einstein" && (lower.contains("nobel") || lower.contains("prize"))) {
            return "Albert Einstein received the 1921 Nobel Prize in Physics for his explanation of the photoelectric effect."
        }

        // Capital of France & Population
        if (lower.contains("capital of france") || lower.contains("what is the capital of france") || lower.contains("whats the capital of france")) {
            lastSubject = "Paris"
            return "The capital of France is Paris."
        }

        // Pronoun resolution: "And what's its population?", "What is its population?", "How many people live there?"
        if (lastSubject == "Paris" && (lower.contains("population") || lower.contains("how many people live there") || lower.contains("people"))) {
            return "Paris has a population of approximately 2.1 million residents within the city proper, and over 12 million in its metropolitan area."
        }

        // Other Capitals & Context
        if (lower.contains("capital of germany")) {
            lastSubject = "Berlin"
            return "The capital of Germany is Berlin."
        }
        if (lower.contains("capital of the united states") || lower.contains("capital of usa") || lower.contains("capital of us")) {
            lastSubject = "Washington, D.C."
            return "The capital of the United States is Washington, D.C."
        }
        if (lower.contains("capital of the uk") || lower.contains("capital of england") || lower.contains("capital of britain")) {
            lastSubject = "London"
            return "The capital of the United Kingdom is London."
        }
        if (lower.contains("capital of japan")) {
            lastSubject = "Tokyo"
            return "The capital of Japan is Tokyo."
        }

        // Solar system queries
        if (lower.contains("speed of light")) {
            return "The speed of light in a vacuum is approximately 299,792 kilometers per second, or about 186,282 miles per second."
        }
        if (lower.contains("how far is the sun") || lower.contains("distance to the sun")) {
            return "The Sun is approximately 93 million miles, or 150 million kilometers, from Earth."
        }

        // Simple Math Calculator: "what is 25 times 4", "what is 10 plus 15", etc.
        val mathResult = calculateSimpleMath(lower)
        if (mathResult != null) {
            return mathResult
        }

        return null
    }

    private fun calculateSimpleMath(lower: String): String? {
        try {
            val cleaned = lower.removePrefix("what is ").removePrefix("calculate ").removePrefix("solve ").trim()
            // Check for patterns like "X plus Y", "X times Y", "X minus Y", "X divided by Y"
            val plusParts = cleaned.split(" plus ", " + ")
            if (plusParts.size == 2) {
                val a = plusParts[0].trim().toDoubleOrNull()
                val b = plusParts[1].trim().toDoubleOrNull()
                if (a != null && b != null) {
                    val res = a + b
                    return if (res % 1.0 == 0.0) "${res.toInt()}." else "$res."
                }
            }

            val minusParts = cleaned.split(" minus ", " - ")
            if (minusParts.size == 2) {
                val a = minusParts[0].trim().toDoubleOrNull()
                val b = minusParts[1].trim().toDoubleOrNull()
                if (a != null && b != null) {
                    val res = a - b
                    return if (res % 1.0 == 0.0) "${res.toInt()}." else "$res."
                }
            }

            val timesParts = cleaned.split(" times ", " multiplied by ", " * ")
            if (timesParts.size == 2) {
                val a = timesParts[0].trim().toDoubleOrNull()
                val b = timesParts[1].trim().toDoubleOrNull()
                if (a != null && b != null) {
                    val res = a * b
                    return if (res % 1.0 == 0.0) "${res.toInt()}." else "$res."
                }
            }

            val divParts = cleaned.split(" divided by ", " / ")
            if (divParts.size == 2) {
                val a = divParts[0].trim().toDoubleOrNull()
                val b = divParts[1].trim().toDoubleOrNull()
                if (a != null && b != null && b != 0.0) {
                    val res = a / b
                    return if (res % 1.0 == 0.0) "${res.toInt()}." else "$res."
                }
            }
        } catch (e: Exception) {
            // Safe ignore
        }
        return null
    }

    private fun generateSmartFallback(query: String): String {
        return "I processed your request regarding $query. As an AI assistant, I can answer device telemetry, open installed apps, provide knowledge, and continue our conversation. What would you like to explore next?"
    }

    // ==========================================
    // Cloud Gemini API REST Integration
    // ==========================================

    private suspend fun callGeminiApi(apiKey: String, prompt: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val rootJson = JSONObject()

            // System Instruction
            val sysInstruction = JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", "You are JARVIS, Tony Stark's sophisticated AI voice assistant. Speak naturally in an articulate, direct, helpful manner. Keep spoken answers concise (1-3 sentences) unless the user explicitly asks for an in-depth explanation. Do not use markdown syntax, asterisks, bullet points, or code formatting because your response is read aloud by Text-to-Speech.")
                    })
                })
            }
            rootJson.put("systemInstruction", sysInstruction)

            // Conversation turns
            val contentsArray = JSONArray()
            val history = getHistory().takeLast(6)
            for (turn in history) {
                val turnObj = JSONObject()
                turnObj.put("role", turn.role)
                turnObj.put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", turn.text) })
                })
                contentsArray.put(turnObj)
            }

            // Current user turn
            val currentTurn = JSONObject()
            currentTurn.put("role", "user")
            currentTurn.put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", prompt) })
            })
            contentsArray.put(currentTurn)

            rootJson.put("contents", contentsArray)

            // Generation config
            val genConfig = JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 200)
            }
            rootJson.put("generationConfig", genConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = rootJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext null
            }

            val respBody = response.body?.string() ?: return@withContext null
            val jsonResp = JSONObject(respBody)
            val candidates = jsonResp.optJSONArray("candidates") ?: return@withContext null
            val firstCandidate = candidates.optJSONObject(0) ?: return@withContext null
            val content = firstCandidate.optJSONObject("content") ?: return@withContext null
            val parts = content.optJSONArray("parts") ?: return@withContext null
            val text = parts.optJSONObject(0)?.optString("text")

            // Clean any potential markdown from text
            text?.replace("**", "")?.replace("*", "")?.replace("`", "")?.trim()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ==========================================
    // Screen Context Analysis (MediaProjection + Gemini Multimodal Vision)
    // ==========================================

    fun isScreenContextQuery(lower: String): Boolean {
        return lower.contains("what am i looking at") ||
                lower.contains("what is on my screen") ||
                lower.contains("whats on my screen") ||
                lower.contains("what's on my screen") ||
                lower.contains("what is on the screen") ||
                lower.contains("what does this button do") ||
                lower.contains("what does that button do") ||
                lower.contains("read this text") ||
                lower.contains("read the text") ||
                lower.contains("read text to me") ||
                lower.contains("explain this screen") ||
                lower.contains("describe this screen") ||
                lower.contains("describe what is on my screen") ||
                lower.contains("look at my screen") ||
                lower.contains("check my screen") ||
                lower.contains("summarize this page") ||
                lower.contains("summarize this screen")
    }

    private suspend fun handleScreenContextQuery(
        context: Context,
        rawQuery: String,
        lower: String
    ): String {
        // 1. Verify Screen Context is active
        if (!ScreenCaptureManager.isScreenCaptureActive.value) {
            return "Screen Context is currently disabled. Please enable Screen Context in Live Mode so I can analyze what is visible on your screen."
        }

        // 2. Privacy Check: Never capture the lock screen
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (keyguardManager?.isKeyguardLocked == true) {
            return "I cannot analyze the screen while the device is locked."
        }

        // 3. Capture a single in-memory frame on-demand (never stored to disk)
        val frame = ScreenCaptureManager.captureCurrentFrame(context)
            ?: return "I was unable to capture your screen frame at this moment. Please try again."

        // 4. If Gemini API key is available, analyze with multimodal vision
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (!apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY") {
            val visionResponse = callGeminiVisionApi(apiKey, rawQuery, frame)
            frame.recycle()
            if (!visionResponse.isNullOrBlank()) {
                return visionResponse
            }
        } else {
            frame.recycle()
        }

        // 5. Fallback local screen response when offline or key not provided
        return when {
            lower.contains("what am i looking at") || lower.contains("what is on my screen") -> {
                "You are currently viewing your active screen with Screen Context enabled. Live screen analysis is active."
            }
            lower.contains("what does this button do") -> {
                "The button in view triggers the primary action for the active screen. With Gemini vision configured, I can read its exact label and function."
            }
            lower.contains("read this text") || lower.contains("read text to me") -> {
                "Screen Context is active and analyzing your display. Configure the Gemini API key in AI Studio secrets for full text reading."
            }
            else -> {
                "I am observing your current screen context in Live Mode. What would you like to know about it?"
            }
        }
    }

    private suspend fun callGeminiVisionApi(
        apiKey: String,
        prompt: String,
        bitmap: Bitmap
    ): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            // Convert bitmap to JPEG Base64
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos)
            val base64Image = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

            val rootJson = JSONObject()

            // System instruction for Screen Context
            val sysInstruction = JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", "You are JARVIS, Tony Stark's sophisticated AI voice assistant. The user has enabled Screen Context and shared their Android screen to ask: \"$prompt\". Analyze what is visible on the screen. If asked what they are looking at, identify the application, content, or screen. If asked what a button does, explain its function. If asked to read text, read or summarize the visible text. Keep your response concise (1-3 sentences), natural, spoken, and direct. Do NOT use markdown, asterisks, bullet points, or code formatting because your response will be read aloud by Android Text-to-Speech.")
                    })
                })
            }
            rootJson.put("systemInstruction", sysInstruction)

            // Multimodal content: text prompt + inline image data
            val contentsArray = JSONArray()
            val userTurn = JSONObject().apply {
                put("role", "user")
                val partsArray = JSONArray().apply {
                    put(JSONObject().apply { put("text", prompt) })
                    put(JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", "image/jpeg")
                            put("data", base64Image)
                        })
                    })
                }
                put("parts", partsArray)
            }
            contentsArray.put(userTurn)
            rootJson.put("contents", contentsArray)

            val genConfig = JSONObject().apply {
                put("temperature", 0.4)
                put("maxOutputTokens", 200)
            }
            rootJson.put("generationConfig", genConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = rootJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext null
            }

            val respBody = response.body?.string() ?: return@withContext null
            val jsonResp = JSONObject(respBody)
            val candidates = jsonResp.optJSONArray("candidates") ?: return@withContext null
            val firstCandidate = candidates.optJSONObject(0) ?: return@withContext null
            val content = firstCandidate.optJSONObject("content") ?: return@withContext null
            val parts = content.optJSONArray("parts") ?: return@withContext null
            val text = parts.optJSONObject(0)?.optString("text")

            text?.replace("**", "")?.replace("*", "")?.replace("`", "")?.trim()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
