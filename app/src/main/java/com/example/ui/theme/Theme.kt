package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val LavenderColorScheme =
  darkColorScheme(
    primary = SophisticatedPrimary,
    onPrimary = SophisticatedOnPrimary,
    primaryContainer = SophisticatedPrimaryContainer,
    onPrimaryContainer = SophisticatedOnPrimaryContainer,
    secondary = SophisticatedSecondary,
    onSecondary = SophisticatedOnSecondary,
    secondaryContainer = SophisticatedSecondaryContainer,
    onSecondaryContainer = SophisticatedOnSecondaryContainer,
    tertiary = SophisticatedTertiary,
    onTertiary = SophisticatedOnTertiary,
    tertiaryContainer = SophisticatedTertiaryContainer,
    onTertiaryContainer = SophisticatedOnTertiaryContainer,
    background = SophisticatedBackground,
    onBackground = SophisticatedOnBackground,
    surface = SophisticatedSurface,
    onSurface = SophisticatedOnSurface,
    surfaceVariant = SophisticatedSurfaceVariant,
    onSurfaceVariant = SophisticatedOnSurfaceVariant,
    outline = SophisticatedOutline,
    outlineVariant = SophisticatedOutlineVariant
  )

private val EmeraldColorScheme =
  darkColorScheme(
    primary = Color(0xFF81C784), // mint/emerald
    onPrimary = Color(0xFF0F3812),
    primaryContainer = Color(0xFF1E2F20),
    onPrimaryContainer = Color(0xFFD0F8D2),
    secondary = Color(0xFF81C784),
    onSecondary = Color(0xFF0F3812),
    secondaryContainer = Color(0xFF223525),
    onSecondaryContainer = Color(0xFFC5E1A5),
    tertiary = Color(0xFF80CBC4),
    onTertiary = Color(0xFF00332C),
    tertiaryContainer = Color(0xFF1F3532),
    background = Color(0xFF101612), // deep forest charcoal background
    onBackground = Color(0xFFE3EDE5),
    surface = Color(0xFF101612),
    onSurface = Color(0xFFE3EDE5),
    surfaceVariant = Color(0xFF1F2F23),
    onSurfaceVariant = Color(0xFFCBD6CD),
    outline = Color(0xFF38463B),
    outlineVariant = Color(0xFF2E3B30)
  )

private val OceanColorScheme =
  darkColorScheme(
    primary = Color(0xFF80DEEA), // cosmic cyan
    onPrimary = Color(0xFF00363A),
    primaryContainer = Color(0xFF203B42),
    onPrimaryContainer = Color(0xFFE0F7FA),
    secondary = Color(0xFF80DEEA),
    onSecondary = Color(0xFF00363A),
    secondaryContainer = Color(0xFF1E2E33),
    onSecondaryContainer = Color(0xFFC2E8EC),
    tertiary = Color(0xFF90CAF9),
    onTertiary = Color(0xFF0D47A1),
    tertiaryContainer = Color(0xFF1F3246),
    background = Color(0xFF0F141C), // cosmic deep space navy
    onBackground = Color(0xFFE2E7EC),
    surface = Color(0xFF0F141C),
    onSurface = Color(0xFFE2E7EC),
    surfaceVariant = Color(0xFF1F2D3B),
    onSurfaceVariant = Color(0xFFC6CED4),
    outline = Color(0xFF38434D),
    outlineVariant = Color(0xFF2D353F)
  )

private val RoseColorScheme =
  darkColorScheme(
    primary = Color(0xFFEF9A9A), // sunset rose
    onPrimary = Color(0xFF491818),
    primaryContainer = Color(0xFF3A2121),
    onPrimaryContainer = Color(0xFFFFEBEE),
    secondary = Color(0xFFEF9A9A),
    onSecondary = Color(0xFF491818),
    secondaryContainer = Color(0xFF332020),
    onSecondaryContainer = Color(0xFFFFCDD2),
    tertiary = Color(0xFFF48FB1),
    onTertiary = Color(0xFF4A1525),
    tertiaryContainer = Color(0xFF3F212C),
    background = Color(0xFF1C1313), // deep rose charcoal background
    onBackground = Color(0xFFECE1E1),
    surface = Color(0xFF1C1313),
    onSurface = Color(0xFFECE1E1),
    surfaceVariant = Color(0xFF302424),
    onSurfaceVariant = Color(0xFFD6CBCB),
    outline = Color(0xFF473A3A),
    outlineVariant = Color(0xFF3B2F2F)
  )

@Composable
fun MyApplicationTheme(
  themeName: String = "LAVENDER",
  darkTheme: Boolean = true, // Force dark theme by default for Sophisticated Dark experience
  dynamicColor: Boolean = false, // Disable dynamic colors to enforce branding
  content: @Composable () -> Unit,
) {
  val colorScheme = when (themeName) {
    "EMERALD" -> EmeraldColorScheme
    "OCEAN" -> OceanColorScheme
    "ROSE" -> RoseColorScheme
    else -> LavenderColorScheme
  }
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
