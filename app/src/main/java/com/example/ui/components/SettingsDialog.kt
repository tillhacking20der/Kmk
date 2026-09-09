package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanDark
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisNavyBg
import com.example.ui.theme.JarvisNavyCard
import com.example.ui.theme.JarvisNavyCardBorder
import com.example.ui.theme.JarvisNavySurface
import com.example.ui.theme.JarvisSchoolRed
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    hasMicPermission: Boolean,
    hasNotificationPermission: Boolean,
    isWakeWordRequired: Boolean,
    isVoiceResponseEnabled: Boolean,
    speechLanguage: String,
    onRequestMicPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onToggleWakeWord: (Boolean) -> Unit,
    onToggleVoiceResponse: (Boolean) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val languages = listOf(
        "en-US" to "English (United States)",
        "en-GB" to "English (United Kingdom)",
        "de-DE" to "German (Deutsch)",
        "fr-FR" to "French (Français)",
        "es-ES" to "Spanish (Español)",
        "it-IT" to "Italian (Italiano)",
        "ja-JP" to "Japanese (日本語)",
        "hi-IN" to "Hindi (हिन्दी)"
    )

    var langDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.2.dp, JarvisCyan.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .testTag("settings_dialog"),
            color = JarvisNavySurface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SYSTEM SETTINGS",
                            color = JarvisCyan,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "JARVIS Voice Assistant • Redmi Note 14",
                            color = JarvisTextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(JarvisNavyCard)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = JarvisTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Voice Assistant Preferences
                Text(
                    text = "ASSISTANT PREFERENCES",
                    color = JarvisTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Wake word toggle
                SettingsToggleItem(
                    title = "Require Wake Word (\"Jarvis\")",
                    subtitle = if (isWakeWordRequired) "Must say \"Jarvis\" before command" else "Direct listening mode without wake word",
                    icon = Icons.Default.RecordVoiceOver,
                    checked = isWakeWordRequired,
                    onCheckedChange = onToggleWakeWord
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Text to speech toggle
                SettingsToggleItem(
                    title = "Voice Responses (TTS)",
                    subtitle = if (isVoiceResponseEnabled) "JARVIS speaks responses out loud" else "Mute voice responses (silent mode)",
                    icon = Icons.Default.VolumeUp,
                    checked = isVoiceResponseEnabled,
                    onCheckedChange = onToggleVoiceResponse
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Speech Language Selector
                ExposedDropdownMenuBox(
                    expanded = langDropdownExpanded,
                    onExpandedChange = { langDropdownExpanded = it }
                ) {
                    val currentDisplay = languages.find { it.first == speechLanguage }?.second ?: speechLanguage

                    OutlinedTextField(
                        value = currentDisplay,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Speech Recognition Language") },
                        leadingIcon = {
                            Icon(Icons.Default.Language, contentDescription = null, tint = JarvisCyan)
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langDropdownExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisNavyCardBorder,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = langDropdownExpanded,
                        onDismissRequest = { langDropdownExpanded = false },
                        modifier = Modifier.background(JarvisNavyCard)
                    ) {
                        languages.forEach { (code, name) ->
                            DropdownMenuItem(
                                text = { Text(name, color = JarvisTextPrimary) },
                                onClick = {
                                    onLanguageSelected(code)
                                    langDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Permissions Section
                Text(
                    text = "ANDROID 16 PERMISSIONS & PRIVACY",
                    color = JarvisTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mic Permission Card
                PermissionDiagnosticItem(
                    title = "Microphone Permission",
                    description = "Required to detect the \"Jarvis\" wake word and recognize spoken commands.",
                    isGranted = hasMicPermission,
                    icon = Icons.Default.Mic,
                    onRequest = onRequestMicPermission
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Notification Permission Card
                PermissionDiagnosticItem(
                    title = "Foreground Notifications",
                    description = "Required on Android 13+ to keep JARVIS running in the background when the app is closed.",
                    isGranted = hasNotificationPermission,
                    icon = Icons.Default.Notifications,
                    onRequest = onRequestNotificationPermission
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Privacy Commitment Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0A1526))
                        .border(1.dp, JarvisCyan.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Zero Cloud Telemetry Guarantee",
                                color = JarvisCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Audio is never recorded, stored, or transmitted to any server. All speech processing occurs locally on your device. Microphones stop completely when JARVIS is off or in School Mode.",
                                color = JarvisTextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Done",
                        color = Color(0xFF001A24),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(JarvisNavyCard)
            .border(1.dp, JarvisNavyCardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF131F35)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = JarvisCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, color = JarvisTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, color = JarvisTextSecondary, fontSize = 11.sp)
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF001A24),
                checkedTrackColor = JarvisCyan,
                uncheckedThumbColor = JarvisTextMuted,
                uncheckedTrackColor = Color(0xFF141F33),
                uncheckedBorderColor = JarvisNavyCardBorder
            )
        )
    }
}

@Composable
private fun PermissionDiagnosticItem(
    title: String,
    description: String,
    isGranted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(JarvisNavyCard)
            .border(
                1.dp,
                if (isGranted) JarvisGreen.copy(alpha = 0.4f) else JarvisAmber.copy(alpha = 0.5f),
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGranted) JarvisGreen else JarvisAmber,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = JarvisTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    color = JarvisTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        if (!isGranted) {
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = JarvisAmber),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Grant", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
