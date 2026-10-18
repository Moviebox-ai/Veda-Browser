package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Premium Light Scheme (PRD 3.1)
private val PremiumLightColorScheme = lightColorScheme(
    primary = VelocityPrimary,
    onPrimary = Color.White,
    primaryContainer = VelocitySurfaceSecondary,
    onPrimaryContainer = VelocityPrimaryDark,
    secondary = VelocityPrimaryDark,
    onSecondary = Color.White,
    secondaryContainer = VelocitySurfaceSecondary,
    onSecondaryContainer = VelocityTextPrimary,
    tertiary = VelocitySuccess,
    onTertiary = Color.White,
    background = VelocityBackgroundLight,
    onBackground = VelocityTextPrimary,
    surface = VelocitySurfaceLight,
    onSurface = VelocityTextPrimary,
    surfaceVariant = VelocitySurfaceSecondary,
    onSurfaceVariant = VelocityTextSecondary,
    outline = VelocityBorder,
    outlineVariant = VelocityBorder,
    error = VelocityError,
    onError = Color.White
)

// Dark Scheme
private val DarkColorScheme = darkColorScheme(
    primary = VelocityPrimaryDarkTheme,
    onPrimary = VelocityOnPrimaryDark,
    primaryContainer = VelocityPrimaryContainerDark,
    onPrimaryContainer = VelocityOnPrimaryContainerDark,
    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF042F2E),
    background = VelocityBackgroundDark,
    onBackground = VelocityOnBackgroundDark,
    surface = VelocitySurfaceDark,
    onSurface = VelocityOnSurfaceDark,
    surfaceVariant = VelocitySurfaceVariantDark,
    onSurfaceVariant = VelocityOnSurfaceVariantDark,
    outline = Color(0xFF334155),
    error = VelocityError,
    onError = Color.White
)

// AMOLED Black Scheme
private val AmoledColorScheme = darkColorScheme(
    primary = VelocityPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E293B),
    onPrimaryContainer = Color(0xFF93C5FD),
    secondary = VelocityPrimary,
    onSecondary = Color.White,
    background = AmoledBackground,
    onBackground = Color(0xFFF8FAFC),
    surface = AmoledSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = AmoledBorder,
    error = VelocityError,
    onError = Color.White
)

// Blue Variant Scheme
private val BlueLightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    background = Color(0xFFF0F9FF),
    onBackground = VelocityTextPrimary,
    surface = Color.White,
    onSurface = VelocityTextPrimary,
    surfaceVariant = Color(0xFFE0F2FE),
    onSurfaceVariant = Color(0xFF0369A1),
    outline = Color(0xFFBAE6FD)
)

// Green Emerald Scheme
private val GreenLightColorScheme = lightColorScheme(
    primary = ThemeGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = ThemeGreenSurface2,
    onPrimaryContainer = Color(0xFF065F46),
    background = ThemeGreenBg,
    onBackground = VelocityTextPrimary,
    surface = Color.White,
    onSurface = VelocityTextPrimary,
    surfaceVariant = ThemeGreenSurface2,
    onSurfaceVariant = Color(0xFF065F46),
    outline = Color(0xFFBBF7D0)
)

// Purple Royal Scheme
private val PurpleLightColorScheme = lightColorScheme(
    primary = ThemePurplePrimary,
    onPrimary = Color.White,
    primaryContainer = ThemePurpleSurface2,
    onPrimaryContainer = Color(0xFF581C87),
    background = ThemePurpleBg,
    onBackground = VelocityTextPrimary,
    surface = Color.White,
    onSurface = VelocityTextPrimary,
    surfaceVariant = ThemePurpleSurface2,
    onSurfaceVariant = Color(0xFF581C87),
    outline = Color(0xFFE9D5FF)
)

// Red Crimson Scheme
private val RedLightColorScheme = lightColorScheme(
    primary = ThemeRedPrimary,
    onPrimary = Color.White,
    primaryContainer = ThemeRedSurface2,
    onPrimaryContainer = Color(0xFF991B1B),
    background = ThemeRedBg,
    onBackground = VelocityTextPrimary,
    surface = Color.White,
    onSurface = VelocityTextPrimary,
    surfaceVariant = ThemeRedSurface2,
    onSurfaceVariant = Color(0xFF991B1B),
    outline = Color(0xFFFECACA)
)

@Composable
fun VelocityTheme(
    themeMode: String = "light",
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme: ColorScheme = when (themeMode) {
        "light" -> PremiumLightColorScheme
        "dark" -> DarkColorScheme
        "amoled" -> AmoledColorScheme
        "blue" -> BlueLightColorScheme
        "green" -> GreenLightColorScheme
        "purple" -> PurpleLightColorScheme
        "red" -> RedLightColorScheme
        "system" -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else if (darkTheme) {
                DarkColorScheme
            } else {
                PremiumLightColorScheme
            }
        }
        else -> PremiumLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    VelocityTheme(themeMode = if (darkTheme) "dark" else "light", dynamicColor = dynamicColor, content = content)
}
