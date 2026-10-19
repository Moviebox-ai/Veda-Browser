package com.aaryo.velocity.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaryo.velocity.model.BrowserSettings
import com.aaryo.velocity.ui.components.VelocityLogoIcon
import com.aaryo.velocity.ui.components.WallpaperCanvas
import com.aaryo.velocity.ui.theme.AccentBlue
import com.aaryo.velocity.ui.theme.AccentCyan
import com.aaryo.velocity.ui.theme.AccentGreen
import com.aaryo.velocity.ui.theme.AccentOrange
import com.aaryo.velocity.ui.theme.AccentPink
import com.aaryo.velocity.ui.theme.AccentPurple
import com.aaryo.velocity.ui.theme.AccentYellow
import com.aaryo.velocity.ui.theme.VelocityBlue
import com.aaryo.velocity.ui.theme.VelocityCyan
import com.aaryo.velocity.ui.theme.parseAccentColor

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

    val scrollState = rememberScrollState()

    val accentColors = listOf(
        Pair("#2563EB", AccentBlue),
        Pair("#9333EA", AccentPurple),
        Pair("#EC4899", AccentPink),
        Pair("#F97316", AccentOrange),
        Pair("#F59E0B", AccentYellow),
        Pair("#10B981", AccentGreen),
        Pair("#06B6D4", AccentCyan)
    )

    val wallpapers = listOf(
        Pair("MOUNTAINS", "Alpine Twilight"),
        Pair("FOREST", "Misty Dawn"),
        Pair("CYBER", "Cyber Neon"),
        Pair("MINIMAL", "Obsidian Mesh")
    )

    val currentAccent = parseAccentColor(settings.accentColorHex, AccentBlue)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Appearance",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("appearance_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF080B11)
                )
            )
        },
        containerColor = Color(0xFF080B11),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // 1. Live Preview Card (Matching Mobile 6 in mockup!)
            Text(
                text = "Preview",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                shape = RoundedCornerShape(settings.cornerRadiusDp.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1523)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, currentAccent.copy(alpha = 0.6f))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    WallpaperCanvas(theme = settings.wallpaperTheme, modifier = Modifier.fillMaxSize())

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        VelocityLogoIcon(size = 36.dp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Velocity",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xCC111827),
                            border = androidx.compose.foundation.BorderStroke(1.dp, currentAccent.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(36.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔍 Search or type URL",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Themes (Light / Dark / System)
            Text(
                text = "Theme",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThemeOptionCard(
                    title = "Light",
                    icon = Icons.Default.LightMode,
                    isSelected = settings.themeMode == "LIGHT",
                    accentColor = currentAccent,
                    onClick = { onUpdateSettings(settings.copy(themeMode = "LIGHT")) },
                    modifier = Modifier.weight(1f)
                )

                ThemeOptionCard(
                    title = "Dark",
                    icon = Icons.Default.DarkMode,
                    isSelected = settings.themeMode == "DARK",
                    accentColor = currentAccent,
                    onClick = { onUpdateSettings(settings.copy(themeMode = "DARK")) },
                    modifier = Modifier.weight(1f)
                )

                ThemeOptionCard(
                    title = "System",
                    icon = Icons.Default.PhoneAndroid,
                    isSelected = settings.themeMode == "SYSTEM",
                    accentColor = currentAccent,
                    onClick = { onUpdateSettings(settings.copy(themeMode = "SYSTEM")) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Accent Color Swatches
            Text(
                text = "Accent Color",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                accentColors.forEach { (hex, color) ->
                    val isSelected = settings.accentColorHex.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { onUpdateSettings(settings.copy(accentColorHex = hex)) }
                            .then(
                                if (isSelected) Modifier.border(2.5.dp, Color.White, CircleShape)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Wallpaper Selection
            Text(
                text = "Wallpaper Atmosphere",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                wallpapers.forEach { (themeKey, label) ->
                    val isSelected = settings.wallpaperTheme == themeKey
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) currentAccent.copy(alpha = 0.2f) else Color(0xFF131926),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) currentAccent else Color(0x3338BDF8)
                        ),
                        modifier = Modifier.clickable {
                            onUpdateSettings(settings.copy(wallpaperTheme = themeKey))
                        }
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Toolbar Style (Default / Compact / Bottom)
            Text(
                text = "Toolbar Style",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("Default", "Compact", "Bottom").forEach { style ->
                    val isSelected = settings.toolbarStyle.equals(style, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) currentAccent else Color(0xFF131926),
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0x3394A3B8)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onUpdateSettings(settings.copy(toolbarStyle = style.uppercase())) }
                    ) {
                        Text(
                            text = style,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 6. Corner Radius Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Corner Radius",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${settings.cornerRadiusDp} dp",
                    color = currentAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Slider(
                value = settings.cornerRadiusDp.toFloat(),
                onValueChange = { onUpdateSettings(settings.copy(cornerRadiusDp = it.toInt())) },
                valueRange = 8f..28f,
                steps = 4,
                colors = SliderDefaults.colors(
                    thumbColor = currentAccent,
                    activeTrackColor = currentAccent,
                    inactiveTrackColor = Color(0xFF1E293B)
                )
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ThemeOptionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.15f) else Color(0xFF131926),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isSelected) accentColor else Color(0x2238BDF8)
        ),
        modifier = modifier
            .height(72.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) accentColor else Color(0xFF94A3B8),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
