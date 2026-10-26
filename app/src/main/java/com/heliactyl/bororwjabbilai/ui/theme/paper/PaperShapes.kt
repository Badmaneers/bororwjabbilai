package com.heliactyl.bororwjabbilai.ui.theme.paper

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class PaperShapes(
    val extraSmall: Shape = RoundedCornerShape(8.dp),
    val small: Shape = RoundedCornerShape(12.dp),
    val medium: Shape = RoundedCornerShape(18.dp),
    val large: Shape = RoundedCornerShape(24.dp),
    val sheet: Shape = RoundedCornerShape(28.dp),
    val pill: Shape = RoundedCornerShape(50),
)

val DefaultPaperShapes = PaperShapes()
