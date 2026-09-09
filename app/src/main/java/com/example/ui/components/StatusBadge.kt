package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.state.VoiceStatus
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanGlow
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisNavyCard
import com.example.ui.theme.JarvisNavyCardBorder
import com.example.ui.theme.JarvisSchoolRed
import com.example.ui.theme.JarvisSchoolRedBg
import com.example.ui.theme.JarvisSchoolRedBorder
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun StatusBadge(
    status: VoiceStatus,
    isSchoolMode: Boolean,
    lastPhrase: String,
    lastResponse: String,
    modifier: Modifier = Modifier
) {
    val statusColor = when {
        isSchoolMode -> JarvisSchoolRed
        status == VoiceStatus.PROCESSING -> JarvisAmber
        status == VoiceStatus.CONVERSING -> JarvisGreen
        status == VoiceStatus.LISTENING -> JarvisGreen
        status == VoiceStatus.WAITING_FOR_WAKE_WORD -> JarvisCyan
        status == VoiceStatus.SPEAKING -> JarvisCyan
        else -> JarvisTextMuted
    }

    val icon = when {
        isSchoolMode -> Icons.Default.School
        status == VoiceStatus.PROCESSING -> Icons.Default.Sync
        status == VoiceStatus.CONVERSING -> Icons.Default.RecordVoiceOver
        status == VoiceStatus.LISTENING -> Icons.Default.Mic
        status == VoiceStatus.SPEAKING -> Icons.Default.RecordVoiceOver
        status == VoiceStatus.WAITING_FOR_WAKE_WORD -> Icons.Default.Mic
        else -> Icons.Default.MicOff
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isSchoolMode) JarvisSchoolRedBg.copy(alpha = 0.6f)
                else JarvisNavyCard
            )
            .border(
                width = 1.dp,
                color = if (isSchoolMode) JarvisSchoolRedBorder else JarvisNavyCardBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(18.dp)
            .testTag("status_badge_container")
    ) {
        // Status Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.15f))
                    .border(1.dp, statusColor.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = status.label,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSchoolMode) "SCHOOL MODE ACTIVE" else status.label.uppercase(),
                        color = statusColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (isSchoolMode) "Microphones muted. Voice services stopped." else status.subtitle,
                    color = JarvisTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Telemetry Feed (Heard speech & response)
        AnimatedVisibility(
            visible = lastPhrase.isNotBlank() || lastResponse.isNotBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF070D18))
                    .border(0.8.dp, JarvisNavyCardBorder.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                if (lastPhrase.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "VOICE INPUT",
                            color = JarvisCyan.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "\"$lastPhrase\"",
                            color = JarvisTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }

                if (lastPhrase.isNotBlank() && lastResponse.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (lastResponse.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "JARVIS FEED",
                            color = JarvisGreen.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = lastResponse,
                            color = JarvisCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        }
    }
}
