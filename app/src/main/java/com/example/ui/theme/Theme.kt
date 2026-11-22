package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.AppSettings
import com.example.model.ButtonSizeOption
import com.example.model.ThemeMode

@Immutable
data class UiDimensions(
  val buttonHeight: Dp = 48.dp,
  val buttonHorizontalPadding: Dp = 16.dp,
  val cardCornerRadius: Dp = 22.dp,
  val spacingSmall: Dp = 8.dp,
  val spacingMedium: Dp = 16.dp,
  val spacingLarge: Dp = 24.dp,
  val uiScale: Float = 1.0f,
  val isDark: Boolean = false,
  val cardSurface: Color = LightCard,
  val elevatedSurface: Color = LightCardElevated,
  val subtleBorder: Color = LightOutline,
)

val LocalUiDimensions = staticCompositionLocalOf { UiDimensions() }

val AppShapes =
  Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun AiSolverTheme(
  settings: AppSettings = AppSettings(),
  content: @Composable () -> Unit,
) {
  val isDark =
    when (settings.themeMode) {
      ThemeMode.DARK -> true
      ThemeMode.LIGHT -> false
      ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

  val accent = accentPaletteFor(settings.accentTheme, isDark)
  val scale = settings.uiScale.coerceIn(0.85f, 1.20f)
  val btnOpt: ButtonSizeOption = settings.buttonSize

  val colorScheme =
    if (isDark) {
      darkColorScheme(
        primary = accent.primary,
        onPrimary = accent.onPrimary,
        primaryContainer = accent.primaryContainer,
        onPrimaryContainer = accent.onPrimaryContainer,
        secondary = accent.secondary,
        onSecondary = accent.onSecondary,
        secondaryContainer = accent.secondaryContainer,
        onSecondaryContainer = accent.onSecondaryContainer,
        tertiary = accent.tertiary,
        background = DarkBackground,
        onBackground = DarkTextPrimary,
        surface = DarkSurface,
        onSurface = DarkTextPrimary,
        surfaceVariant = DarkCard,
        onSurfaceVariant = DarkTextSecondary,
        surfaceContainer = DarkCard,
        surfaceContainerHigh = DarkCardElevated,
        surfaceContainerHighest = Color(0xFF2B3148),
        outline = DarkOutline,
        outlineVariant = Color(0xFF23283B),
        error = ErrorCoral,
        onError = Color.White,
      )
    } else {
      lightColorScheme(
        primary = accent.primary,
        onPrimary = accent.onPrimary,
        primaryContainer = accent.primaryContainer,
        onPrimaryContainer = accent.onPrimaryContainer,
        secondary = accent.secondary,
        onSecondary = accent.onSecondary,
        secondaryContainer = accent.secondaryContainer,
        onSecondaryContainer = accent.onSecondaryContainer,
        tertiary = accent.tertiary,
        background = LightBackground,
        onBackground = LightTextPrimary,
        surface = LightSurface,
        onSurface = LightTextPrimary,
        surfaceVariant = LightCardElevated,
        onSurfaceVariant = LightTextSecondary,
        surfaceContainer = LightCard,
        surfaceContainerHigh = LightCardElevated,
        surfaceContainerHighest = Color(0xFFE5EAF8),
        outline = LightOutline,
        outlineVariant = Color(0xFFEAEEF9),
        error = ErrorCoral,
        onError = Color.White,
      )
    }

  val dimensions =
    UiDimensions(
      buttonHeight = (48f * btnOpt.heightScale * scale).coerceAtLeast(48f).dp,
      buttonHorizontalPadding = (btnOpt.horizontalPaddingDp * scale).dp,
      cardCornerRadius = (22f * scale).dp,
      spacingSmall = (8f * scale).dp,
      spacingMedium = (16f * scale).dp,
      spacingLarge = (24f * scale).dp,
      uiScale = scale,
      isDark = isDark,
      cardSurface = if (isDark) DarkCard else LightCard,
      elevatedSurface = if (isDark) DarkCardElevated else LightCardElevated,
      subtleBorder = if (isDark) DarkOutline else LightOutline,
    )

  val typography = buildAppTypography(fontScale = settings.fontScale, uiScale = scale)

  CompositionLocalProvider(LocalUiDimensions provides dimensions) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = typography,
      shapes = AppShapes,
      content = content,
    )
  }
}
