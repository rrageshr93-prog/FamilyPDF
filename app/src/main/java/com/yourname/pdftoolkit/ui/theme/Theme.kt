package com.yourname.pdftoolkit.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ── Light Palette ─────────────────────────────────────────────
private val LightColors = lightColorScheme(
    primary          = Color(0xFF4A7043),  // Warm Green
    onPrimary        = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCCE8C3),
    onPrimaryContainer = Color(0xFF0D2109),

    secondary        = Color(0xFFDD8B4F),  // Soft Orange
    onSecondary      = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDCBE),
    onSecondaryContainer = Color(0xFF2F1500),

    background       = Color(0xFFFFFBF5),  // Warm Cream
    onBackground     = Color(0xFF1C1B1F),
    surface          = Color(0xFFFFFBF5),
    onSurface        = Color(0xFF1C1B1F),
    surfaceVariant   = Color(0xFFE8F5E3),

    error            = Color(0xFFBA1A1A),
    onError          = Color(0xFFFFFFFF),
)

// ── Dark Palette ──────────────────────────────────────────────
private val DarkColors = darkColorScheme(
    primary          = Color(0xFF8FB37E),
    onPrimary        = Color(0xFF1A3A14),
    primaryContainer = Color(0xFF32552C),
    onPrimaryContainer = Color(0xFFCCE8C3),

    secondary        = Color(0xFFEEB07A),
    onSecondary      = Color(0xFF4A2800),
    secondaryContainer = Color(0xFF6A3C00),
    onSecondaryContainer = Color(0xFFFFDCBE),

    background       = Color(0xFF1C1B1F),
    onBackground     = Color(0xFFE6E1E5),
    surface          = Color(0xFF1C1B1F),
    onSurface        = Color(0xFFE6E1E5),
    surfaceVariant   = Color(0xFF2A3828),

    error            = Color(0xFFFFB4AB),
    onError          = Color(0xFF690005),
)

// ── Theme Entry Point ─────────────────────────────────────────
@Composable
fun FamilyPDFTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,          // keep false = consistent family branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else      -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}