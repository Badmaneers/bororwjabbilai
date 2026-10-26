package com.heliactyl.bororwjabbilai.ui.theme.paper

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class PaperColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accentInk: Color,
    val border: Color,
    val divider: Color,
    val ruledLine: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val isDark: Boolean,
)

val PaperLightColors = PaperColors(
    background = Color(0xFFF6F1E7),        // Warm Ivory / Cream
    surface = Color(0xFFFBF8F1),           // Clean Parchment
    surfaceElevated = Color(0xFFFFFFFF),   // Crisp White Sheet
    textPrimary = Color(0xFF221F1B),       // Dark Charcoal Ink
    textSecondary = Color(0xFF5C554B),     // Muted Graphite
    textTertiary = Color(0xFF8A8072),      // Faint Pencil
    accentInk = Color(0xFF8B263E),         // Deep Burgundy Ink
    border = Color(0xFFE4DAD0),            // Deckled Paper Edge
    divider = Color(0xFFEAE2D8),           // Faint Paper Seam
    ruledLine = Color(0xFFD3E0EA),         // Faint Blue Notebook Line
    success = Color(0xFF386641),           // Muted Sage
    warning = Color(0xFFBC6C25),           // Warm Terracotta
    error = Color(0xFFB00020),             // Deep Crimson
    isDark = false,
)

val PaperDarkColors = PaperColors(
    background = Color(0xFF141312),        // Deep Charcoal Ground
    surface = Color(0xFF1E1C1A),           // Darkened Parchment
    surfaceElevated = Color(0xFF282522),   // Elevated Dark Sheet
    textPrimary = Color(0xFFF4EFEA),       // Aged White Text
    textSecondary = Color(0xFFB5ACA0),     // Soft Muted Text
    textTertiary = Color(0xFF80776C),      // Dim Graphite
    accentInk = Color(0xFFE07A5F),         // Muted Terracotta / Amber
    border = Color(0xFF36322E),            // Dark Edge
    divider = Color(0xFF2D2A26),           // Seam Line
    ruledLine = Color(0xFF2B333B),         // Subtle Dark Blue Line
    success = Color(0xFF6B9080),           // Soft Muted Sage
    warning = Color(0xFFE9C46A),           // Soft Gold
    error = Color(0xFFE63946),             // Rose Red
    isDark = true,
)

val PaperAmoledColors = PaperColors(
    background = Color(0xFF000000),        // Pitch Black
    surface = Color(0xFF0A0A0A),           // Pure Black Surface
    surfaceElevated = Color(0xFF141414),   // Elevated Black Sheet
    textPrimary = Color(0xFFFFFFFF),       // High Contrast White
    textSecondary = Color(0xFFB0B0B0),     // Silver Secondary
    textTertiary = Color(0xFF707070),      // Faint Silver
    accentInk = Color(0xFFE07A5F),         // Terracotta Accent
    border = Color(0xFF222222),            // Dark Slate Border
    divider = Color(0xFF1A1A1A),           // Dark Divider
    ruledLine = Color(0xFF1E2630),         // Faint Midnight Blue Line
    success = Color(0xFF6B9080),           // Sage
    warning = Color(0xFFE9C46A),           // Gold
    error = Color(0xFFE63946),             // Red
    isDark = true,
)
