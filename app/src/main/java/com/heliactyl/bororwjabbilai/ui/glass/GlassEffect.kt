/*
 * Copyright 2026 Boro Rwjab Bilai contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * True Liquid Glass implementation inspired by Echo Music's DESIGN.md
 * (dynamic translucency, backdrop blur, lens refraction, vibrancy).
 * Backdrop sampling is powered by the vendored Kyant0/backdrop sources
 * (Apache-2.0) under ui.glass.backdrop. This file itself is original code.
 */
package com.heliactyl.bororwjabbilai.ui.glass

import android.os.Build
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.heliactyl.bororwjabbilai.ui.glass.backdrop.Backdrop
import com.heliactyl.bororwjabbilai.ui.glass.backdrop.drawBackdrop
import com.heliactyl.bororwjabbilai.ui.glass.backdrop.effects.blur
import com.heliactyl.bororwjabbilai.ui.glass.backdrop.effects.colorControls
import com.heliactyl.bororwjabbilai.ui.glass.backdrop.effects.lens
import com.heliactyl.bororwjabbilai.ui.glass.backdrop.highlight.Highlight
import com.heliactyl.bororwjabbilai.ui.glass.backdrop.shadow.Shadow

/**
 * Tunable parameters for the liquid-glass material.
 *
 * Mirrors the design language described in Echo Music's DESIGN.md:
 * small pills use edge refraction + highlight + shadow, large surfaces
 * use a heavy blur with no edge treatment.
 */
@Stable
data class GlassEffectConfig(
    val globalEnabled: Boolean = true,
    val vibrancy: Float = 1f,
    /** Blur in dp for pills. Player backgrounds multiply this. */
    val blurRadius: Float = 8f,
    /** 0..1 mapped to 0..[LensMaxDp]. 0.5 matches Apple's 24dp recipe. */
    val lensHeight: Float = 0.5f,
    /** 0..1 mapped to 0..[LensMaxDp]. */
    val lensAmount: Float = 0.5f,
    val chromaticAberration: Boolean = true,
    val depthEffect: Boolean = true,
    /** Unspecified = adaptive light/dark tint. */
    val surfaceTintColor: Color = Color.Unspecified,
    val surfaceOpacity: Float = 0.4f,
    val textColor: Color = Color.Unspecified,
    val playerEnabled: Boolean = true,
    val miniPlayerEnabled: Boolean = true,
    val navBarEnabled: Boolean = true,
) {
    fun isEnabledFor(component: GlassComponent): Boolean =
        globalEnabled && when (component) {
            GlassComponent.PLAYER -> playerEnabled
            GlassComponent.MINI_PLAYER -> miniPlayerEnabled
            GlassComponent.NAV_BAR -> navBarEnabled
        }
}

enum class GlassComponent { PLAYER, MINI_PLAYER, NAV_BAR }

/** Max lens refraction in dp when sliders are at 1.0. */
const val LensMaxDp = 48f

/** Full-screen backgrounds use 4x the pill blur (Apple Music style). */
const val PlayerBlurMultiplier = 4f

/** Cheapest backdrop resolution; heavy blur hides upscaling. */
const val MinGlassResolutionScale = 0.33f

/** Blur dp at which the minimum scale is safe. */
const val FullQualityBlurDp = 8f

fun glassResolutionScale(blurRadiusDp: Float): Float {
    val t = (blurRadiusDp / FullQualityBlurDp).coerceIn(0f, 1f)
    return 1f - t * (1f - MinGlassResolutionScale)
}

fun isGlassSupported(sdkInt: Int = Build.VERSION.SDK_INT): Boolean =
    sdkInt >= Build.VERSION_CODES.S

/** Maps 0..2 vibrancy preference to a saturation multiplier. */
fun glassSaturation(vibrancy: Float): Float =
    1f + 0.5f * vibrancy.coerceIn(0f, 2f)

val LocalGlassEffectConfig = staticCompositionLocalOf { GlassEffectConfig() }

