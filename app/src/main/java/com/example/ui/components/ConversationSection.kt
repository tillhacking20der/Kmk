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
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.state.ConversationMessage
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanDark
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisNavyCard
import com.example.ui.theme.JarvisNavyCardBorder
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConversationSection(
    messages: List<ConversationMessage>,
    onClearConversation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("h:mm:ss a", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(JarvisNavyCard)
            .border(1.dp, JarvisNavyCardBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
            .testTag("conversation_section_container")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(JarvisCyanDark.copy(alpha = 0.4f))
                        .border(1.dp, JarvisCyan.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Conversation Thread",
                        tint = JarvisCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "AI VOICE CONVERSATION",
                        color = JarvisCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (messages.isEmpty()) "No active session history" else "${messages.size} dialog turns in memory",
                        color = JarvisTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            if (messages.isNotEmpty()) {
                IconButton(
                    onClick = onClearConversation,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("clear_conversation_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Clear Conversation Memory",
                        tint = JarvisTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF070D18))
                    .border(0.8.dp, JarvisNavyCardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Ready for voice conversation.",
                        color = JarvisCyan.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Say \"Jarvis\" or ask any question. Natural back-and-forth multi-turn conversation is active.",
                        color = JarvisTextSecondary,
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Show up to the last 10 messages
                messages.takeLast(10).forEach { msg ->
                    ConversationBubble(message = msg, timeStr = timeFormat.format(Date(msg.timestamp)))
                }
            }
        }
    }
}

@Composable
private fun ConversationBubble(
    message: ConversationMessage,
    timeStr: String
) {
    val isUser = message.isUser

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isUser) Color(0xFF0C1929) else Color(0xFF081422)
            )
            .border(
                width = 0.8.dp,
                color = if (isUser) JarvisCyan.copy(alpha = 0.35f) else JarvisGreen.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isUser) Icons.Default.Person else Icons.Default.SmartToy,
                    contentDescription = if (isUser) "User" else "JARVIS",
                    tint = if (isUser) JarvisCyan else JarvisGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isUser) "YOU" else "JARVIS",
                    color = if (isUser) JarvisCyan else JarvisGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = timeStr,
                color = JarvisTextMuted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = message.text,
            color = JarvisTextPrimary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}
