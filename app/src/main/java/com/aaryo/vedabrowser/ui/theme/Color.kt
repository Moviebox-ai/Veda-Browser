package com.aaryo.vedabrowser.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Core Velocity Brand Colors
val VelocityCyan = Color(0xFF00E5FF)
val VelocityBlue = Color(0xFF2563EB)
val VelocityBlueDark = Color(0xFF1D4ED8)
val VelocityPurple = Color(0xFF8B5CF6)
val VelocityNavy = Color(0xFF0A0E17)
val VelocitySlate = Color(0xFF1E293B)
val VelocityCardDark = Color(0xFF131926)
val VelocitySurfaceDark = Color(0xFF080B11)

// UI Feedback Colors
val CyanAccent = Color(0xFF00D2FF)
val GreenSecure = Color(0xFF10B981)
val RedWarning = Color(0xFFEF4444)
val AmberAlert = Color(0xFFF59E0B)

// Accent Palette from Mockup (Customize Browser / Appearance)
val AccentBlue = Color(0xFF2563EB)
val AccentPurple = Color(0xFF9333EA)
val AccentPink = Color(0xFFEC4899)
val AccentOrange = Color(0xFFF97316)
val AccentYellow = Color(0xFFF59E0B)
val AccentGreen = Color(0xFF10B981)
val AccentCyan = Color(0xFF06B6D4)
val AccentGradientStart = Color(0xFF3B82F6)
val AccentGradientEnd = Color(0xFF8B5CF6)

// Quick Dial / Brand Colors from Mockup
val BrandYouTube = Color(0xFFFF0000)
val BrandGoogleBlue = Color(0xFF4285F4)
val BrandGoogleRed = Color(0xFFEA4335)
val BrandGoogleYellow = Color(0xFFFBBC05)
val BrandGoogleGreen = Color(0xFF34A853)
val BrandGmail = Color(0xFFEA4335)
val BrandChatGPT = Color(0xFF10A37F)
val BrandGitHub = Color(0xFF24292E)
val BrandFacebook = Color(0xFF1877F2)
val BrandInstagramStart = Color(0xFFE1306C)
val BrandInstagramEnd = Color(0xFF833AB4)
val BrandAmazon = Color(0xFFFF9900)
val BrandFlipkart = Color(0xFFFFE500)
val BrandFigma = Color(0xFFF24E1E)

// Dark Glass & Card Colors
val GlassBorderDark = Color(0x333B82F6)
val GlassSurfaceDark = Color(0xCC111827)
val OmniboxGlassDark = Color(0xD9141B2D)
val CardBackgroundDark = Color(0xFF131927)
val CardBorderDark = Color(0xFF1E283C)

// Gradient Brushes
val VelocityGradient = Brush.horizontalGradient(
    colors = listOf(VelocityCyan, VelocityBlue, VelocityPurple)
)

val VelocityLogoGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF00E5FF), Color(0xFF3B82F6), Color(0xFFA855F7))
)

val MountainSkyGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF070A14),
        Color(0xFF12122B),
        Color(0xFF2A1542),
        Color(0xFF4A1942),
        Color(0xFF1B1B36)
    )
)

// Light Theme Colors
val LightPrimary = Color(0xFF2563EB)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFDBEAFE)
val LightOnPrimaryContainer = Color(0xFF1E40AF)
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF0F172A)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightOnSurfaceVariant = Color(0xFF475569)
val LightOutline = Color(0xFFCBD5E1)

// Dark Theme Colors
val DarkPrimary = Color(0xFF3B82F6)
val DarkOnPrimary = Color(0xFFFFFFFF)
val DarkPrimaryContainer = Color(0xFF1D4ED8)
val DarkOnPrimaryContainer = Color(0xFFDBEAFE)
val DarkBackground = Color(0xFF080B11)
val DarkSurface = Color(0xFF0E131F)
val DarkOnSurface = Color(0xFFF1F5F9)
val DarkSurfaceVariant = Color(0xFF161D2D)
val DarkOnSurfaceVariant = Color(0xFF94A3B8)
val DarkOutline = Color(0xFF243048)

// Incognito Stealth Colors
val IncognitoPurple = Color(0xFFA855F7)
val IncognitoDarkBg = Color(0xFF090614)
val IncognitoSurface = Color(0xFF16102B)
