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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaryo.velocity.model.Tab

/**
 * Screen 4: Menu
 * Exact replica of the reference mockup:
 * - Header: "Menu"
 * - Sign in to sync profile card
 * - 8 Quick action grid (New Tab, Incognito, History, Downloads, Bookmarks, Recent Tabs, Find in Page, Desktop Site)
 * - 4 circular toggle pills: Dark Mode, Ad Block, Data Saver, Settings
 * - Bottom list items: Share, Rate App, Help & Feedback
 */
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
        containerColor = Color(0xFF0C101F),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: "Menu"
            Text(
                text = "Menu",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 16.dp)
            )

            // Top Card: Sign in to sync
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF131D33),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2E4E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onMenuAction("PASSWORDS")
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF2563EB), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sign in to sync",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Bookmarks, history, tabs",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Open",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 8 Quick Actions Grid (2 rows x 4 columns)
            // Row 1: New Tab, Incognito, History, Downloads
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MenuQuickActionItem(
                    icon = Icons.Default.Add,
                    label = "New Tab",
                    onClick = {
                        onDismiss()
                        onMenuAction("NEW_TAB")
                    }
                )
                MenuQuickActionItem(
                    icon = Icons.Default.VisibilityOff,
                    label = "Incognito",
                    onClick = {
                        onDismiss()
                        onMenuAction("NEW_INCOGNITO_TAB")
                    }
                )
                MenuQuickActionItem(
                    icon = Icons.Default.History,
                    label = "History",
                    onClick = {
                        onDismiss()
                        onMenuAction("HISTORY")
                    }
                )
                MenuQuickActionItem(
                    icon = Icons.Default.FileDownload,
                    label = "Downloads",
                    onClick = {
                        onDismiss()
                        onMenuAction("DOWNLOADS")
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Row 2: Bookmarks, Recent Tabs, Find in Page, Desktop Site
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MenuQuickActionItem(
                    icon = Icons.Default.BookmarkBorder,
                    label = "Bookmarks",
                    onClick = {
                        onDismiss()
                        onMenuAction("BOOKMARKS")
                    }
                )
                MenuQuickActionItem(
                    icon = Icons.Default.Tab,
                    label = "Recent Tabs",
                    onClick = {
                        onDismiss()
                        onMenuAction("TAB_SWITCHER")
                    }
                )
                MenuQuickActionItem(
                    icon = Icons.Default.Search,
                    label = "Find in Page",
                    onClick = {
                        onDismiss()
                        onMenuAction("FIND_IN_PAGE")
                    }
                )
                MenuQuickActionItem(
                    icon = Icons.Default.DesktopWindows,
                    label = "Desktop Site",
                    isActive = isDesktopMode,
                    onClick = {
                        onToggleDesktop()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4 Round Feature Tiles: Dark Mode, Ad Block, Data Saver, Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                RoundFeatureButton(
                    icon = Icons.Default.DarkMode,
                    label = "Dark Mode",
                    badgeColor = Color(0xFF1E283C),
                    iconTint = Color(0xFF60A5FA),
                    onClick = {
                        onDismiss()
                        onMenuAction("APPEARANCE")
                    }
                )
                RoundFeatureButton(
                    icon = Icons.Default.Shield,
                    label = "Ad Block",
                    badgeColor = Color(0xFFDC2626),
                    iconTint = Color.White,
                    onClick = {
                        onDismiss()
                        onMenuAction("SHIELD")
                    }
                )
                RoundFeatureButton(
                    icon = Icons.Default.Eco,
                    label = "Data Saver",
                    badgeColor = Color(0xFF16A34A),
                    iconTint = Color.White,
                    onClick = {
                        onDismiss()
                        onMenuAction("SETTINGS")
                    }
                )
                RoundFeatureButton(
                    icon = Icons.Default.Settings,
                    label = "Settings",
                    badgeColor = Color(0xFF1E283C),
                    iconTint = Color(0xFF94A3B8),
                    onClick = {
                        onDismiss()
                        onMenuAction("SETTINGS")
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFF1A2338), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // List Options: Share, Rate App, Help & Feedback
            MenuListRow(
                icon = Icons.Default.Share,
                title = "Share",
                onClick = {
                    onDismiss()
                    onMenuAction("SHARE")
                }
            )
            MenuListRow(
                icon = Icons.Default.StarBorder,
                title = "Rate App",
                onClick = {
                    onDismiss()
                    onMenuAction("RATE_APP")
                }
            )
            MenuListRow(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                title = "Help & Feedback",
                onClick = {
                    onDismiss()
                    onMenuAction("AI_ASSISTANT")
                }
            )
        }
    }
}

@Composable
private fun MenuQuickActionItem(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isActive) Color(0xFF2563EB) else Color(0xFF131B2E)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color.White else Color(0xFF94A3B8),
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun RoundFeatureButton(
    icon: ImageVector,
    label: String,
    badgeColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(badgeColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun MenuListRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = Color(0xFF475569),
            modifier = Modifier.size(14.dp)
        )
    }
}
