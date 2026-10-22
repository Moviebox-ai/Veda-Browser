package com.aaryo.vedabrowser.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

fun parseAccentColor(hex: String, fallback: Color = Color(0xFF2563EB)): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        val colorInt = when (cleanHex.length) {
            6 -> android.graphics.Color.parseColor("#FF$cleanHex")
            8 -> android.graphics.Color.parseColor("#$cleanHex")
            else -> return fallback
        }
        Color(colorInt)
    } catch (_: Exception) {
        fallback
    }
}

@Composable
fun VedaTheme(
    themeMode: String = "DARK",
    accentColorHex: String = "#2563EB",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = VelocityTheme(themeMode, accentColorHex, dynamicColor, content)

@Composable
fun VelocityTheme(
    themeMode: String = "DARK",
    accentColorHex: String = "#2563EB",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode.uppercase()) {
        "LIGHT" -> false
        "SYSTEM" -> isSystemDark
        else -> true // Default to Dark matching mockup!
    }

    val primaryAccent = parseAccentColor(accentColorHex, DarkPrimary)

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> darkColorScheme(
            primary = primaryAccent,
            onPrimary = Color.White,
            primaryContainer = primaryAccent.copy(alpha = 0.3f),
            onPrimaryContainer = Color.White,
            background = DarkBackground,
            surface = DarkSurface,
            onSurface = DarkOnSurface,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = DarkOnSurfaceVariant,
            outline = DarkOutline
        )
        else -> lightColorScheme(
            primary = primaryAccent,
            onPrimary = Color.White,
            primaryContainer = primaryAccent.copy(alpha = 0.15f),
            onPrimaryContainer = primaryAccent,
            background = LightBackground,
            surface = LightSurface,
            onSurface = LightOnSurface,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = LightOnSurfaceVariant,
            outline = LightOutline
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
