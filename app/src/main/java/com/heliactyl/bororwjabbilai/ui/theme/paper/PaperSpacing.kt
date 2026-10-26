package com.heliactyl.bororwjabbilai.ui.theme.paper

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class PaperSpacing(
    val none: Dp = 0.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
)

@Immutable
data class PaperElevations(
    val none: Dp = 0.dp,
    val flat: Dp = 2.dp,
    val card: Dp = 4.dp,
    val sheet: Dp = 8.dp,
    val dialog: Dp = 16.dp,
)

val DefaultPaperSpacing = PaperSpacing()
val DefaultPaperElevations = PaperElevations()
