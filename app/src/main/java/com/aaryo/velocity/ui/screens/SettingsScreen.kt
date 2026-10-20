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
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.aaryo.velocity.ui.components.VelocityLogoIcon
import com.aaryo.velocity.ui.theme.VelocityBlue
import com.aaryo.velocity.ui.theme.VelocityCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: BrowserSettings,
    onUpdateSettings: (BrowserSettings) -> Unit,
    onOpenClearData: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenDownloads: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onDismiss()
    }

    val scrollState = rememberScrollState()

    // Dialog control states
    var showSearchEngines by remember { mutableStateOf(false) }
    var showGeneralDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTabsDialog by remember { mutableStateOf(false) }
    var showHomepageDialog by remember { mutableStateOf(false) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }
    var showLanguagesDialog by remember { mutableStateOf(false) }
    var showDownloadsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

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
            // Appearance & Themes Banner
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

            // Categories
            SettingsNavRow(
                icon = Icons.Default.Tune,
                title = "General",
                value = "Workspace: ${settings.activeWorkspace}",
                onClick = { showGeneralDialog = true }
            )

            SettingsNavRow(
                icon = Icons.Default.Security,
                title = "Privacy & Security",
                value = if (settings.adBlockGlobalEnabled) "Shield Active" else "Shield Paused",
                onClick = { showPrivacyDialog = true }
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
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = VelocityCyan)
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
                value = if (settings.desktopSiteDefault) "Desktop Site" else "Mobile Grid",
                onClick = { showTabsDialog = true }
            )

            SettingsNavRow(
                icon = Icons.Default.Home,
                title = "Homepage",
                value = if (settings.homepageUrl == "velocity://newtab") "Start Page" else "Custom URL",
                onClick = { showHomepageDialog = true }
            )

            SettingsNavRow(
                icon = Icons.Default.Accessibility,
                title = "Accessibility & Text Zoom",
                value = "${settings.textZoomPercent}% >",
                onClick = { showAccessibilityDialog = true }
            )

            SettingsNavRow(
                icon = Icons.Default.Language,
                title = "Languages & Translation",
                value = "English / Hindi >",
                onClick = { showLanguagesDialog = true }
            )

            SettingsNavRow(
                icon = Icons.Default.Download,
                title = "Downloads Storage",
                value = "Downloads folder >",
                onClick = { showDownloadsDialog = true }
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
                onClick = { showAboutDialog = true }
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // 1. General Settings Dialog
    if (showGeneralDialog) {
        val workspaces = listOf("Personal", "Work", "Study", "Entertainment")
        AlertDialog(
            onDismissRequest = { showGeneralDialog = false },
            title = { Text("General Settings", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Active Workspace:", color = VelocityCyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    workspaces.forEach { ws ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUpdateSettings(settings.copy(activeWorkspace = ws)) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = settings.activeWorkspace == ws,
                                onClick = { onUpdateSettings(settings.copy(activeWorkspace = ws)) },
                                colors = RadioButtonDefaults.colors(selectedColor = VelocityCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(ws, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGeneralDialog = false }) {
                    Text("Done", color = VelocityCyan)
                }
            },
            containerColor = Color(0xFF131926)
        )
    }

    // 2. Privacy & Security Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy & Security", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    SettingsSwitchRow(
                        icon = Icons.Default.CleaningServices,
                        title = "Clear Data on Exit",
                        subtitle = "Auto-clear cache and cookies when closed",
                        checked = settings.clearDataOnExit,
                        onCheckedChange = { onUpdateSettings(settings.copy(clearDataOnExit = it)) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsSwitchRow(
                        icon = Icons.Default.Palette,
                        title = "Force Dark Web Content",
                        subtitle = "Invert web page colors for dark mode",
                        checked = settings.forceDarkWebContent,
                        onCheckedChange = { onUpdateSettings(settings.copy(forceDarkWebContent = it)) }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Close", color = VelocityCyan)
                }
            },
            containerColor = Color(0xFF131926)
        )
    }

    // 3. Tabs Dialog
    if (showTabsDialog) {
        AlertDialog(
            onDismissRequest = { showTabsDialog = false },
            title = { Text("Tabs Management", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Velocity uses isolated Chromium process contexts for Incognito tabs, preventing cookies or cache leakage.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    SettingsSwitchRow(
                        icon = Icons.Default.DesktopWindows,
                        title = "Open Desktop View Always",
                        subtitle = "Request PC user agent for all tabs",
                        checked = settings.desktopSiteDefault,
                        onCheckedChange = { onUpdateSettings(settings.copy(desktopSiteDefault = it)) }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTabsDialog = false }) {
                    Text("Done", color = VelocityCyan)
                }
            },
            containerColor = Color(0xFF131926)
        )
    }

    // 4. Homepage Dialog
    if (showHomepageDialog) {
        var isCustom by remember { mutableStateOf(settings.homepageUrl != "velocity://newtab") }
        var customUrl by remember { mutableStateOf(if (isCustom) settings.homepageUrl else "https://") }

        AlertDialog(
            onDismissRequest = { showHomepageDialog = false },
            title = { Text("Homepage URL", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCustom = false }
                            .padding(vertical = 6.dp)
                    ) {
                        RadioButton(
                            selected = !isCustom,
                            onClick = { isCustom = false },
                            colors = RadioButtonDefaults.colors(selectedColor = VelocityCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Velocity Start Page (Default)", color = Color.White, fontSize = 14.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCustom = true }
                            .padding(vertical = 6.dp)
                    ) {
                        RadioButton(
                            selected = isCustom,
                            onClick = { isCustom = true },
                            colors = RadioButtonDefaults.colors(selectedColor = VelocityCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Custom Website URL", color = Color.White, fontSize = 14.sp)
                    }

                    if (isCustom) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customUrl,
                            onValueChange = { customUrl = it },
                            label = { Text("Homepage Address") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = VelocityCyan
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val finalUrl = if (!isCustom) "velocity://newtab" else {
                            if (customUrl.startsWith("http")) customUrl.trim() else "https://${customUrl.trim()}"
                        }
                        onUpdateSettings(settings.copy(homepageUrl = finalUrl))
                        showHomepageDialog = false
                    }
                ) {
                    Text("Save", color = VelocityCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showHomepageDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF131926)
        )
    }

    // 5. Accessibility & Text Zoom Dialog
    if (showAccessibilityDialog) {
        val zoomOptions = listOf(75, 90, 100, 115, 130, 150, 175, 200)
        var selectedZoom by remember { mutableStateOf(settings.textZoomPercent) }

        AlertDialog(
            onDismissRequest = { showAccessibilityDialog = false },
            title = { Text("Web Text Zoom", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Adjust text size scaling for web pages (Current: $selectedZoom%):",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    zoomOptions.chunked(4).forEach { rowOptions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            rowOptions.forEach { zoom ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedZoom == zoom) VelocityBlue else Color(0xFF1E293B),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (selectedZoom == zoom) VelocityCyan else Color(0x3394A3B8)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp)
                                        .clickable {
                                            selectedZoom = zoom
                                            onUpdateSettings(settings.copy(textZoomPercent = zoom))
                                        }
                                ) {
                                    Text(
                                        text = "$zoom%",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccessibilityDialog = false }) {
                    Text("Done", color = VelocityCyan, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF131926)
        )
    }

    // 6. Languages Dialog
    if (showLanguagesDialog) {
        val languages = listOf("English", "Hindi (हिंदी)", "Spanish (Español)", "French (Français)", "German (Deutsch)", "Japanese (日本語)")
        var selectedLang by remember { mutableStateOf("Hindi (हिंदी)") }

        AlertDialog(
            onDismissRequest = { showLanguagesDialog = false },
            title = { Text("Languages & Translation", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Target Translation Language:", color = VelocityCyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    languages.forEach { lang ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedLang = lang }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedLang == lang,
                                onClick = { selectedLang = lang },
                                colors = RadioButtonDefaults.colors(selectedColor = VelocityCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(lang, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguagesDialog = false }) {
                    Text("Save", color = VelocityCyan)
                }
            },
            containerColor = Color(0xFF131926)
        )
    }

    // 7. Downloads Dialog
    if (showDownloadsDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadsDialog = false },
            title = { Text("Downloads Location", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = VelocityCyan, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Default Directory", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("/storage/emulated/0/Download", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "Files downloaded in Velocity are saved to your device's standard Downloads folder and visible across all file managers.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDownloadsDialog = false
                        onOpenDownloads()
                    }
                ) {
                    Text("View Downloads", color = VelocityCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadsDialog = false }) {
                    Text("Close", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF131926)
        )
    }

    // 8. About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VelocityLogoIcon(size = 32.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Velocity Browser", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column {
                    Text("Version: 1.0.0 (Production Build)", color = VelocityCyan, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Engine: Android Chromium System WebView\n" +
                                "Privacy: Built-in AdBlock & Tracker Protection\n" +
                                "Security: TLS 1.3, DNT, Multi-profile Incognito\n" +
                                "Extensions: Userscripts GM API Engine",
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Fast, private, and fully customizable next-generation browser.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK", color = VelocityCyan, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF131926)
        )
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
