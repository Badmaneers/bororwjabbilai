package com.heliactyl.bororwjabbilai.ui.paper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.heliactyl.bororwjabbilai.ui.theme.paper.PaperTheme

enum class PaperSurfaceStyle {
    RULED,
    FLAT,
}

@Composable
fun Modifier.paperSurface(
    style: PaperSurfaceStyle = PaperSurfaceStyle.RULED,
    shape: Shape = PaperTheme.shapes.sheet,
    elevation: Dp = if (style == PaperSurfaceStyle.FLAT) PaperTheme.elevations.none else PaperTheme.elevations.sheet,
    borderWidth: Dp = 1.dp,
): Modifier {
    val colors = PaperTheme.colors

    val surfaceBg = if (colors.isDark) {
        Brush.linearGradient(
            colors = listOf(
                colors.surfaceElevated,
                colors.surface,
                colors.surfaceElevated,
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFDFBF7),
                Color(0xFFF8F3E8),
                Color(0xFFFAF6EE),
            )
        )
    }

    val borderBrush = if (colors.isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.14f),
                Color.White.copy(alpha = 0.04f),
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFE8DFC9),
                Color(0xFFDCD0B4),
            )
        )
    }

    return this.then(
        Modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                clip = false
            )
            .background(
                brush = surfaceBg,
                shape = shape
            )
            .border(
                width = borderWidth,
                brush = borderBrush,
                shape = shape
            )
    )
}

@Composable
fun PaperSurfaceCanvas(
    modifier: Modifier = Modifier,
    style: PaperSurfaceStyle = PaperSurfaceStyle.RULED,
    lineSpacing: Dp = 28.dp,
    topPadding: Dp = 20.dp,
    showMargin: Boolean = true,
) {
    val colors = PaperTheme.colors

    if (style == PaperSurfaceStyle.RULED) {
        Canvas(modifier = modifier) {
            val topPaddingPx = topPadding.toPx()
            val lineSpacingPx = lineSpacing.toPx()
            val lineCount = ((size.height - topPaddingPx) / lineSpacingPx).toInt()
            val strokeWidth = 1.dp.toPx()

            // Draw notebook ruled lines
            for (i in 1..lineCount) {
                val y = topPaddingPx + (i * lineSpacingPx)
                drawLine(
                    color = colors.ruledLine,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
                )
            }

            // Margin line drawn at x = 36.dp
            if (showMargin) {
                val marginX = 36.dp.toPx()
                drawLine(
                    color = colors.accentInk.copy(alpha = 0.35f),
                    start = Offset(marginX, 0f),
                    end = Offset(marginX, size.height),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }
    }
}

@Composable
fun PaperSurfaceContainer(
    modifier: Modifier = Modifier,
    style: PaperSurfaceStyle = PaperSurfaceStyle.RULED,
    shape: Shape = PaperTheme.shapes.sheet,
    lineSpacing: Dp = 28.dp,
    topPadding: Dp = 20.dp,
    showMargin: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .paperSurface(style = style, shape = shape)
            .clip(shape)
    ) {
        PaperSurfaceCanvas(
            style = style,
            lineSpacing = lineSpacing,
            topPadding = topPadding,
            showMargin = showMargin,
            modifier = Modifier.matchParentSize()
        )
        content()
    }
}
