package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanDark
import com.example.ui.theme.JarvisCyanLight
import com.example.ui.theme.JarvisNavyCard
import com.example.ui.theme.JarvisNavyCardBorder
import com.example.ui.theme.JarvisSchoolRed
import com.example.ui.theme.JarvisSchoolRedBg
import com.example.ui.theme.JarvisSchoolRedBorder
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun PowerSwitchesCard(
    isJarvisEnabled: Boolean,
    isSchoolMode: Boolean,
    onJarvisToggled: (Boolean) -> Unit,
    onSchoolModeToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // JARVIS MASTER SWITCH CARD
        val jarvisCardBorder by animateColorAsState(
            targetValue = if (isJarvisEnabled) JarvisCyan else JarvisNavyCardBorder,
            label = "jarvis_border"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (isJarvisEnabled) Color(0xFF0C192E) else JarvisNavyCard
                )
                .border(
                    width = if (isJarvisEnabled) 1.5.dp else 1.dp,
                    color = jarvisCardBorder,
                    shape = RoundedCornerShape(18.dp)
                )
                .clickable {
                    if (!isSchoolMode) {
                        onJarvisToggled(!isJarvisEnabled)
                    }
                }
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .testTag("jarvis_power_card"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (isJarvisEnabled) JarvisCyanDark.copy(alpha = 0.5f)
                            else Color(0xFF131D31)
                        )
                        .border(
                            1.dp,
                            if (isJarvisEnabled) JarvisCyan else JarvisNavyCardBorder,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "JARVIS Power",
                        tint = if (isJarvisEnabled) JarvisCyan else JarvisTextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "JARVIS",
                            color = JarvisTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isJarvisEnabled) JarvisCyan.copy(alpha = 0.2f)
                                    else Color(0xFF1C273B)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isJarvisEnabled) "ACTIVE" else "STANDBY",
                                color = if (isJarvisEnabled) JarvisCyan else JarvisTextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (isSchoolMode) "Disabled by School Mode"
                        else if (isJarvisEnabled) "Background voice assistant running"
                        else "Tap to activate voice listening",
                        color = if (isSchoolMode) JarvisSchoolRed else JarvisTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Switch(
                checked = isJarvisEnabled,
                onCheckedChange = { onJarvisToggled(it) },
                enabled = !isSchoolMode,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF001A24),
                    checkedTrackColor = JarvisCyan,
                    uncheckedThumbColor = JarvisTextSecondary,
                    uncheckedTrackColor = Color(0xFF141F33),
                    uncheckedBorderColor = JarvisNavyCardBorder
                ),
                modifier = Modifier.testTag("jarvis_power_switch")
            )
        }

        // SCHOOL MODE SWITCH CARD
        val schoolCardBorder by animateColorAsState(
            targetValue = if (isSchoolMode) JarvisSchoolRed else JarvisNavyCardBorder,
            label = "school_border"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (isSchoolMode) JarvisSchoolRedBg else JarvisNavyCard
                )
                .border(
                    width = if (isSchoolMode) 1.5.dp else 1.dp,
                    color = schoolCardBorder,
                    shape = RoundedCornerShape(18.dp)
                )
                .clickable { onSchoolModeToggled(!isSchoolMode) }
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .testTag("school_mode_card"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSchoolMode) JarvisSchoolRed.copy(alpha = 0.2f)
                            else Color(0xFF131D31)
                        )
                        .border(
                            1.dp,
                            if (isSchoolMode) JarvisSchoolRed else JarvisNavyCardBorder,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "School Mode",
                        tint = if (isSchoolMode) JarvisSchoolRed else JarvisTextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "School Mode",
                            color = if (isSchoolMode) JarvisSchoolRed else JarvisTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isSchoolMode) JarvisSchoolRed.copy(alpha = 0.2f)
                                    else Color(0xFF1C273B)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isSchoolMode) "MUTED" else "OFF",
                                color = if (isSchoolMode) JarvisSchoolRed else JarvisTextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (isSchoolMode) "All voice listening & microphone stopped"
                        else "Silence assistant during classes & lectures",
                        color = if (isSchoolMode) JarvisSchoolRed.copy(alpha = 0.9f) else JarvisTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Switch(
                checked = isSchoolMode,
                onCheckedChange = { onSchoolModeToggled(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = JarvisSchoolRed,
                    uncheckedThumbColor = JarvisTextSecondary,
                    uncheckedTrackColor = Color(0xFF141F33),
                    uncheckedBorderColor = JarvisNavyCardBorder
                ),
                modifier = Modifier.testTag("school_mode_switch")
            )
        }
    }
}
