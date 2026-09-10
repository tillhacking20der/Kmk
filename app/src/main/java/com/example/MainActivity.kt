package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CommandActionType
import com.example.service.ScreenCaptureManager
import com.example.state.JarvisStateHolder
import com.example.state.VoiceStatus
import com.example.ui.JarvisViewModel
import com.example.ui.JarvisViewModelFactory
import com.example.ui.components.AppearanceSettingsScreen
import com.example.ui.components.ArcReactorVisualizer
import com.example.ui.components.ChatScreen
import com.example.ui.components.ConversationSection
import com.example.ui.components.CustomCommandsSection
import com.example.ui.components.GlobalSettingsScreen
import com.example.ui.components.InstalledAppsDialog
import com.example.ui.components.JarvisBackgroundCanvas
import com.example.ui.components.LivePhoneModeCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.TelemetryLogSection
import com.example.ui.components.VoiceSettingsScreen
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanDark
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisNavyBg
import com.example.ui.theme.JarvisNavyCard
import com.example.ui.theme.JarvisNavyCardBorder
import com.example.ui.theme.JarvisSchoolRed
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import com.example.ui.theme.MyApplicationTheme

enum class JarvisNavTab(val label: String) {
    HUD("HUD"),
    CHAT("CHAT"),
    CUSTOM_COMMANDS("CUSTOM COMMANDS"),
    VOICE("VOICE"),
    APPEARANCE("APPEARANCE"),
    SETTINGS("SETTINGS")
}

class MainActivity : ComponentActivity() {

