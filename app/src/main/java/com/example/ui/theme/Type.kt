package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val SpaceGroteskFontFamily = FontFamily(
  Font(R.font.space_grotesk, FontWeight.Normal),
  Font(R.font.space_grotesk, FontWeight.Medium),
  Font(R.font.space_grotesk, FontWeight.SemiBold),
  Font(R.font.space_grotesk, FontWeight.Bold),
)

val PlusJakartaSansFontFamily = FontFamily(
  Font(R.font.plus_jakarta_sans, FontWeight.Normal),
  Font(R.font.plus_jakarta_sans, FontWeight.Medium),
  Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
  Font(R.font.plus_jakarta_sans, FontWeight.Bold),
)

val JetBrainsMonoFontFamily = FontFamily(
  Font(R.font.jetbrains_mono, FontWeight.Normal),
  Font(R.font.jetbrains_mono, FontWeight.Medium),
)

fun buildAppTypography(fontScale: Float = 1.0f, uiScale: Float = 1.0f): Typography {
  val scale = (fontScale * uiScale).coerceIn(0.75f, 1.5f)
  return Typography(
    displaySmall =
      TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (32f * scale).sp,
        lineHeight = (38f * scale).sp,
        letterSpacing = (-0.5).sp,
      ),
    headlineMedium =
      TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (24f * scale).sp,
        lineHeight = (30f * scale).sp,
        letterSpacing = (-0.25).sp,
      ),
    headlineSmall =
      TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (20f * scale).sp,
        lineHeight = (26f * scale).sp,
      ),
    titleLarge =
      TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (18f * scale).sp,
        lineHeight = (24f * scale).sp,
      ),
    titleMedium =
      TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (16f * scale).sp,
        lineHeight = (22f * scale).sp,
      ),
    titleSmall =
      TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (14f * scale).sp,
        lineHeight = (20f * scale).sp,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (15f * scale).sp,
        lineHeight = (22f * scale).sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (14f * scale).sp,
        lineHeight = (20f * scale).sp,
      ),
    bodySmall =
      TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (12f * scale).sp,
        lineHeight = (16f * scale).sp,
      ),
    labelLarge =
      TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (14f * scale).sp,
        lineHeight = (18f * scale).sp,
      ),
    labelMedium =
      TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (12f * scale).sp,
        lineHeight = (16f * scale).sp,
      ),
    labelSmall =
      TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (11f * scale).sp,
        lineHeight = (14f * scale).sp,
      ),
  )
}
