package com.example.expenceflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/* ---------------- COLORS ---------------- */

// 🌞 Light (Gold)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFB68D40),          // gold accent
    background = Color(0xFFFFF1C1),       // 💛 GOLD background
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color(0xFF3A2E0F),     // dark brown text
    onSurface = Color.Black,
)

// 🌙 Dark
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB68D40),          // same gold accent
    background = Color(0xFF121212),       // dark bg
    surface = Color(0xFF1C1B17),
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
)

/* ---------------- THEME ---------------- */

@Composable
fun ExpenceFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    AppThemeState.isDark.value = darkTheme   // 🔥 THIS LINE
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
