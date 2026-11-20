package com.nurvakti.global.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Brand {
    val Emerald = Color(0xFF0B6E4F)
    val EmeraldDark = Color(0xFF07513A)
    val EmeraldLight = Color(0xFF3FAF86)
    val Gold = Color(0xFFC9A227)
    val GoldLight = Color(0xFFF0D27A)
    val Navy = Color(0xFF0B1F3A)
    val NavyDeep = Color(0xFF07152A)
    val Ivory = Color(0xFFFBF8F1)
}

private val Light = lightColorScheme(
    primary = Brand.Emerald, onPrimary = Color.White, primaryContainer = Color(0xFFCDEFE2), onPrimaryContainer = Brand.EmeraldDark,
    secondary = Brand.Gold, onSecondary = Brand.Navy, secondaryContainer = Color(0xFFF7E8B8), onSecondaryContainer = Color(0xFF3D2F00),
    tertiary = Brand.Navy, onTertiary = Color.White,
    background = Brand.Ivory, onBackground = Brand.Navy, surface = Color.White, onSurface = Brand.Navy,
    surfaceVariant = Color(0xFFEFF3EF), onSurfaceVariant = Color(0xFF41504A), outline = Color(0xFFBFC9C3),
)
private val Dark = darkColorScheme(
    primary = Brand.EmeraldLight, onPrimary = Brand.NavyDeep, primaryContainer = Brand.EmeraldDark, onPrimaryContainer = Color(0xFFCDEFE2),
    secondary = Brand.GoldLight, onSecondary = Brand.NavyDeep, secondaryContainer = Color(0xFF5A4700), onSecondaryContainer = Brand.GoldLight,
    tertiary = Color(0xFF9FB6D8), background = Brand.NavyDeep, onBackground = Color(0xFFE8EEF5), surface = Brand.Navy, onSurface = Color(0xFFE8EEF5),
    surfaceVariant = Color(0xFF162B4A), onSurfaceVariant = Color(0xFFB8C6D8), outline = Color(0xFF3A5276),
)

private val Type = Typography(
    displayLarge = TextStyle(fontSize = 54.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
)

@Composable fun NurTheme(mode: String, content: @Composable () -> Unit) {
    val dark = when (mode) { "DARK" -> true; "LIGHT" -> false; else -> isSystemInDarkTheme() }
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = Type, content = content)
}
