package com.yourname.pdftoolkit.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF4A7043),      // Warm Green
    secondary = Color(0xFFDD8B4F),    // Soft Orange
    background = Color(0xFFFFFBF5),   // Warm Cream
    surface = Color(0xFFFFFBF5),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FB37E),
    secondary = Color(0xFFEEB07A),
    background = Color(0xFF1C1B1F),
)

@Composable
fun FamilyPDFTheme(
    darkTheme: Boolean = false,  // You can make this dynamic later
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}