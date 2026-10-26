package com.heliactyl.bororwjabbilai.ui.theme.paper

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

enum class PaperThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED,
}

val LocalPaperColors = staticCompositionLocalOf { PaperLightColors }
val LocalPaperTypography = staticCompositionLocalOf { DefaultPaperTypography }
val LocalPaperShapes = staticCompositionLocalOf { DefaultPaperShapes }
val LocalPaperSpacing = staticCompositionLocalOf { DefaultPaperSpacing }
val LocalPaperElevations = staticCompositionLocalOf { DefaultPaperElevations }

@Composable
fun PaperTheme(
    mode: PaperThemeMode = PaperThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (mode) {
        PaperThemeMode.SYSTEM -> systemDark
        PaperThemeMode.LIGHT -> false
        PaperThemeMode.DARK, PaperThemeMode.AMOLED -> true
    }

    val paperColors = when (mode) {
        PaperThemeMode.SYSTEM -> if (systemDark) PaperDarkColors else PaperLightColors
        PaperThemeMode.LIGHT -> PaperLightColors
        PaperThemeMode.DARK -> PaperDarkColors
        PaperThemeMode.AMOLED -> PaperAmoledColors
    }

    val materialColorScheme = if (isDark) {
        darkColorScheme(
            primary = paperColors.accentInk,
            surface = paperColors.surface,
            background = paperColors.background,
            onPrimary = paperColors.surface,
            onSurface = paperColors.textPrimary,
            onBackground = paperColors.textPrimary,
            outline = paperColors.border,
        )
    } else {
        lightColorScheme(
            primary = paperColors.accentInk,
            surface = paperColors.surface,
            background = paperColors.background,
            onPrimary = paperColors.surface,
            onSurface = paperColors.textPrimary,
            onBackground = paperColors.textPrimary,
            outline = paperColors.border,
        )
    }

    CompositionLocalProvider(
        LocalPaperColors provides paperColors,
        LocalPaperTypography provides DefaultPaperTypography,
        LocalPaperShapes provides DefaultPaperShapes,
        LocalPaperSpacing provides DefaultPaperSpacing,
        LocalPaperElevations provides DefaultPaperElevations,
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            content = content,
        )
    }
}

object PaperTheme {
    val colors: PaperColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPaperColors.current

    val typography: PaperTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalPaperTypography.current

    val shapes: PaperShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalPaperShapes.current

    val spacing: PaperSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalPaperSpacing.current

    val elevations: PaperElevations
        @Composable
        @ReadOnlyComposable
        get() = LocalPaperElevations.current
}
