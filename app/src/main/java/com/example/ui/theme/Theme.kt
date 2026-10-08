package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.domain.model.AppThemePreset

fun getThemeColorScheme(preset: AppThemePreset) = darkColorScheme(
    primary = Color(preset.primaryHex),
    secondary = Color(preset.secondaryHex),
    tertiary = ElectricBlueLight,
    background = Color(preset.surfaceHex),
    surface = Color(preset.surfaceHex),
    surfaceVariant = VmusixSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = VmusixTextPrimary,
    onSurface = VmusixTextPrimary,
    onSurfaceVariant = VmusixTextSecondary
)

val VmusixDarkColorScheme = getThemeColorScheme(AppThemePreset.MIDNIGHT_PURPLE)

@Composable
fun VmusixTheme(
    preset: AppThemePreset = AppThemePreset.MIDNIGHT_PURPLE,
    content: @Composable () -> Unit
) {
    val colorScheme = getThemeColorScheme(preset)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    VmusixTheme(preset = AppThemePreset.MIDNIGHT_PURPLE, content = content)
}
