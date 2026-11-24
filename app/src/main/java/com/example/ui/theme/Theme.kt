package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val NurDarkColorScheme = darkColorScheme(
    primary = LuminousGold,
    onPrimary = ObsidianNight,
    primaryContainer = EmeraldSurfaceElevated,
    onPrimaryContainer = LuminousGold,
    secondary = AntiqueGold,
    onSecondary = ObsidianNight,
    secondaryContainer = Color(0xFF144231),
    onSecondaryContainer = WarmCream,
    tertiary = SubtleSage,
    onTertiary = ObsidianNight,
    background = DeepEmeraldBg,
    onBackground = WarmCream,
    surface = EmeraldSurface,
    onSurface = WarmCream,
    surfaceVariant = EmeraldSurfaceElevated,
    onSurfaceVariant = MutedCream,
    outline = EmeraldBorder,
    outlineVariant = Color(0xFF153D2F),
    error = ErrorCoral,
    onError = ObsidianNight
)

private val NurLightColorScheme = lightColorScheme(
    primary = MosqueGreenPrimary,
    onPrimary = CreamParchmentBg,
    primaryContainer = Color(0xFFD6EDE3),
    onPrimaryContainer = MosqueGreenDark,
    secondary = BurnishedGold,
    onSecondary = CreamParchmentBg,
    secondaryContainer = Color(0xFFF3E6C3),
    onSecondaryContainer = Color(0xFF4A3404),
    tertiary = Color(0xFF2B6B56),
    onTertiary = CreamParchmentBg,
    background = CreamParchmentBg,
    onBackground = InkCharcoal,
    surface = CreamSurface,
    onSurface = InkCharcoal,
    surfaceVariant = CreamSurfaceElevated,
    onSurfaceVariant = InkMuted,
    outline = MosqueBorder,
    outlineVariant = Color(0xFFE3DAC6),
    error = ErrorDeep,
    onError = Color.White
)

val NurShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun NurTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NurDarkColorScheme else NurLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = NurShapes,
        content = content
    )
}
