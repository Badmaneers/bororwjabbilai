package com.heliactyl.bororwjabbilai.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.heliactyl.bororwjabbilai.R

val PencilPeteFontFamily = FontFamily(
    Font(R.font.pencil_pete, FontWeight.Normal),
    Font(R.font.pencil_pete, FontWeight.Medium),
    Font(R.font.pencil_pete, FontWeight.SemiBold),
    Font(R.font.pencil_pete, FontWeight.Bold)
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
