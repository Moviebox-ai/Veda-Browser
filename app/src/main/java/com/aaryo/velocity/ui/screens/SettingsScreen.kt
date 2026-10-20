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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

/**
 * Screen 5: Settings
 * Exact replica of the reference mockup:
 * - Header: "← Settings"
 * - List rows with circular colored icon badges:
 *   1. Search Engine (Blue circle) -> "Google" >
 *   2. Privacy & Security (Blue circle) >
 *   3. Ad Blocker (Red circle) -> Switch (ON)
 *   4. Data Saver (Green circle) -> Switch (ON)
 *   5. Theme (Orange circle) -> "System" >
 *   6. Appearance (Purple circle) >
 *   7. Download Settings (Blue circle) >
 *   8. Languages (Magenta circle) >
 *   9. Home Page (Cyan circle) >
 *   10. Advanced (Cyan circle) >
 *   11. About (Blue circle) >
 */
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

    // Dialogs
    var showSearchEnginesDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showHomePageDialog by remember { mutableStateOf(false) }
    var showLanguagesDialog by remember { mutableStateOf(false) }
    var showAdvancedDialog by remember { mutableStateOf(false) }
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
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 1. Search Engine (Blue circle)
            SettingsMockRow(
                icon = Icons.Default.Search,
                iconBg = Color(0xFF2563EB),
                title = "Search Engine",
                subtitle = settings.defaultSearchEngine.displayName,
                onClick = { showSearchEnginesDialog = true }
            )

            // 2. Privacy & Security (Blue circle)
            SettingsMockRow(
                icon = Icons.Default.Security,
                iconBg = Color(0xFF2563EB),
                title = "Privacy & Security",
                onClick = { showPrivacyDialog = true }
            )

            // 3. Ad Blocker (Red circle) with Switch
            SettingsSwitchRow(
                icon = Icons.Default.Shield,
                iconBg = Color(0xFFEF4444),
                title = "Ad Blocker",
                checked = settings.adBlockGlobalEnabled,
                onCheckedChange = { onUpdateSettings(settings.copy(adBlockGlobalEnabled = it)) }
            )

            // 4. Data Saver (Green circle) with Switch
            SettingsSwitchRow(
                icon = Icons.Default.Eco,
                iconBg = Color(0xFF10B981),
                title = "Data Saver",
                checked = settings.blockThirdPartyCookies,
                onCheckedChange = { onUpdateSettings(settings.copy(blockThirdPartyCookies = it)) }
            )

            // 5. Theme (Orange circle) -> "System" >
            SettingsMockRow(
                icon = Icons.Default.Palette,
                iconBg = Color(0xFFF97316),
                title = "Theme",
                subtitle = settings.themeMode.lowercase().replaceFirstChar { it.uppercase() },
                onClick = onOpenAppearance
            )

            // 6. Appearance (Purple circle)
            SettingsMockRow(
                icon = Icons.Default.Tune,
                iconBg = Color(0xFF8B5CF6),
                title = "Appearance",
                onClick = onOpenAppearance
            )

            // 7. Download Settings (Blue circle)
            SettingsMockRow(
                icon = Icons.Default.FileDownload,
                iconBg = Color(0xFF2563EB),
                title = "Download Settings",
                onClick = onOpenDownloads
            )

            // 8. Languages (Magenta circle)
            SettingsMockRow(
                icon = Icons.Default.Language,
                iconBg = Color(0xFFD946EF),
                title = "Languages",
                subtitle = "English",
                onClick = { showLanguagesDialog = true }
            )

            // 9. Home Page (Cyan circle)
            SettingsMockRow(
                icon = Icons.Default.Home,
                iconBg = Color(0xFF06B6D4),
                title = "Home Page",
                subtitle = if (settings.homepageUrl == "velocity://newtab") "Velocity Start" else settings.homepageUrl,
                onClick = { showHomePageDialog = true }
            )

            // 10. Advanced (Cyan/Blue circle)
            SettingsMockRow(
                icon = Icons.Default.Code,
                iconBg = Color(0xFF0284C7),
                title = "Advanced",
                onClick = { showAdvancedDialog = true }
            )

            // 11. About (Blue circle)
            SettingsMockRow(
                icon = Icons.Default.Info,
                iconBg = Color(0xFF3B82F6),
                title = "About",
                onClick = { showAboutDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Search Engine Selection Dialog
        if (showSearchEnginesDialog) {
            AlertDialog(
                onDismissRequest = { showSearchEnginesDialog = false },
                title = { Text("Default Search Engine", color = Color.White) },
                text = {
                    Column {
                        SearchEngine.entries.forEach { engine ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onUpdateSettings(settings.copy(defaultSearchEngine = engine))
                                        showSearchEnginesDialog = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.defaultSearchEngine == engine,
                                    onClick = {
                                        onUpdateSettings(settings.copy(defaultSearchEngine = engine))
                                        showSearchEnginesDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = VelocityBlue)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(engine.displayName, color = Color.White, fontSize = 15.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSearchEnginesDialog = false }) {
                        Text("Done", color = VelocityCyan)
                    }
                },
                containerColor = Color(0xFF131927)
            )
        }

        // Privacy & Security Dialog
        if (showPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                title = { Text("Privacy & Security", color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("HTTPS-Only Mode", color = Color.White, fontSize = 14.sp)
                            Switch(
                                checked = settings.httpsOnlyMode,
                                onCheckedChange = { onUpdateSettings(settings.copy(httpsOnlyMode = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF3B82F6))
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Safe Browsing Shield", color = Color.White, fontSize = 14.sp)
                            Switch(
                                checked = settings.safeBrowsingEnabled,
                                onCheckedChange = { onUpdateSettings(settings.copy(safeBrowsingEnabled = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF3B82F6))
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Clear Data on Exit", color = Color.White, fontSize = 14.sp)
                            Switch(
                                checked = settings.clearDataOnExit,
                                onCheckedChange = { onUpdateSettings(settings.copy(clearDataOnExit = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF3B82F6))
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPrivacyDialog = false }) {
                        Text("OK", color = VelocityCyan)
                    }
                },
                containerColor = Color(0xFF131927)
            )
        }

        // Homepage Dialog
        if (showHomePageDialog) {
            var customHome by remember { mutableStateOf(settings.homepageUrl) }
            AlertDialog(
                onDismissRequest = { showHomePageDialog = false },
                title = { Text("Homepage", color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = customHome,
                            onValueChange = { customHome = it },
                            label = { Text("Start Page URL") }
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onUpdateSettings(settings.copy(homepageUrl = customHome.trim()))
                            showHomePageDialog = false
                        }
                    ) {
                        Text("Save", color = VelocityCyan)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            onUpdateSettings(settings.copy(homepageUrl = "velocity://newtab"))
                            showHomePageDialog = false
                        }
                    ) {
                        Text("Default", color = Color(0xFF94A3B8))
                    }
                },
                containerColor = Color(0xFF131927)
            )
        }

        // Languages Dialog
        if (showLanguagesDialog) {
            AlertDialog(
                onDismissRequest = { showLanguagesDialog = false },
                title = { Text("Languages", color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Preferred Display Language: English (US)", color = Color.White)
                        Text("Web Page Translation: Enabled", color = Color(0xFF94A3B8))
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLanguagesDialog = false }) {
                        Text("OK", color = VelocityCyan)
                    }
                },
                containerColor = Color(0xFF131927)
            )
        }

        // Advanced Dialog
        if (showAdvancedDialog) {
            AlertDialog(
                onDismissRequest = { showAdvancedDialog = false },
                title = { Text("Advanced Settings", color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("JavaScript JIT Engine", color = Color.White, fontSize = 14.sp)
                            Switch(
                                checked = settings.javaScriptEnabled,
                                onCheckedChange = { onUpdateSettings(settings.copy(javaScriptEnabled = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF3B82F6))
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Desktop View by Default", color = Color.White, fontSize = 14.sp)
                            Switch(
                                checked = settings.desktopSiteDefault,
                                onCheckedChange = { onUpdateSettings(settings.copy(desktopSiteDefault = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF3B82F6))
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAdvancedDialog = false }) {
                        Text("OK", color = VelocityCyan)
                    }
                },
                containerColor = Color(0xFF131927)
            )
        }

        // About Dialog
        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                title = { Text("About Velocity Browser", color = Color.White) },
                text = {
                    Column {
                        Text("Velocity Browser v2.4.0 (2026)", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Fast • Secure • Private Native Web Browser engine powered by Kotlin and Jetpack Compose.", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAboutDialog = false }) {
                        Text("OK", color = VelocityCyan)
                    }
                },
                containerColor = Color(0xFF131927)
            )
        }
    }
}

@Composable
private fun SettingsMockRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(iconBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        if (subtitle != null) {
            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = Color(0xFF475569),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(iconBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF3B82F6),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF1E283C)
            )
        )
    }
}
