package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// PRD 3.1 Design Tokens - Premium Light Theme
// ==========================================
val VelocityPrimary = Color(0xFF1769E8)          // Primary actions & active navigation
val VelocityPrimaryDark = Color(0xFF1054C7)      // Pressed and emphasized states
val VelocityBackgroundLight = Color(0xFFF4F8FD)  // Main screen background
val VelocitySurfaceLight = Color(0xFFFFFFFF)     // Cards, dialogs, and panels
val VelocitySurfaceSecondary = Color(0xFFEAF2FC) // Search fields and secondary surfaces
val VelocityTextPrimary = Color(0xFF172033)      // Main text and headings
val VelocityTextSecondary = Color(0xFF64748B)    // Supporting information
val VelocityBorder = Color(0xFFDCE6F2)          // Dividers and card outlines
val VelocitySuccess = Color(0xFF168653)          // Successful operations
val VelocityWarning = Color(0xFFD97706)          // Warnings and caution states
val VelocityError = Color(0xFFDC2626)            // Errors and destructive actions
val VelocityFocus = Color(0xFF1769E8)            // Focus indicator

// Compatible naming for existing usages
val VelocityPrimaryLight = VelocityPrimary
val VelocityOnPrimaryLight = Color(0xFFFFFFFF)
val VelocityPrimaryContainerLight = VelocitySurfaceSecondary
val VelocityOnPrimaryContainerLight = VelocityPrimaryDark

val VelocitySecondaryLight = Color(0xFF0F766E)
val VelocityOnSecondaryLight = Color(0xFFFFFFFF)
val VelocitySecondaryContainerLight = Color(0xFFE6FFFA)
val VelocityOnSecondaryContainerLight = Color(0xFF115E59)

val VelocityTertiaryLight = Color(0xFF4F46E5)
val VelocityOnTertiaryLight = Color(0xFFFFFFFF)

val VelocityOnBackgroundLight = VelocityTextPrimary
val VelocityOnSurfaceLight = VelocityTextPrimary
val VelocitySurfaceVariantLight = VelocitySurfaceSecondary
val VelocityOnSurfaceVariantLight = VelocityTextSecondary

// ==========================================
// Dark Theme Colors
// ==========================================
val VelocityPrimaryDarkTheme = Color(0xFF38BDF8)
val VelocityOnPrimaryDark = Color(0xFF082F49)
val VelocityPrimaryContainerDark = Color(0xFF0C4A6E)
val VelocityOnPrimaryContainerDark = Color(0xFFBAE6FD)

val VelocityBackgroundDark = Color(0xFF090D16) // Deep Dark Navy
val VelocityOnBackgroundDark = Color(0xFFF1F5F9)
val VelocitySurfaceDark = Color(0xFF0F172A)     // Slate 900
val VelocityOnSurfaceDark = Color(0xFFF1F5F9)
val VelocitySurfaceVariantDark = Color(0xFF1E293B) // Slate 800
val VelocityOnSurfaceVariantDark = Color(0xFF94A3B8)

// ==========================================
// AMOLED Black Theme Colors
// ==========================================
val AmoledBackground = Color(0xFF000000)
val AmoledSurface = Color(0xFF0A0A0A)
val AmoledSurfaceVariant = Color(0xFF141414)
val AmoledBorder = Color(0xFF222222)

// ==========================================
// Color Accents & Themes (Blue, Green, Purple, Red)
// ==========================================
val ThemeGreenPrimary = Color(0xFF059669)
val ThemeGreenBg = Color(0xFFF0FDF4)
val ThemeGreenSurface2 = Color(0xFFDCFCE7)

val ThemePurplePrimary = Color(0xFF7C3AED)
val ThemePurpleBg = Color(0xFFFAF5FF)
val ThemePurpleSurface2 = Color(0xFFF3E8FF)

val ThemeRedPrimary = Color(0xFFDC2626)
val ThemeRedBg = Color(0xFFFEF2F2)
val ThemeRedSurface2 = Color(0xFFFEE2E2)

// Status & Browser indicator accents
val SecureGreen = VelocitySuccess
val InsecureWarning = VelocityWarning
val DangerRed = VelocityError
val PrivateModePurple = Color(0xFF7C3AED)
val PrivateModeSurface = Color(0xFF1F1235)
