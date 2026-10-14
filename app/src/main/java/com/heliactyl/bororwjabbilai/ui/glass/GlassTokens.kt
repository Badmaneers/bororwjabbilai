/*
 * Copyright 2026 Boro Rwjab Bilai contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Echo Music design tokens ported from DESIGN.md:
 * dynamic-first color, translucent surfaces, 24/28dp radii,
 * flat (0dp) elevations, bold headers.
 */
package com.heliactyl.bororwjabbilai.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

val DefaultThemeSeed = Color(0xFFED5564)

/** Translucent card container, Echo-style: surfaceVariant @ 30%. */
@Composable
fun glassCardContainer(): Color =
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

/** Grouped-list container (iOS Settings style): surfaceContainerHighest, 24dp, flat. */
@Composable
fun groupedListContainer(): Color =
    MaterialTheme.colorScheme.surfaceContainerHighest

object GlassShapes {
    val Card = RoundedCornerShape(24.dp)
    val CardLarge = RoundedCornerShape(28.dp)
    val Pill = RoundedCornerShape(percent = 50)
    val GroupTop = RoundedCornerShape(
        topStart = 24.dp, topEnd = 24.dp,
        bottomStart = 4.dp, bottomEnd = 4.dp,
    )
    val GroupMiddle = RoundedCornerShape(4.dp)
    val GroupBottom = RoundedCornerShape(
        topStart = 4.dp, topEnd = 4.dp,
        bottomStart = 24.dp, bottomEnd = 24.dp,
    )
}

object GlassTokens {
    const val CardAlpha = 0.3f
    const val SurfaceOpacity = 0.4f
    val LightTint = Color(0xFFFAFAFA)
    val DarkTint = Color(0xFF121212)

    @Composable
    fun adaptiveTint(): Color =
        if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) LightTint else DarkTint

    @Composable
    fun isDark(): Boolean =
        MaterialTheme.colorScheme.surface.luminance() < 0.5f

    /** Flat integration: FABs/menus on glass must use 0dp elevation. */
    const val GlassElevationDp = 0
}

/**
 * Static translucent material used when true backdrop glass is unavailable
 * (pre-S devices, dialogs/popups in separate windows, glass disabled).
 * Matches Echo's semantic roles: adaptive tint + specular top border.
 */
@Composable
fun Modifier.fallbackGlass(
    cornerRadiusDp: Int = 24,
    tint: Color? = null,
    borderWidthDp: Float? = null,
): Modifier {
    val dark = GlassTokens.isDark()
    val base = tint ?: if (dark) {
        Color(0xFF1C1C1E).copy(alpha = 0.72f)
    } else {
        Color.White.copy(alpha = 0.55f)
    }
    val bw = borderWidthDp ?: if (dark) 1f else 1.2f
    val borderBrush = if (dark) {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.08f))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 1f), Color.Black.copy(alpha = 0.25f))
        )
    }
    val shape = RoundedCornerShape(cornerRadiusDp.dp)
    return this
        .clip(shape)
        .background(
            Brush.verticalGradient(
                if (dark) {
                    listOf(
                        base.copy(alpha = (base.alpha * 1.1f).coerceAtMost(1f)),
                        base,
                        base.copy(alpha = base.alpha * 0.9f),
                    )
                } else {
                    listOf(
                        base.copy(alpha = base.alpha * 0.8f),
                        base,
                        base.copy(alpha = (base.alpha * 1.2f).coerceAtMost(1f)),
                    )
                }
            ),
            shape,
        )
        .border(bw.dp, borderBrush, shape)
}
