package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.example.data.model.InstalledAppInfo
import com.example.service.JarvisVoiceService
import com.example.service.ScreenCaptureManager
import com.example.state.JarvisStateHolder
import com.example.state.VoiceStatus
import com.example.ui.JarvisViewModel
import com.example.ui.JarvisViewModelFactory
import com.example.ui.components.ArcReactorVisualizer
import com.example.ui.components.ConversationSection
import com.example.ui.components.CustomCommandsSection
import com.example.ui.components.InstalledAppsDialog
import com.example.ui.components.LivePhoneModeCard
import com.example.ui.components.PowerSwitchesCard
import com.example.ui.components.SettingsDialog
import com.example.ui.components.StatusBadge
import com.example.ui.components.TelemetryLogSection
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

    // State collections
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

    // Live Voice / Phone Mode & Screen Context
    val isLiveMode by viewModel.isLiveMode.collectAsStateWithLifecycle()
    val isMicMuted by viewModel.isMicMuted.collectAsStateWithLifecycle()
    val isScreenContextEnabled by viewModel.isScreenContextEnabled.collectAsStateWithLifecycle()
    val liveCallDurationSeconds by viewModel.liveCallDurationSeconds.collectAsStateWithLifecycle()

    val customCommands by viewModel.allCommands.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()

    // Screen Capture (MediaProjection) Manager & Launcher
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
            Toast.makeText(context, "Screen capture permission was declined.", Toast.LENGTH_SHORT).show()
        }
    }

    // Permissions check
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
            Toast.makeText(context, "Microphone permission is required for voice control", Toast.LENGTH_LONG).show()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    // Dialogs
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showInstalledAppsDialog by remember { mutableStateOf(false) }

    // Initial permission request on first launch if not granted
    LaunchedEffect(Unit) {
        if (!hasMicPermission) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = JarvisNavyBg,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // TOP BAR: Branding & Quick Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(JarvisCyanDark.copy(alpha = 0.5f))
                            .border(1.2.dp, JarvisCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "J",
                            color = JarvisCyan,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

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
                            color = JarvisCyan.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Installed Apps Browser Button
                    IconButton(
                        onClick = {
                            viewModel.loadInstalledApps()
                            showInstalledAppsDialog = true
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(JarvisNavyCard)
                            .border(1.dp, JarvisNavyCardBorder, CircleShape)
                            .testTag("open_apps_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = "Installed Apps",
                            tint = JarvisCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Settings Dialog Button
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(JarvisNavyCard)
                            .border(1.dp, JarvisNavyCardBorder, CircleShape)
                            .testTag("open_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = JarvisCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // PERMISSION WARNING BANNER IF MISSING
            if (!hasMicPermission) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .testTag("mic_permission_banner"),
                    colors = CardDefaults.cardColors(containerColor = JarvisAmber.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(JarvisAmber, JarvisAmber.copy(alpha = 0.5f))))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = JarvisAmber)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Microphone Required", color = JarvisAmber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("JARVIS needs microphone access to listen for voice commands.", color = JarvisTextSecondary, fontSize = 11.sp)
                            }
                        }
                        Button(
                            onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisAmber),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Allow", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // HOLOGRAPHIC ARC REACTOR VISUALIZER
            ArcReactorVisualizer(
                status = voiceStatus,
                amplitude = soundAmplitude,
                isSchoolMode = isSchoolMode,
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

            Spacer(modifier = Modifier.height(14.dp))

            // STATUS BADGE & TELEMETRY
            StatusBadge(
                status = voiceStatus,
                isSchoolMode = isSchoolMode,
                lastPhrase = lastPhrase,
                lastResponse = lastResponse
            )

            Spacer(modifier = Modifier.height(16.dp))

            // LIVE VOICE / PHONE MODE & SCREEN CONTEXT CARD
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
                onEndLiveMode = {
                    viewModel.stopLiveMode()
                },
                onToggleMicMute = {
                    viewModel.toggleMicMute()
                },
                onRequestScreenCapture = {
                    if (mediaProjectionManager != null) {
                        screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                    } else {
                        Toast.makeText(context, "Screen capture service unavailable.", Toast.LENGTH_SHORT).show()
                    }
                },
                onDisableScreenCapture = {
                    viewModel.stopScreenContext()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // DUAL MASTER SWITCHES: JARVIS & SCHOOL MODE
            PowerSwitchesCard(
                isJarvisEnabled = isJarvisEnabled,
                isSchoolMode = isSchoolMode,
                onJarvisToggled = { enabled ->
                    if (!hasMicPermission) {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        viewModel.toggleJarvis(enabled)
                    }
                },
                onSchoolModeToggled = { active ->
                    viewModel.toggleSchoolMode(active)
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // AI VOICE CONVERSATION STREAM
            ConversationSection(
                messages = conversationHistory,
                onClearConversation = { viewModel.clearConversation() }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // VOICE CONVERSATION & COMMAND SUGGESTIONS CHIPS
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
                        "\"Jarvis, explain what I'm looking at\"",
                        "\"Jarvis, summarize this screen\"",
                        "\"Jarvis, what time is it?\"",
                        "\"Jarvis, how are you?\"",
                        "\"Jarvis, explain black holes to me\"",
                        "\"Jarvis, who is Albert Einstein?\"",
                        "\"When was he born?\"",
                        "\"What's the capital of France?\"",
                        "\"And what's its population?\"",
                        "\"Jarvis, what did I just ask you?\"",
                        "\"Jarvis, what is the battery level?\"",
                        "\"Jarvis, open YouTube\"",
                        "\"Jarvis, open Google\"",
                        "\"Jarvis, open Minecraft\"",
                        "\"Jarvis, end conversation\"",
                        "\"Jarvis, activate School Mode\"",
                        "\"Jarvis, deactivate School Mode\"",
                        "\"Jarvis, go to sleep\"",
                        "\"Jarvis, stop listening\""
                    )

                    samplePhrases.forEach { phrase ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(JarvisNavyCard)
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
                                color = JarvisCyan,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // CUSTOM COMMANDS SECTION
            CustomCommandsSection(
                commands = customCommands,
                installedApps = installedApps,
                onAddCommand = { trigger, action, pkg, appName, response ->
                    viewModel.addCustomCommand(trigger, action, pkg, appName, response)
                },
                onUpdateCommand = { cmd ->
                    viewModel.updateCustomCommand(cmd)
                },
                onDeleteCommand = { cmd ->
                    viewModel.deleteCustomCommand(cmd)
                },
                onToggleCommand = { cmd ->
                    viewModel.toggleCommandEnabled(cmd)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // LIVE HUD TELEMETRY LOGS
            TelemetryLogSection(
                logs = recentLogs,
                onClearLogs = { viewModel.clearLogs() }
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Settings & Permissions Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            hasMicPermission = hasMicPermission,
            hasNotificationPermission = hasNotificationPermission,
            isWakeWordRequired = isWakeWordRequired,
            isVoiceResponseEnabled = isVoiceResponseEnabled,
            speechLanguage = speechLanguage,
            onRequestMicPermission = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            onRequestNotificationPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onToggleWakeWord = { viewModel.setWakeWordRequired(it) },
            onToggleVoiceResponse = { viewModel.setVoiceResponseEnabled(it) },
            onLanguageSelected = { viewModel.setSpeechLanguage(it) },
            onDismiss = { showSettingsDialog = false }
        )
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
