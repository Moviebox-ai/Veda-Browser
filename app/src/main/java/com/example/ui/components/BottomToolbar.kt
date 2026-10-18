package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrivateModePurple

@Composable
fun BottomToolbar(
    isHomeScreen: Boolean = false,
    canGoBack: Boolean,
    canGoForward: Boolean,
    tabCount: Int,
    isPrivate: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onOpenBookmarks: () -> Unit = {},
    onOpenDownloads: () -> Unit = {},
    onOpenTabs: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isHomeScreen) {
                // PRD Section 4 Primary Navigation: Home, Bookmarks, Tabs, Downloads, More
                // 1. Home (Active)
                IconButton(
                    onClick = onHome,
                    modifier = Modifier.size(48.dp).testTag("nav_home_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // 2. Bookmarks
                IconButton(
                    onClick = onOpenBookmarks,
                    modifier = Modifier.size(48.dp).testTag("nav_bookmarks_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Bookmarks",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 3. Tabs (with live badge)
                IconButton(
                    onClick = onOpenTabs,
                    modifier = Modifier.size(48.dp).testTag("nav_tabs_button")
                ) {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = if (isPrivate) PrivateModePurple else MaterialTheme.colorScheme.primary
                            ) {
                                Text(text = tabCount.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 4. Downloads
                IconButton(
                    onClick = onOpenDownloads,
                    modifier = Modifier.size(48.dp).testTag("nav_downloads_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Downloads",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 5. More Menu
                IconButton(
                    onClick = onOpenMenu,
                    modifier = Modifier.size(48.dp).testTag("nav_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // In-Page Web Navigation Bar: Back, Forward, Home, Tabs, Menu
                // 1. Back
                IconButton(
                    onClick = onBack,
                    enabled = canGoBack,
                    modifier = Modifier.size(48.dp).testTag("toolbar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }

                // 2. Forward
                IconButton(
                    onClick = onForward,
                    enabled = canGoForward,
                    modifier = Modifier.size(48.dp).testTag("toolbar_forward_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward",
                        tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }

                // 3. Home
                IconButton(
                    onClick = onHome,
                    modifier = Modifier.size(48.dp).testTag("toolbar_home_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 4. Tabs
                IconButton(
                    onClick = onOpenTabs,
                    modifier = Modifier.size(48.dp).testTag("toolbar_tabs_button")
                ) {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = if (isPrivate) PrivateModePurple else MaterialTheme.colorScheme.primary
                            ) {
                                Text(text = tabCount.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 5. Menu
                IconButton(
                    onClick = onOpenMenu,
                    modifier = Modifier.size(48.dp).testTag("toolbar_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
