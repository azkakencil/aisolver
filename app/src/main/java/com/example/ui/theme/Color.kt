package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.model.AccentTheme

// Dark Mode Palette
val DarkBackground = Color(0xFF10121A)
val DarkSurface = Color(0xFF151824)
val DarkCard = Color(0xFF1C2030)
val DarkCardElevated = Color(0xFF24293D)
val DarkOutline = Color(0xFF2E354D)
val DarkTextPrimary = Color(0xFFFFFFFF)
val DarkTextSecondary = Color(0xFFB4BCD4)

// Refined Bright Light Mode Palette
val LightBackground = Color(0xFFF4F6FC)
val LightSurface = Color(0xFFFFFFFF)
val LightCard = Color(0xFFFFFFFF)
val LightCardElevated = Color(0xFFEEF2FB)
val LightOutline = Color(0xFFE0E5F4)
val LightTextPrimary = Color(0xFF121629)
val LightTextSecondary = Color(0xFF55607D)

// Core Accent Colors
val PrimaryViolet = Color(0xFF7C5CFC)
val SecondaryMint = Color(0xFF20C9A6)
val ErrorCoral = Color(0xFFE63E62)
val WarningAmber = Color(0xFFE59500)
val SuccessMint = Color(0xFF10B981)

data class AccentPalette(
  val primary: Color,
  val onPrimary: Color,
  val primaryContainer: Color,
  val onPrimaryContainer: Color,
  val secondary: Color,
  val onSecondary: Color,
  val secondaryContainer: Color,
  val onSecondaryContainer: Color,
  val tertiary: Color,
)

fun accentPaletteFor(theme: AccentTheme, isDark: Boolean): AccentPalette {
  return when (theme) {
    AccentTheme.VIOLET_MINT ->
      if (isDark) {
        AccentPalette(
          primary = PrimaryViolet,
          onPrimary = Color.White,
          primaryContainer = Color(0xFF2B2358),
          onPrimaryContainer = Color(0xFFE7DEFF),
          secondary = SecondaryMint,
          onSecondary = Color(0xFF06261E),
          secondaryContainer = Color(0xFF143C34),
          onSecondaryContainer = Color(0xFFADF5E4),
          tertiary = Color(0xFF56B4FF),
        )
      } else {
        AccentPalette(
          primary = Color(0xFF6C47FF),
          onPrimary = Color.White,
          primaryContainer = Color(0xFFEDE8FF),
          onPrimaryContainer = Color(0xFF251270),
          secondary = Color(0xFF0DB394),
          onSecondary = Color.White,
          secondaryContainer = Color(0xFFD6F9F1),
          onSecondaryContainer = Color(0xFF033D31),
          tertiary = Color(0xFF2F80ED),
        )
      }

    AccentTheme.OCEAN_CYAN ->
      if (isDark) {
        AccentPalette(
          primary = Color(0xFF3898FF),
          onPrimary = Color.White,
          primaryContainer = Color(0xFF183254),
          onPrimaryContainer = Color(0xFFD5E9FF),
          secondary = Color(0xFF22D3EE),
          onSecondary = Color(0xFF05262E),
          secondaryContainer = Color(0xFF123945),
          onSecondaryContainer = Color(0xFFB3F4FF),
          tertiary = Color(0xFF818CF8),
        )
      } else {
        AccentPalette(
          primary = Color(0xFF1D74DF),
          onPrimary = Color.White,
          primaryContainer = Color(0xFFDCEBFF),
          onPrimaryContainer = Color(0xFF092A57),
          secondary = Color(0xFF0891B2),
          onSecondary = Color.White,
          secondaryContainer = Color(0xFFCFF4FC),
          onSecondaryContainer = Color(0xFF04313D),
          tertiary = Color(0xFF5B64E0),
        )
      }

    AccentTheme.SUNSET_CORAL ->
      if (isDark) {
        AccentPalette(
          primary = Color(0xFF6366F1),
          onPrimary = Color.White,
          primaryContainer = Color(0xFF25285C),
          onPrimaryContainer = Color(0xFFE0E1FF),
          secondary = Color(0xFFFF6B8B),
          onSecondary = Color(0xFF360814),
          secondaryContainer = Color(0xFF471E2B),
          onSecondaryContainer = Color(0xFFFFD6DF),
          tertiary = Color(0xFFFFB547),
        )
      } else {
        AccentPalette(
          primary = Color(0xFF4F46E5),
          onPrimary = Color.White,
          primaryContainer = Color(0xFFE4E6FF),
          onPrimaryContainer = Color(0xFF181461),
          secondary = Color(0xFFE14B6E),
          onSecondary = Color.White,
          secondaryContainer = Color(0xFFFFDCE4),
          onSecondaryContainer = Color(0xFF4D0A1B),
          tertiary = Color(0xFFD9820B),
        )
      }

    AccentTheme.EMERALD_GOLD ->
      if (isDark) {
        AccentPalette(
          primary = Color(0xFF10B981),
          onPrimary = Color(0xFF032318),
          primaryContainer = Color(0xFF123B2E),
          onPrimaryContainer = Color(0xFFB5F7DF),
          secondary = Color(0xFFF59E0B),
          onSecondary = Color(0xFF2E1B02),
          secondaryContainer = Color(0xFF422E11),
          onSecondaryContainer = Color(0xFFFFE5B4),
          tertiary = Color(0xFF38BDF8),
        )
      } else {
        AccentPalette(
          primary = Color(0xFF059669),
          onPrimary = Color.White,
          primaryContainer = Color(0xFFD0F7E8),
          onPrimaryContainer = Color(0xFF023826),
          secondary = Color(0xFFD97706),
          onSecondary = Color.White,
          secondaryContainer = Color(0xFFFEF0D1),
          onSecondaryContainer = Color(0xFF452300),
          tertiary = Color(0xFF0284C7),
        )
      }
  }
}
