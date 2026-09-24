package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = CyberCyan,
  onPrimary = Color.Black,
  primaryContainer = Color(0xFF003840),
  onPrimaryContainer = CyberCyan,
  secondary = CyberRed,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFF4A0018),
  onSecondaryContainer = Color(0xFFFFB3C6),
  tertiary = CyberYellow,
  onTertiary = Color.Black,
  background = CyberBg,
  onBackground = TextPrimary,
  surface = CyberSurface,
  onSurface = TextPrimary,
  surfaceVariant = CyberSurfaceVariant,
  onSurfaceVariant = TextSecondary,
  outline = CyberCardBorder
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = DarkColorScheme,
    typography = Typography,
    content = content
  )
}
