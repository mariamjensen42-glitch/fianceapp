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

private val DarkColorScheme = darkColorScheme(
    primary = NaturalPrimaryDark,
    onPrimary = NaturalOnPrimaryDark,
    primaryContainer = NaturalPrimaryContainerDark,
    onPrimaryContainer = NaturalOnPrimaryContainerDark,
    secondary = NaturalSecondaryDark,
    onSecondary = NaturalOnSecondaryDark,
    secondaryContainer = NaturalSecondaryContainerDark,
    onSecondaryContainer = NaturalOnSecondaryContainerDark,
    tertiary = NaturalTertiaryDark,
    onTertiary = NaturalOnTertiaryDark,
    background = NaturalBackgroundDark,
    onBackground = NaturalOnBackgroundDark,
    surface = NaturalSurfaceDark,
    onSurface = NaturalOnSurfaceDark,
    surfaceVariant = NaturalSurfaceVariantDark,
    onSurfaceVariant = NaturalOnSurfaceVariantDark,
    outline = NaturalOutlineDark,
    outlineVariant = NaturalOutlineVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = NaturalPrimaryLight,
    onPrimary = NaturalOnPrimaryLight,
    primaryContainer = NaturalPrimaryContainerLight,
    onPrimaryContainer = NaturalOnPrimaryContainerLight,
    secondary = NaturalSecondaryLight,
    onSecondary = NaturalOnSecondaryLight,
    secondaryContainer = NaturalSecondaryContainerLight,
    onSecondaryContainer = NaturalOnSecondaryContainerLight,
    tertiary = NaturalTertiaryLight,
    onTertiary = NaturalOnTertiaryLight,
    background = NaturalBackgroundLight,
    onBackground = NaturalOnBackgroundLight,
    surface = NaturalSurfaceLight,
    onSurface = NaturalOnSurfaceLight,
    surfaceVariant = NaturalSurfaceVariantLight,
    onSurfaceVariant = NaturalOnSurfaceVariantLight,
    outline = NaturalOutlineLight,
    outlineVariant = NaturalOutlineVariantLight
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color by default for Natural Tones
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
