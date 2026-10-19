package com.aaryo.velocity.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaryo.velocity.model.BrowserSettings
import com.aaryo.velocity.model.SearchEngine
import com.aaryo.velocity.ui.theme.VelocityBlue
import com.aaryo.velocity.ui.theme.VelocityCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: BrowserSettings,
    onUpdateSettings: (BrowserSettings) -> Unit,
    onOpenClearData: () -> Unit,
    onOpenAppearance: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onDismiss()
    }

    val scrollState = rememberScrollState()
    var showSearchEngines by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("settings_back_btn")
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Appearance (Prominently Highlighted as in Mobile 5)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = VelocityBlue.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, VelocityBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAppearance() }
                    .padding(bottom = 12.dp)
                    .testTag("settings_appearance_item")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Appearance",
                            tint = VelocityCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Appearance & Themes",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${settings.themeMode.lowercase().replaceFirstChar { it.uppercase() }} • ${settings.wallpaperTheme.lowercase().replaceFirstChar { it.uppercase() }} wallpaper",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open",
                        tint = VelocityCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Categories matching Mobile 5 from mockup
            SettingsNavRow(
                icon = Icons.Default.Tune,
                title = "General",
                value = "Standard",
                onClick = {}
            )

            SettingsNavRow(
                icon = Icons.Default.Security,
                title = "Privacy & Security",
                value = "Shield Active",
                onClick = {}
            )

            SettingsNavRow(
                icon = Icons.Default.Search,
                title = "Search Engine",
                value = "${settings.defaultSearchEngine.displayName} >",
                onClick = { showSearchEngines = !showSearchEngines }
            )

            if (showSearchEngines) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF131926),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        SearchEngine.entries.forEach { engine ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onUpdateSettings(settings.copy(defaultSearchEngine = engine))
                                        showSearchEngines = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.defaultSearchEngine == engine,
                                    onClick = {
                                        onUpdateSettings(settings.copy(defaultSearchEngine = engine))
                                        showSearchEngines = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = engine.displayName,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            SettingsNavRow(
                icon = Icons.Default.Image,
                title = "Wallpaper",
                value = "${settings.wallpaperTheme} >",
                onClick = onOpenAppearance
            )

            SettingsNavRow(
                icon = Icons.Default.Tab,
                title = "Tabs",
                value = "Grid View >",
                onClick = {}
            )

            SettingsNavRow(
                icon = Icons.Default.Home,
                title = "Homepage",
                value = "On >",
                onClick = {}
            )

            SettingsNavRow(
                icon = Icons.Default.Accessibility,
                title = "Accessibility",
                value = "${settings.textZoomPercent}% >",
                onClick = {}
            )

            SettingsNavRow(
                icon = Icons.Default.Language,
                title = "Languages",
                value = "English / Hindi >",
                onClick = {}
            )

            SettingsNavRow(
                icon = Icons.Default.Download,
                title = "Downloads",
                value = "Internal >",
                onClick = {}
            )

            HorizontalDivider(
                color = Color(0x2238BDF8),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Switch items
            Text(
                text = "Browsing Engine",
                color = VelocityCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            SettingsSwitchRow(
                icon = Icons.Default.Shield,
                title = "Velocity Shield",
                subtitle = "Block ads, trackers, and malicious scripts",
                checked = settings.adBlockGlobalEnabled,
                onCheckedChange = { onUpdateSettings(settings.copy(adBlockGlobalEnabled = it)) }
            )

            SettingsSwitchRow(
                icon = Icons.Default.Lock,
                title = "Do Not Track (DNT)",
                subtitle = "Send DNT header with every page visit",
                checked = settings.doNotTrackEnabled,
                onCheckedChange = { onUpdateSettings(settings.copy(doNotTrackEnabled = it)) }
            )

            SettingsSwitchRow(
                icon = Icons.Default.DesktopWindows,
                title = "Desktop Site by Default",
                subtitle = "Request desktop view for all sites",
                checked = settings.desktopSiteDefault,
                onCheckedChange = { onUpdateSettings(settings.copy(desktopSiteDefault = it)) }
            )

            SettingsSwitchRow(
                icon = Icons.Default.Code,
                title = "JavaScript Enabled",
                subtitle = "Execute modern web application scripts",
                checked = settings.javaScriptEnabled,
                onCheckedChange = { onUpdateSettings(settings.copy(javaScriptEnabled = it)) }
            )

            SettingsSwitchRow(
                icon = Icons.Default.Cookie,
                title = "Allow Cookies",
                subtitle = "Retain user session logins and preferences",
                checked = settings.cookiesEnabled,
                onCheckedChange = { onUpdateSettings(settings.copy(cookiesEnabled = it)) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Clear browsing data button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenClearData() }
                    .padding(vertical = 12.dp)
                    .testTag("setting_clear_data_btn"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0x33EF4444),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = "Clear Data",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Clear Browsing Data",
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = Color(0xFFEF4444)
                    )
                    Text(
                        text = "Clear history, cached images, cookies, and local storage",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            HorizontalDivider(
                color = Color(0x2238BDF8),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // About Velocity
            SettingsNavRow(
                icon = Icons.Default.Info,
                title = "About Velocity",
                value = "v1.0.0 >",
                onClick = {}
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Text(
            text = value,
            color = Color(0xFF64748B),
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Text(
                text = subtitle,
                color = Color(0xFF64748B),
                fontSize = 11.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = VelocityBlue,
                uncheckedTrackColor = Color(0xFF1E293B)
            )
        )
    }
}
