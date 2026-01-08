package com.customersupport.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Purple = Color(0xFF6366F1)
private val PurpleLight = Color(0xFF818CF8)
private val Green = Color(0xFF10B981)
private val Red = Color(0xFFEF4444)
private val Orange = Color(0xFFF59E0B)

private val DarkBackground = Color(0xFF0A0A0F)
private val DarkSurface = Color(0xFF12121A)
private val DarkCard = Color(0xFF1A1A24)

private val DarkColorScheme = darkColorScheme(
    primary = Purple,
    secondary = Green,
    tertiary = Orange,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkCard,
    error = Red,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB0B0B0),
)

private val LightColorScheme = lightColorScheme(
    primary = Purple,
    secondary = Green,
    tertiary = Orange,
    error = Red,
)

@Composable
fun CustomerSupportTheme(
    darkTheme: Boolean = true, // Force dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
