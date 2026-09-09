package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.StopScreenShare
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanDark
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisNavyCard
import com.example.ui.theme.JarvisNavyCardBorder
import com.example.ui.theme.JarvisSchoolRed
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun LivePhoneModeCard(
    isLiveMode: Boolean,
    isMicMuted: Boolean,
    isScreenContextEnabled: Boolean,
    callDurationSeconds: Long,
    isSchoolMode: Boolean,
    onStartLiveMode: () -> Unit,
    onEndLiveMode: () -> Unit,
    onToggleMicMute: () -> Unit,
    onRequestScreenCapture: () -> Unit,
    onDisableScreenCapture: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_phone_mode_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLiveMode) JarvisNavyCard else JarvisNavyCard.copy(alpha = 0.85f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = if (isLiveMode) {
                Brush.horizontalGradient(
                    listOf(
                        JarvisSchoolRed.copy(alpha = pulseAlpha),
                        JarvisCyan.copy(alpha = 0.8f)
                    )
                )
            } else {
                Brush.horizontalGradient(
                    listOf(
                        JarvisNavyCardBorder,
                        JarvisCyanDark.copy(alpha = 0.4f)
                    )
                )
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // HEADER: MODE BADGE & TITLE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLiveMode) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(JarvisSchoolRed.copy(alpha = pulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LIVE PHONE CALL ACTIVE",
                            color = JarvisSchoolRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LIVE CONVERSATION MODE",
                            color = JarvisCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }

                if (isLiveMode) {
                    // Call Duration Display
                    val minutes = callDurationSeconds / 60
                    val seconds = callDurationSeconds % 60
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        color = JarvisCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isLiveMode) {
                // ==========================================
                // ACTIVE LIVE CALL CONTROLS
                // ==========================================
                Text(
                    text = "Continuous listening active. Speak freely without saying \"Jarvis\".",
                    color = JarvisTextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // DUAL SWITCHES: MICROPHONE & SCREEN CONTEXT
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Microphone Mute / Unmute Control
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isMicMuted) JarvisSchoolRed.copy(alpha = 0.2f)
                                    else JarvisCyanDark.copy(alpha = 0.4f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Microphone Status",
                                tint = if (isMicMuted) JarvisSchoolRed else JarvisCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isMicMuted) "Mic Muted" else "Mic Active",
                                color = if (isMicMuted) JarvisSchoolRed else JarvisCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isMicMuted) "Paused" else "Listening",
                                color = JarvisTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = !isMicMuted,
                        onCheckedChange = { onToggleMicMute() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = JarvisCyan,
                            uncheckedThumbColor = JarvisTextMuted,
                            uncheckedTrackColor = JarvisNavyCard
                        ),
                        modifier = Modifier.testTag("live_mic_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Screen Context Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isScreenContextEnabled) JarvisGreen.copy(alpha = 0.2f)
                                    else JarvisNavyCardBorder.copy(alpha = 0.3f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isScreenContextEnabled) Icons.Default.ScreenShare else Icons.Default.StopScreenShare,
                                contentDescription = "Screen Context Status",
                                tint = if (isScreenContextEnabled) JarvisGreen else JarvisTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Screen Context",
                                color = if (isScreenContextEnabled) JarvisGreen else JarvisTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isScreenContextEnabled) "Active • Ask about screen" else "Off • No capture",
                                color = if (isScreenContextEnabled) JarvisGreen.copy(alpha = 0.8f) else JarvisTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = isScreenContextEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) onRequestScreenCapture() else onDisableScreenCapture()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = JarvisGreen,
                            uncheckedThumbColor = JarvisTextMuted,
                            uncheckedTrackColor = JarvisNavyCard
                        ),
                        modifier = Modifier.testTag("live_screen_context_switch")
                    )
                }

                if (isScreenContextEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(JarvisGreen.copy(alpha = 0.1f))
                            .border(1.dp, JarvisGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = JarvisGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Protected: Frames captured on-demand in memory only. Never stored to disk. Never captures lock screen.",
                                color = JarvisGreen,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // END CALL BUTTON
                Button(
                    onClick = onEndLiveMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("end_live_mode_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisSchoolRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "END LIVE CALL",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tip: You can also say \"Jarvis, end conversation\" anytime.",
                    color = JarvisTextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

            } else {
                // ==========================================
                // INACTIVE STATE: START BUTTON & EXPLANATION
                // ==========================================
                Text(
                    text = "Initiate a phone-call style dialogue. JARVIS continuously listens, allowing fluid natural back-and-forth speech without needing wake words.",
                    color = JarvisTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onStartLiveMode,
                        enabled = !isSchoolMode,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("start_live_mode_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "START LIVE CALL",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Privacy assurance note
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = JarvisCyan.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Phone Mode runs via Foreground Service with active notification.",
                        color = JarvisTextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
