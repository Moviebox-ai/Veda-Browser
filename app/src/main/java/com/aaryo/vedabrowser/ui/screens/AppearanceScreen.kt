package com.aaryo.vedabrowser.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaryo.vedabrowser.model.BrowserSettings

data class ThemeColorOption(
    val hex: String,
    val color: Color
)

/**
 * Screen 10: Theme & Colors
 * Exact replica of the reference mockup:
 * - Header: "← Theme"
 * - 3 Theme Previews: Light, Dark, System
 * - 10 Circular Color Accents (2 rows of 5)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    settings: BrowserSettings,
    onUpdateSettings: (BrowserSettings) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onDismiss()
    }

    val currentMode = settings.themeMode.uppercase()
    val currentAccent = settings.accentColorHex.uppercase()

    val row1Colors = listOf(
        ThemeColorOption("#2563EB", Color(0xFF2563EB)), // Blue
        ThemeColorOption("#8B5CF6", Color(0xFF8B5CF6)), // Purple
        ThemeColorOption("#EC4899", Color(0xFFEC4899)), // Pink/Magenta
        ThemeColorOption("#EF4444", Color(0xFFEF4444)), // Red
        ThemeColorOption("#F97316", Color(0xFFF97316))  // Orange
    )

    val row2Colors = listOf(
        ThemeColorOption("#10B981", Color(0xFF10B981)), // Green
        ThemeColorOption("#14B8A6", Color(0xFF14B8A6)), // Mint/Teal
        ThemeColorOption("#06B6D4", Color(0xFF06B6D4)), // Cyan
        ThemeColorOption("#0EA5E9", Color(0xFF0EA5E9)), // Sky Blue
        ThemeColorOption("#A855F7", Color(0xFFA855F7))  // Violet
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Theme",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("theme_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF070B19)
                )
            )
        },
        containerColor = Color(0xFF070B19),
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 3 Theme Mockup Cards Row: Light, Dark, System
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Light Card
                ThemeModeCard(
                    title = "Light",
                    isSelected = currentMode == "LIGHT",
                    isLight = true,
                    isSystem = false,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onUpdateSettings(settings.copy(themeMode = "LIGHT"))
                    }
                )

                // 2. Dark Card
                ThemeModeCard(
                    title = "Dark",
                    isSelected = currentMode == "DARK",
                    isLight = false,
                    isSystem = false,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onUpdateSettings(settings.copy(themeMode = "DARK"))
                    }
                )

                // 3. System Card
                ThemeModeCard(
                    title = "System",
                    isSelected = currentMode == "SYSTEM",
                    isLight = false,
                    isSystem = true,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onUpdateSettings(settings.copy(themeMode = "SYSTEM"))
                    }
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Color Accent Section (2 rows of 5 round color dots)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    row1Colors.forEach { option ->
                        val isSelected = currentAccent == option.hex.uppercase()
                        AccentColorCircle(
                            color = option.color,
                            isSelected = isSelected,
                            onClick = {
                                onUpdateSettings(settings.copy(accentColorHex = option.hex))
                            }
                        )
                    }
                }

                // Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    row2Colors.forEach { option ->
                        val isSelected = currentAccent == option.hex.uppercase()
                        AccentColorCircle(
                            color = option.color,
                            isSelected = isSelected,
                            onClick = {
                                onUpdateSettings(settings.copy(accentColorHex = option.hex))
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Preview Card for Light / Dark / System mode matching the mock card design
 */
@Composable
private fun ThemeModeCard(
    title: String,
    isSelected: Boolean,
    isLight: Boolean,
    isSystem: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFF1E283C),
                    shape = RoundedCornerShape(14.dp)
                )
                .background(
                    if (isLight) Color(0xFFFFFFFF)
                    else if (isSystem) Color(0xFF1A2234)
                    else Color(0xFF0F1523)
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (isLight) {
                // Light Mockup (White page, Google text, search bar)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Google",
                        color = Color(0xFF4285F4),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(14.dp)
                            .background(Color(0xFFE2E8F0), RoundedCornerShape(7.dp))
                    )
                }
            } else if (isSystem) {
                // Split Mockup
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(Color(0xFFFFFFFF))
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(Color(0xFF0F1523))
                    )
                }
            } else {
                // Dark Mockup with selected checkmark badge
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(12.dp)
                            .background(Color(0xFF1E283C), RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(14.dp)
                            .background(Color(0xFF161F32), RoundedCornerShape(7.dp))
                    )
                }
            }

            // Blue checkmark circle if selected
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(24.dp)
                        .background(Color(0xFF2563EB), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            color = if (isSelected) Color.White else Color(0xFF94A3B8),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun AccentColorCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(color)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected Accent",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
