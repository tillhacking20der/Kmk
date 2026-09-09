package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = Color(0xFF001A24),
    primaryContainer = JarvisCyanDark,
    onPrimaryContainer = JarvisCyanLight,
    secondary = JarvisBlue,
    onSecondary = Color.White,
    secondaryContainer = JarvisBlueDark,
    onSecondaryContainer = Color(0xFFD6E4FF),
    tertiary = JarvisAmber,
    onTertiary = Color.Black,
    background = JarvisNavyBg,
    onBackground = JarvisTextPrimary,
    surface = JarvisNavySurface,
    onSurface = JarvisTextPrimary,
    surfaceVariant = JarvisNavyCard,
    onSurfaceVariant = JarvisTextSecondary,
    outline = JarvisNavyCardBorder,
    error = JarvisSchoolRed,
    onError = Color.White,
    errorContainer = JarvisSchoolRedBg,
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = Typography,
        content = content
    )
}

