package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.ui.theme.JarvisNavyBg
import java.io.File

@Composable
fun JarvisBackgroundCanvas(
    backgroundImageUri: String?,
    builtInBackground: String,
    accentColor: Color,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val baseBgColor = if (isDarkMode) JarvisNavyBg else Color(0xFF0F1B2B)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBgColor)
    ) {
        // 1. If user has chosen a custom gallery background
        if (backgroundImageUri != null) {
            val file = File(backgroundImageUri)
            if (file.exists()) {
                AsyncImage(
                    model = file,
                    contentDescription = "Custom Wallpaper",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    alpha = 0.45f
                )
            }
        } else {
            // 2. Built-in backgrounds
            when (builtInBackground) {
                "arc_core" -> {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height * 0.35f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.18f),
                                    accentColor.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.width * 0.85f
                            ),
                            radius = size.width * 0.85f,
                            center = center
                        )
                        // Tech circle guides
                        drawCircle(
                            color = accentColor.copy(alpha = 0.08f),
                            radius = size.width * 0.45f,
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f)
                        )
                        drawCircle(
                            color = accentColor.copy(alpha = 0.05f),
                            radius = size.width * 0.65f,
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                        )
                    }
                }
                "cyber_grid" -> {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val step = 48f
                        var x = 0f
                        while (x < size.width) {
                            drawLine(
                                color = accentColor.copy(alpha = 0.04f),
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 1f
                            )
                            x += step
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(
                                color = accentColor.copy(alpha = 0.04f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f
                            )
                            y += step
                        }
                    }
                }
                "deep_space" -> {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF030712),
                                    Color(0xFF071329),
                                    accentColor.copy(alpha = 0.08f),
                                    Color(0xFF040A17)
                                )
                            )
                        )
                    }
                }
                "holographic_matrix" -> {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF00E676).copy(alpha = 0.10f),
                                    accentColor.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.7f, size.height * 0.2f),
                                radius = size.width * 0.9f
                            )
                        )
                    }
                }
                else -> {
                    // Solid space background
                }
            }
        }
    }
}
