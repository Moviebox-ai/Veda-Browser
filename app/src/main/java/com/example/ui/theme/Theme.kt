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
    primary = VelocityPrimaryDark,
    onPrimary = VelocityOnPrimaryDark,
    primaryContainer = VelocityPrimaryContainerDark,
    onPrimaryContainer = VelocityOnPrimaryContainerDark,
    secondary = VelocitySecondaryDark,
    onSecondary = VelocityOnSecondaryDark,
    secondaryContainer = VelocitySecondaryContainerDark,
    onSecondaryContainer = VelocityOnSecondaryContainerDark,
    tertiary = VelocityTertiaryDark,
    onTertiary = VelocityOnTertiaryDark,
    background = VelocityBackgroundDark,
    onBackground = VelocityOnBackgroundDark,
    surface = VelocitySurfaceDark,
    onSurface = VelocityOnSurfaceDark,
    surfaceVariant = VelocitySurfaceVariantDark,
    onSurfaceVariant = VelocityOnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = VelocityPrimaryLight,
    onPrimary = VelocityOnPrimaryLight,
    primaryContainer = VelocityPrimaryContainerLight,
    onPrimaryContainer = VelocityOnPrimaryContainerLight,
    secondary = VelocitySecondaryLight,
    onSecondary = VelocityOnSecondaryLight,
    secondaryContainer = VelocitySecondaryContainerLight,
    onSecondaryContainer = VelocityOnSecondaryContainerLight,
    tertiary = VelocityTertiaryLight,
    onTertiary = VelocityOnTertiaryLight,
    background = VelocityBackgroundLight,
    onBackground = VelocityOnBackgroundLight,
    surface = VelocitySurfaceLight,
    onSurface = VelocityOnSurfaceLight,
    surfaceVariant = VelocitySurfaceVariantLight,
    onSurfaceVariant = VelocityOnSurfaceVariantLight
)

@Composable
fun VelocityTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
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
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    VelocityTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
