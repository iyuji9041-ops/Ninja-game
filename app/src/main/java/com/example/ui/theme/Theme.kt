package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ShadowCombatColorScheme = darkColorScheme(
    primary = BloodCrimson,
    onPrimary = Color.White,
    primaryContainer = BloodCrimsonDark,
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = SoulCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF004D5A),
    onSecondaryContainer = Color(0xFFB5EFFF),
    tertiary = FlameAmber,
    onTertiary = Color.Black,
    background = ShadowDarkBg,
    onBackground = TextPrimary,
    surface = ShadowSurface,
    onSurface = TextPrimary,
    surfaceVariant = ShadowSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF4A3E61)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ShadowCombatColorScheme,
        typography = Typography,
        content = content
    )
}