/** Backdrop (recorded app content) that glass surfaces sample. Null = no provider yet. */
val LocalAppBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * True on HyperOS/MIUI devices the system's MiBackgroundBlurBlend pass
 * recurses over nested offscreen layers until RenderThread stack overflow
 * (SIGSEGV, 512 prepareTree frames). Edge refraction/highlight/shadow add
 * extra layers, so they are skipped there — blur + tint only.
 */
fun isMiuiEdgeEffectRisk(): Boolean = try {
    val cls = Class.forName("android.os.SystemProperties")
    val get = cls.getMethod("get", String::class.java, String::class.java)
    val miui = (get.invoke(null, "ro.miui.ui.version.name", "") as? String).orEmpty()
    val hyper = (get.invoke(null, "ro.mi.os.version.name", "") as? String).orEmpty()
    val man = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
        Build.MANUFACTURER.equals("POCO", ignoreCase = true) ||
        Build.MANUFACTURER.equals("Redmi", ignoreCase = true)
    miui.isNotEmpty() || hyper.isNotEmpty() || man
} catch (_: Exception) {
    false
}

/**
 * True liquid-glass surface sampling [LocalAppBackdrop].
 *
 * MUST only be used for fixed overlays OUTSIDE `layerBackdrop()` content
 * (bottom bar, search bar). Using it for scrolling items inside the
 * recorded pager makes surfaces sample themselves → RenderThread stack
 * overflow (see MiBackgroundBlurBlend backtrace).
 *
 * - [applyEdgeEffects] = true for pills (lens + highlight + shadow),
 *   false for full-screen surfaces (avoids stray rim light).
 * - [blurRadiusDp] overrides config; pass
 *   `config.blurRadius * PlayerBlurMultiplier` for player backgrounds.
 * - [shape] must be a [CornerBasedShape]; lens throws otherwise.
 *
 * On pre-S devices the modifier returns unchanged — callers should
 * use [fallbackGlass] styling underneath so there is always a
 * translucent material.
 */
@Composable
fun Modifier.trueLiquidGlass(
    config: GlassEffectConfig = LocalGlassEffectConfig.current,
    shape: CornerBasedShape = RoundedCornerShape(24.dp),
    applyEdgeEffects: Boolean = true,
    blurRadiusDp: Float = config.blurRadius,
): Modifier {
    if (!isGlassSupported()) return this
    // If glass is globally disabled, render nothing so the fallback
    // translucent tint underneath shows through.
    if (!config.globalEnabled) return this

    val backdrop = LocalAppBackdrop.current ?: return this
    // HyperOS/MIUI: skip lens + highlight + shadow layers (stack overflow).
    val safeEdgeEffects = applyEdgeEffects && !isMiuiEdgeEffectRisk()
    val density = LocalDensity.current
    val scale = glassResolutionScale(blurRadiusDp)
    val blurPx = with(density) { blurRadiusDp.dp.toPx() } * scale
    val saturation = glassSaturation(config.vibrancy)
    val lensHpx = with(density) { (config.lensHeight * LensMaxDp).dp.toPx() } * scale
    val lensApx = with(density) { (config.lensAmount * LensMaxDp).dp.toPx() } * scale

    val tint = when {
        config.surfaceTintColor.isSpecified -> config.surfaceTintColor
        MaterialTheme.colorScheme.surface.luminance() > 0.5f -> Color(0xFFFAFAFA)
        else -> Color(0xFF121212)
    }

    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            if (saturation != 1f) colorControls(saturation = saturation)
            if (blurPx > 0f) blur(blurPx)
            if (safeEdgeEffects &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                (lensHpx > 0f || lensApx > 0f)
            ) {
                lens(
                    refractionHeight = lensHpx,
                    refractionAmount = lensApx,
                    depthEffect = config.depthEffect,
                    chromaticAberration = config.chromaticAberration,
                )
            }
        },
        highlight = if (safeEdgeEffects) ({ Highlight.Default }) else null,
        shadow = if (safeEdgeEffects) ({ Shadow.Default }) else null,
        onDrawSurface = {
            if (config.surfaceOpacity > 0f) {
                drawRect(
                    color = tint.copy(alpha = config.surfaceOpacity),
                    size = size,
                )
            }
        },
        backdropScale = scale,
    )
}

