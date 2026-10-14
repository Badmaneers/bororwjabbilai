package com.heliactyl.bororwjabbilai.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.heliactyl.bororwjabbilai.ui.glass.LocalGlassEffectConfig
import com.heliactyl.bororwjabbilai.ui.glass.fallbackGlass
import com.heliactyl.bororwjabbilai.ui.glass.trueLiquidGlass

@Composable
fun Modifier.liquidGlass(
    color: Color? = null,
    borderWidth: Float? = null,
    cornerRadius: Int = 24,
    @Suppress("UNUSED_PARAMETER") blurRadius: Float = 0f,
): Modifier {
    return this.fallbackGlass(
        cornerRadiusDp = cornerRadius,
        tint = color,
        borderWidthDp = borderWidth,
    )
}

@Composable
fun Modifier.glassOverlay(
    color: Color? = null,
    borderWidth: Float? = null,
    cornerRadius: Int = 24,
    blurRadiusDp: Float? = null,
): Modifier {
    val config = LocalGlassEffectConfig.current
    val shape = RoundedCornerShape(cornerRadius.dp)
    return this
        .fallbackGlass(
            cornerRadiusDp = cornerRadius,
            tint = color,
            borderWidthDp = borderWidth,
        )
        .trueLiquidGlass(
            config = config,
            shape = shape,
            applyEdgeEffects = cornerRadius > 0,
            blurRadiusDp = blurRadiusDp ?: config.blurRadius,
        )
}

@Composable
fun Modifier.bouncyClick(
    onClick: (() -> Unit)? = null,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = Spring.StiffnessLow
        ),
        label = "bouncyScale"
    )

    return this.then(
        Modifier
            .scale(scale)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier.pointerInput(interactionSource) {
                        detectTapGestures(
                            onPress = { offset ->
                                val press = PressInteraction.Press(offset)
                                interactionSource.emit(press)
                                tryAwaitRelease()
                                interactionSource.emit(PressInteraction.Release(press))
                            }
                        )
                    }
                }
            )
    )
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 24,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .bouncyClick(onClick = onClick)
            .liquidGlass(cornerRadius = cornerRadius)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}