    private val viewModel: JarvisViewModel by viewModels {
        JarvisViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                JarvisDashboard(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun JarvisDashboard(viewModel: JarvisViewModel) {
    val context = LocalContext.current

    // State collections from ViewModel
    val voiceStatus by viewModel.voiceStatus.collectAsStateWithLifecycle()
    val soundAmplitude by viewModel.soundAmplitude.collectAsStateWithLifecycle()
    val lastPhrase by viewModel.lastRecognizedPhrase.collectAsStateWithLifecycle()
    val lastResponse by viewModel.lastResponse.collectAsStateWithLifecycle()
    val recentLogs by viewModel.recentLogs.collectAsStateWithLifecycle()
    val conversationHistory by viewModel.conversationHistory.collectAsStateWithLifecycle()

    val isJarvisEnabled by viewModel.isJarvisEnabled.collectAsStateWithLifecycle()
    val isSchoolMode by viewModel.isSchoolMode.collectAsStateWithLifecycle()
    val isWakeWordRequired by viewModel.isWakeWordRequired.collectAsStateWithLifecycle()
    val isVoiceResponseEnabled by viewModel.isVoiceResponseEnabled.collectAsStateWithLifecycle()
    val speechLanguage by viewModel.speechLanguage.collectAsStateWithLifecycle()

    // AI API Key & Chat History Persistence
    val userApiKey by viewModel.userApiKey.collectAsStateWithLifecycle()
    val saveChatHistory by viewModel.saveChatHistory.collectAsStateWithLifecycle()

    // Voice / TTS
    val ttsVoiceName by viewModel.ttsVoiceName.collectAsStateWithLifecycle()
    val ttsSpeed by viewModel.ttsSpeed.collectAsStateWithLifecycle()
    val ttsPitch by viewModel.ttsPitch.collectAsStateWithLifecycle()
    val availableVoices by viewModel.availableTtsVoices.collectAsStateWithLifecycle()

    // Appearance & Customization
    val backgroundImageUri by viewModel.backgroundImageUri.collectAsStateWithLifecycle()
    val builtInBackground by viewModel.builtInBackground.collectAsStateWithLifecycle()
    val accentColorIndex by viewModel.accentColorIndex.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val cardTransparency by viewModel.cardTransparency.collectAsStateWithLifecycle()
    val fontSizeScale by viewModel.fontSizeScale.collectAsStateWithLifecycle()
    val fontStyle by viewModel.fontStyle.collectAsStateWithLifecycle()
    val buttonStyle by viewModel.buttonStyle.collectAsStateWithLifecycle()
    val animationsEnabled by viewModel.animationsEnabled.collectAsStateWithLifecycle()

    // Live Voice / Phone Mode & Screen Context
    val isLiveMode by viewModel.isLiveMode.collectAsStateWithLifecycle()
    val isMicMuted by viewModel.isMicMuted.collectAsStateWithLifecycle()
    val isScreenContextEnabled by viewModel.isScreenContextEnabled.collectAsStateWithLifecycle()
    val liveCallDurationSeconds by viewModel.liveCallDurationSeconds.collectAsStateWithLifecycle()

    val customCommands by viewModel.allCommands.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()

    // Dynamic Accent Color
    val accentColor = when (accentColorIndex) {
        1 -> JarvisAmber
        2 -> JarvisGreen
        3 -> Color(0xFFD500F9)
        4 -> Color(0xFFFF1744)
        else -> JarvisCyan
    }

    // Active Navigation Tab
    var currentTab by remember { mutableStateOf(JarvisNavTab.HUD) }
    var showInstalledAppsDialog by remember { mutableStateOf(false) }

    // Screen Capture Manager & Launcher
    val mediaProjectionManager = remember {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
    }

    val screenCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val success = ScreenCaptureManager.start(context, result.resultCode, result.data!!)
            if (success) {
                JarvisStateHolder.setScreenContextEnabled(true)
                Toast.makeText(context, "Screen Context active. Ask JARVIS about what's on screen.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to initialize screen capture.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Screen capture permission declined.", Toast.LENGTH_SHORT).show()
        }
    }

    // Hardware Permissions Check
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            Toast.makeText(context, "Microphone permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Microphone permission required for voice assistant", Toast.LENGTH_LONG).show()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    // Initial permission request
    LaunchedEffect(Unit) {
        if (!hasMicPermission) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Dynamic Background Wallpaper / Built-in Canvas
        JarvisBackgroundCanvas(
            backgroundImageUri = backgroundImageUri,
            builtInBackground = builtInBackground,
            accentColor = accentColor,
            isDarkMode = isDarkMode
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // TOP BRANDING BAR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { currentTab = JarvisNavTab.HUD }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.25f))
                                .border(1.2.dp, accentColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "J",
                                color = accentColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "JARVIS",
                                color = JarvisTextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 3.sp
                            )
                            Text(
                                text = "Redmi Note 14 • Android 16",
                                color = accentColor.copy(alpha = 0.85f),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Installed Apps Quick Action
                        IconButton(
                            onClick = {
                                viewModel.loadInstalledApps()
                                showInstalledAppsDialog = true
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(JarvisNavyCard.copy(alpha = cardTransparency))
                                .border(1.dp, JarvisNavyCardBorder, CircleShape)
                                .testTag("open_apps_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = "Installed Apps",
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Quick HUD Home / Switch Button
                        if (currentTab != JarvisNavTab.HUD) {
                            Button(
                                onClick = { currentTab = JarvisNavTab.HUD },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.2f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("HUD", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // PERMISSION WARNING BANNER (IF MIC MISSING)
                if (!hasMicPermission) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("mic_permission_banner"),
                        colors = CardDefaults.cardColors(containerColor = JarvisAmber.copy(alpha = 0.18f)),
                        shape = RoundedCornerShape(14.dp),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(listOf(JarvisAmber, JarvisAmber.copy(alpha = 0.5f)))
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = JarvisAmber)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Microphone Required", color = JarvisAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("JARVIS needs microphone access for voice interaction.", color = JarvisTextSecondary, fontSize = 11.sp)
                                }
                            }
                            Button(
                                onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                colors = ButtonDefaults.buttonColors(containerColor = JarvisAmber),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Allow", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // ==================================================
                // 16. UI MAIN CONTROLS: [ ON / OFF ], [ SCHOOL MODE ], [ LIVE CONVERSATION ]
                // ==================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // [ ON / OFF ]
                    Button(
                        onClick = {
                            if (!hasMicPermission) {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else if (isSchoolMode) {
                                Toast.makeText(context, "Deactivate School Mode to turn on JARVIS", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.toggleJarvis(!isJarvisEnabled)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isJarvisEnabled) JarvisGreen.copy(alpha = 0.25f) else JarvisNavyCard.copy(alpha = cardTransparency)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.2.dp,
                            color = if (isJarvisEnabled) JarvisGreen else JarvisNavyCardBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("power_master_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            tint = if (isJarvisEnabled) JarvisGreen else JarvisTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isJarvisEnabled) "ON" else "OFF",
                            color = if (isJarvisEnabled) JarvisGreen else JarvisTextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // [ SCHOOL MODE ]
                    Button(
                        onClick = { viewModel.toggleSchoolMode(!isSchoolMode) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSchoolMode) JarvisSchoolRed.copy(alpha = 0.3f) else JarvisNavyCard.copy(alpha = cardTransparency)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.2.dp,
                            color = if (isSchoolMode) JarvisSchoolRed else JarvisNavyCardBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp)
                            .testTag("school_mode_master_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = if (isSchoolMode) JarvisSchoolRed else JarvisTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSchoolMode) "SCHOOL ON" else "SCHOOL OFF",
                            color = if (isSchoolMode) JarvisSchoolRed else JarvisTextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // [ LIVE CONVERSATION ]
                    Button(
                        onClick = {
                            if (!hasMicPermission) {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else if (isSchoolMode) {
                                Toast.makeText(context, "School Mode is active. Turn it off first.", Toast.LENGTH_SHORT).show()
                            } else {
                                if (isLiveMode) viewModel.stopLiveMode() else viewModel.startLiveMode()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLiveMode) accentColor.copy(alpha = 0.3f) else JarvisNavyCard.copy(alpha = cardTransparency)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.2.dp,
                            color = if (isLiveMode) accentColor else JarvisNavyCardBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(44.dp)
                            .testTag("live_conversation_master_button")
                    ) {
                        Icon(
                            imageVector = if (isLiveMode) Icons.Default.PhoneDisabled else Icons.Default.Phone,
                            contentDescription = null,
                            tint = if (isLiveMode) accentColor else JarvisTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isLiveMode) "END CALL" else "LIVE CALL",
                            color = if (isLiveMode) accentColor else JarvisTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // LARGE ANIMATED JARVIS VOICE INDICATOR
                ArcReactorVisualizer(
                    status = voiceStatus,
                    amplitude = soundAmplitude,
                    isSchoolMode = isSchoolMode,
                    animationsEnabled = animationsEnabled,
                    accentColor = accentColor,
                    onClick = {
                        if (!hasMicPermission) {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else if (isSchoolMode) {
                            Toast.makeText(context, "School Mode is ON. Deactivate School Mode to use JARVIS.", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.toggleJarvis(!isJarvisEnabled)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // VOICE STATUS BADGE
                StatusBadge(
                    status = voiceStatus,
                    isSchoolMode = isSchoolMode,
                    lastPhrase = lastPhrase,
                    lastResponse = lastResponse
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ==================================================
                // 16. UI BUTTONS: [ CHAT ], [ CUSTOM COMMANDS ], [ VOICE ], [ APPEARANCE ], [ SETTINGS ]
                // ==================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf(
                        JarvisNavTab.HUD to ("HUD" to Icons.Default.Terminal),
                        JarvisNavTab.CHAT to ("CHAT" to Icons.Default.ChatBubbleOutline),
                        JarvisNavTab.CUSTOM_COMMANDS to ("COMMANDS" to Icons.Default.Tune),
                        JarvisNavTab.VOICE to ("VOICE" to Icons.Default.RecordVoiceOver),
                        JarvisNavTab.APPEARANCE to ("APPEARANCE" to Icons.Default.FormatPaint),
                        JarvisNavTab.SETTINGS to ("SETTINGS" to Icons.Default.Settings)
                    )

                    tabs.forEach { (tab, pair) ->
                        val (label, icon) = pair
                        val isSelected = currentTab == tab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) accentColor.copy(alpha = 0.22f)
                                    else JarvisNavyCard.copy(alpha = cardTransparency)
                                )
                                .border(
                                    width = 1.2.dp,
                                    color = if (isSelected) accentColor else JarvisNavyCardBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { currentTab = tab }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("nav_tab_${tab.name.lowercase()}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) accentColor else JarvisTextMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    color = if (isSelected) accentColor else JarvisTextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // DYNAMIC ACTIVE SCREEN CONTENT
                when (currentTab) {
                    JarvisNavTab.HUD -> {
                        // LIVE CALL CARD
                        LivePhoneModeCard(
                            isLiveMode = isLiveMode,
                            isMicMuted = isMicMuted,
                            isScreenContextEnabled = isScreenContextEnabled,
                            callDurationSeconds = liveCallDurationSeconds,
                            isSchoolMode = isSchoolMode,
                            onStartLiveMode = {
                                if (!hasMicPermission) {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    viewModel.startLiveMode()
                                }
                            },
                            onEndLiveMode = { viewModel.stopLiveMode() },
                            onToggleMicMute = { viewModel.toggleMicMute() },
                            onRequestScreenCapture = {
                                if (mediaProjectionManager != null) {
                                    screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                                } else {
                                    Toast.makeText(context, "Screen capture service unavailable.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDisableScreenCapture = { viewModel.stopScreenContext() }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // CONVERSATION STREAM PREVIEW
                        ConversationSection(
                            messages = conversationHistory,
                            onClearConversation = { viewModel.clearConversation() }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // SUGGESTIONS ROW
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "TRY ASKING OR SAYING",
                                color = JarvisTextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val samplePhrases = listOf(
                                    "\"Jarvis, start live conversation\"",
                                    "\"Jarvis, what's on my screen?\"",
                                    "\"Jarvis, explain black holes\"",
                                    "\"Jarvis, who is Albert Einstein?\"",
                                    "\"When was he born?\"",
                                    "\"Jarvis, what is the battery level?\"",
                                    "\"Jarvis, open YouTube\"",
                                    "\"Jarvis, end conversation\""
                                )
                                samplePhrases.forEach { phrase ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(JarvisNavyCard.copy(alpha = cardTransparency))
                                            .border(1.dp, JarvisNavyCardBorder, RoundedCornerShape(20.dp))
                                            .clickable {
                                                val cleanCmd = phrase.removeSurrounding("\"")
                                                Toast.makeText(context, "Executing: $cleanCmd", Toast.LENGTH_SHORT).show()
                                                viewModel.executeQuickPrompt(cleanCmd)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = phrase,
                                            color = accentColor,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // TELEMETRY LOGS
                        TelemetryLogSection(
                            logs = recentLogs,
                            onClearLogs = { viewModel.clearLogs() }
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    JarvisNavTab.CHAT -> {
                        ChatScreen(
                            messages = conversationHistory,
                            saveChatHistory = saveChatHistory,
                            onToggleSaveHistory = { viewModel.setSaveChatHistory(it) },
                            onClearChat = { viewModel.clearConversation() },
                            onSendMessage = { prompt ->
                                viewModel.executeQuickPrompt(prompt)
                            },
                            accentColor = accentColor,
                            cardTransparency = cardTransparency
                        )
                    }

                    JarvisNavTab.CUSTOM_COMMANDS -> {
                        CustomCommandsSection(
                            commands = customCommands,
                            installedApps = installedApps,
                            onAddCommand = { trigger, action, pkg, appName, response ->
                                viewModel.addCustomCommand(trigger, action, pkg, appName, response)
                            },
                            onUpdateCommand = { cmd -> viewModel.updateCustomCommand(cmd) },
                            onDeleteCommand = { cmd -> viewModel.deleteCustomCommand(cmd) },
                            onToggleCommand = { cmd -> viewModel.toggleCommandEnabled(cmd) }
                        )
                    }

                    JarvisNavTab.VOICE -> {
                        VoiceSettingsScreen(
                            speechLanguage = speechLanguage,
                            ttsVoiceName = ttsVoiceName,
                            ttsSpeed = ttsSpeed,
                            ttsPitch = ttsPitch,
                            isVoiceResponseEnabled = isVoiceResponseEnabled,
                            isWakeWordRequired = isWakeWordRequired,
                            availableVoices = availableVoices,
                            onLanguageChange = { viewModel.setSpeechLanguage(it) },
                            onVoiceNameChange = { viewModel.setTtsVoiceName(it) },
                            onSpeedChange = { viewModel.setTtsSpeed(it) },
                            onPitchChange = { viewModel.setTtsPitch(it) },
                            onVoiceResponseToggle = { viewModel.setVoiceResponseEnabled(it) },
                            onWakeWordToggle = { viewModel.setWakeWordRequired(it) },
                            onTestVoice = { sampleText -> viewModel.testTtsVoice(sampleText) },
                            accentColor = accentColor,
                            cardTransparency = cardTransparency
                        )
                    }

                    JarvisNavTab.APPEARANCE -> {
                        AppearanceSettingsScreen(
                            backgroundImageUri = backgroundImageUri,
                            builtInBackground = builtInBackground,
                            accentColorIndex = accentColorIndex,
                            isDarkMode = isDarkMode,
                            cardTransparency = cardTransparency,
                            fontSizeScale = fontSizeScale,
                            fontStyle = fontStyle,
                            buttonStyle = buttonStyle,
                            animationsEnabled = animationsEnabled,
                            onSelectGalleryImage = { viewModel.setBackgroundImageUri(it) },
                            onSelectBuiltInBg = { viewModel.setBuiltInBackground(it) },
                            onSelectAccentColor = { viewModel.setAccentColorIndex(it) },
                            onToggleDarkMode = { viewModel.setDarkMode(it) },
                            onCardTransparencyChange = { viewModel.setCardTransparency(it) },
                            onFontSizeChange = { viewModel.setFontSizeScale(it) },
                            onFontStyleChange = { viewModel.setFontStyle(it) },
                            onButtonStyleChange = { viewModel.setButtonStyle(it) },
                            onToggleAnimations = { viewModel.setAnimationsEnabled(it) },
                            accentColor = accentColor
                        )
                    }

                    JarvisNavTab.SETTINGS -> {
                        GlobalSettingsScreen(
                            userApiKey = userApiKey,
                            saveChatHistory = saveChatHistory,
                            isSchoolMode = isSchoolMode,
                            hasMicPermission = hasMicPermission,
                            hasNotificationPermission = hasNotificationPermission,
                            onSaveApiKey = { viewModel.setUserApiKey(it) },
                            onToggleSaveHistory = { viewModel.setSaveChatHistory(it) },
                            onRequestMicPermission = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            onRequestNotificationPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            accentColor = accentColor,
                            cardTransparency = cardTransparency
                        )
                    }
                }
            }
        }
    }

    // Installed Apps Browser Dialog
    if (showInstalledAppsDialog) {
        InstalledAppsDialog(
            apps = installedApps,
            onLaunchApp = { pkg ->
                try {
                    val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                    if (intent != null) {
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "Could not open app", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error opening app: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            },
            onCreateCommandForApp = { app ->
                showInstalledAppsDialog = false
                viewModel.addCustomCommand(
                    trigger = "open ${app.label.lowercase()}",
                    actionType = CommandActionType.OPEN_APP,
                    targetPackage = app.packageName,
                    targetAppName = app.label,
                    response = "Opening ${app.label}."
                )
                Toast.makeText(context, "Created command for ${app.label}", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showInstalledAppsDialog = false }
        )
    }
}
