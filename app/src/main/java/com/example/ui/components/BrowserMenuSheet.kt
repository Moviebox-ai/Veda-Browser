package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.Tab
import com.example.ui.theme.PrivateModePurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserMenuSheet(
    currentTab: Tab?,
    isBookmarked: Boolean,
    onDismiss: () -> Unit,
    onNewTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDownloads: () -> Unit,
    onFindInPage: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onToggleReaderMode: () -> Unit,
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
    onOpenSpeedTest: () -> Unit,
    onOpenSettings: () -> Unit,
    onClearData: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isInternal = currentTab?.isInternal == true

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .verticalScroll(rememberScrollState())
                .testTag("browser_menu_sheet")
        ) {
            // New Tab & Private Tab
            MenuItem(
                icon = Icons.Default.Add,
                title = "New Tab",
                onClick = { onDismiss(); onNewTab() }
            )
            MenuItem(
                icon = Icons.Default.Security,
                title = "New Private Tab",
                iconTint = PrivateModePurple,
                onClick = { onDismiss(); onNewPrivateTab() }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            // Page Actions (only enabled if not internal page)
            if (!isInternal) {
                MenuItem(
                    icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    title = if (isBookmarked) "Remove Bookmark" else "Bookmark Page",
                    iconTint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { onToggleBookmark(); onDismiss() }
                )
                MenuItem(
                    icon = Icons.Default.Share,
                    title = "Share Link",
                    onClick = { onDismiss(); onShare() }
                )
                MenuItem(
                    icon = Icons.Default.ContentCopy,
                    title = "Copy Link",
                    onClick = { onDismiss(); onCopyLink() }
                )
                MenuItem(
                    icon = Icons.Default.FindInPage,
                    title = "Find in Page",
                    onClick = { onDismiss(); onFindInPage() }
                )
                MenuItem(
                    icon = Icons.Default.AutoStories,
                    title = "Reader Mode",
                    onClick = { onDismiss(); onToggleReaderMode() }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleDesktopMode() }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DesktopWindows,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Desktop Site",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = currentTab?.desktopMode == true,
                        onCheckedChange = { onToggleDesktopMode() }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            }

            // Navigation Destinations
            MenuItem(
                icon = Icons.Default.Bookmark,
                title = "Bookmarks",
                onClick = { onDismiss(); onOpenBookmarks() }
            )
            MenuItem(
                icon = Icons.Default.History,
                title = "History",
                onClick = { onDismiss(); onOpenHistory() }
            )
            MenuItem(
                icon = Icons.Default.Download,
                title = "Downloads",
                onClick = { onDismiss(); onOpenDownloads() }
            )
            MenuItem(
                icon = Icons.Default.Speed,
                title = "Speed Test & Booster",
                iconTint = MaterialTheme.colorScheme.primary,
                onClick = { onDismiss(); onOpenSpeedTest() }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            MenuItem(
                icon = Icons.Default.Settings,
                title = "Settings",
                onClick = { onDismiss(); onOpenSettings() }
            )
            MenuItem(
                icon = Icons.Default.Delete,
                title = "Clear Browsing Data",
                iconTint = MaterialTheme.colorScheme.error,
                onClick = { onDismiss(); onClearData() }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    title: String,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
