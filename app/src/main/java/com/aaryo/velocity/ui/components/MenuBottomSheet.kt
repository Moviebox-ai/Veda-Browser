package com.aaryo.velocity.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaryo.velocity.model.Tab
import com.aaryo.velocity.ui.theme.VelocityBlue
import com.aaryo.velocity.ui.theme.VelocityCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VelocityMenuSheet(
    tab: Tab?,
    isDesktopMode: Boolean,
    onToggleDesktop: () -> Unit,
    onMenuAction: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F1523),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: Velocity branding + Close button (Matches Mobile 3)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VelocityLogoIcon(size = 32.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Velocity",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("menu_sheet_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Menu",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            HorizontalDivider(color = Color(0x2238BDF8), thickness = 1.dp)

            Spacer(modifier = Modifier.height(6.dp))

            // Menu Items List matching Mobile 3
            MenuItemRow(
                icon = Icons.Default.Add,
                title = "New Tab",
                shortcut = "Ctrl+T",
                onClick = {
                    onDismiss()
                    onMenuAction("NEW_TAB")
                }
            )

            MenuItemRow(
                icon = Icons.Default.Security,
                title = "New Incognito Tab",
                shortcut = "Ctrl+Shift+N",
                onClick = {
                    onDismiss()
                    onMenuAction("NEW_INCOGNITO_TAB")
                }
            )

            MenuItemRow(
                icon = Icons.Default.History,
                title = "History",
                onClick = {
                    onDismiss()
                    onMenuAction("HISTORY")
                }
            )

            MenuItemRow(
                icon = Icons.Default.Download,
                title = "Downloads",
                shortcut = "Ctrl+J",
                onClick = {
                    onDismiss()
                    onMenuAction("DOWNLOADS")
                }
            )

            MenuItemRow(
                icon = Icons.Default.Bookmark,
                title = "Bookmarks",
                shortcut = "Ctrl+Shift+B",
                onClick = {
                    onDismiss()
                    onMenuAction("BOOKMARKS")
                }
            )

            MenuItemRow(
                icon = Icons.Default.Tab,
                title = "Recent Tabs",
                onClick = {
                    onDismiss()
                    onMenuAction("TAB_SWITCHER")
                }
            )

            MenuItemRow(
                icon = Icons.Default.Extension,
                title = "Extensions",
                onClick = {
                    onDismiss()
                    onMenuAction("EXTENSIONS")
                }
            )

            MenuItemRow(
                icon = Icons.Default.Key,
                title = "Passwords",
                onClick = {
                    onDismiss()
                    onMenuAction("PASSWORDS")
                }
            )

            MenuItemRow(
                icon = Icons.Default.Palette,
                title = "Appearance",
                onClick = {
                    onDismiss()
                    onMenuAction("APPEARANCE")
                }
            )

            MenuItemRow(
                icon = Icons.Default.Settings,
                title = "Settings",
                onClick = {
                    onDismiss()
                    onMenuAction("SETTINGS")
                }
            )

            HorizontalDivider(
                color = Color(0x2238BDF8),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Desktop Site with Switch Toggle (Matches Mobile 3)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleDesktop() }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Computer,
                        contentDescription = "Desktop Site",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Desktop Site",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Switch(
                    checked = isDesktopMode,
                    onCheckedChange = { onToggleDesktop() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = VelocityBlue,
                        uncheckedTrackColor = Color(0xFF1E293B)
                    )
                )
            }

            MenuItemRow(
                icon = Icons.Default.FindInPage,
                title = "Find in Page",
                shortcut = "Ctrl+F",
                onClick = {
                    onDismiss()
                    onMenuAction("FIND_IN_PAGE")
                }
            )

            MenuItemRow(
                icon = Icons.Default.Translate,
                title = "Translate",
                onClick = {
                    onDismiss()
                    onMenuAction("TRANSLATE")
                }
            )

            MenuItemRow(
                icon = Icons.Default.OpenInBrowser,
                title = "Add to Home Screen",
                onClick = {
                    onDismiss()
                    onMenuAction("ADD_HOME_SCREEN")
                }
            )
        }
    }
}

@Composable
private fun MenuItemRow(
    icon: ImageVector,
    title: String,
    shortcut: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
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
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        if (shortcut != null) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0x2238BDF8)
            ) {
                Text(
                    text = shortcut,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
