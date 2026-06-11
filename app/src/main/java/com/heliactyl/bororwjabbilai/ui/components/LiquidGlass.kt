package com.heliactyl.bororwjabbilai.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp

@Composable
fun Modifier.liquidGlass(
    color: Color? = null,
    borderWidth: Float? = null,
    cornerRadius: Int = 24,
    blurRadius: Float = 0f,
): Modifier {
    val backgroundColor = MaterialTheme.colorScheme.surface
    val isDark = backgroundColor.luminance() < 0.5f
    
    // Modern iOS-style frosted glass values
    val baseColor = color ?: if (isDark) {
        Color(0xFF1C1C1E).copy(alpha = 0.70f)
    } else {
        Color.White.copy(alpha = 0.50f) // Slightly more transparent for better contrast with borders
    }
    
    val finalBorderWidth = borderWidth ?: if (isDark) 1f else 1.2f
    
    val borderBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.08f)
            )
        )
    } else {
        // High-contrast elegant border for light mode
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.90f), // Strong top specular
                Color.Black.copy(alpha = 0.12f)  // Subtle bottom definition
            )
        )
    }

    return this.then(
        Modifier
            .graphicsLayer {
                if (blurRadius > 0f && (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S)) {
                    renderEffect = android.graphics.RenderEffect.createBlurEffect(
                        blurRadius,
                        blurRadius,
                        android.graphics.Shader.TileMode.MIRROR
                    ).asComposeRenderEffect()
                }
                shape = RoundedCornerShape(cornerRadius.dp)
                clip = true
            }
            .background(
                Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(
                            baseColor.copy(alpha = baseColor.alpha * 1.1f),
                            baseColor,
                            baseColor.copy(alpha = baseColor.alpha * 0.9f)
                        )
                    } else {
                        listOf(
                            baseColor.copy(alpha = baseColor.alpha * 0.8f),
                            baseColor,
                            baseColor.copy(alpha = baseColor.alpha * 1.2f)
                        )
                    }
                )
            )
            .border(
                width = finalBorderWidth.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(cornerRadius.dp)
            )
    )
}

@Composable
fun Modifier.bouncyClick(
    onClick: () -> Unit
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
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    )
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 24,
    onClick: () -> Unit,
    content: @Composable () -> Unit
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
