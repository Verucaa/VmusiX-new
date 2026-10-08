package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Vmusix Brand Colors
val MidnightPurple = Color(0xFF6D28D9)
val MidnightPurpleLight = Color(0xFF8B5CF6)
val ElectricBlue = Color(0xFF3B82F6)
val ElectricBlueLight = Color(0xFF60A5FA)

// Neutral Dark & Glass Palette
val VmusixBackground = Color(0xFF090812)
val VmusixSurface = Color(0xFF121021)
val VmusixSurfaceVariant = Color(0xFF1B1832)
val VmusixCardBorder = Color(0x338B5CF6)
val VmusixSurfaceElevated = Color(0xFF242042)
val VmusixTextPrimary = Color(0xFFF8FAFC)
val VmusixTextSecondary = Color(0xFF94A3B8)
val VmusixTextMuted = Color(0xFF64748B)

// Glass & Gradient Brushes
val VmusixBrandGradient = Brush.horizontalGradient(
    colors = listOf(MidnightPurple, ElectricBlue)
)

val VmusixCardGlassBrush = Brush.linearGradient(
    colors = listOf(Color(0x336D28D9), Color(0x1A3B82F6))
)

val VmusixAccentGlow = Brush.radialGradient(
    colors = listOf(Color(0x406D28D9), Color.Transparent)
)
