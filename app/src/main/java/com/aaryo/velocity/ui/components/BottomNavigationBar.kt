package com.aaryo.velocity.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
 * Bottom Navigation Bar
 * - On Home Page (NewTab): 4 items: Home, Tabs, Downloads, Menu (Matching Screen 2)
 * - On Web Page: 5 items: Back, Forward, Home, Tabs count, More menu (Matching Screen 7)
 */
@Composable
fun BottomNavigationBar(
    tab: Tab?,
    tabCount: Int,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onGoHome: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenDownloads: () -> Unit = {},
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNewTab = tab == null || tab.isNewTab
    val isIncognito = tab?.isIncognito == true

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = if (isIncognito) Color(0xFF090A14) else Color(0xFF070B19),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF161F36))
    ) {
        if (isNewTab) {
            // Screen 2 & 6: Home Page Bottom Bar (Home, Tabs, Downloads, Menu)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Home
                HomeNavButton(
                    icon = Icons.Filled.Home,
                    label = "Home",
                    isActive = true,
                    onClick = onGoHome,
                    modifier = Modifier.testTag("nav_home_button")
                )

                // 2. Tabs
                TabNavButton(
                    tabCount = tabCount,
                    isActive = false,
                    onClick = onOpenTabs,
                    modifier = Modifier.testTag("nav_tabs_button")
                )

                // 3. Downloads
                HomeNavButton(
                    icon = Icons.Filled.FileDownload,
                    label = "Downloads",
                    isActive = false,
                    onClick = onOpenDownloads,
                    modifier = Modifier.testTag("nav_downloads_button")
                )

                // 4. Menu
                HomeNavButton(
                    icon = Icons.Filled.Menu,
                    label = "Menu",
                    isActive = false,
                    onClick = onOpenMenu,
                    modifier = Modifier.testTag("nav_menu_button")
                )
            }
        } else {
            // Screen 7: Web Page View Bottom Bar (Back, Forward, Home, Tabs [3], More ⋮)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back
                IconButton(
                    onClick = onGoBack,
                    enabled = tab?.canGoBack == true,
                    modifier = Modifier.testTag("nav_web_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (tab?.canGoBack == true) Color.White else Color(0xFF475569),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Forward
                IconButton(
                    onClick = onGoForward,
                    enabled = tab?.canGoForward == true,
                    modifier = Modifier.testTag("nav_web_forward")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward",
                        tint = if (tab?.canGoForward == true) Color.White else Color(0xFF475569),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Home
                IconButton(
                    onClick = onGoHome,
                    modifier = Modifier.testTag("nav_web_home")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Home,
                        contentDescription = "Home",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Tab Count Badge (e.g. [3])
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onOpenTabs() }
                        .padding(6.dp)
                        .testTag("nav_web_tabs"),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White),
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = tabCount.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // More Menu ⋮
                IconButton(
                    onClick = onOpenMenu,
                    modifier = Modifier.testTag("nav_web_more")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeNavButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) Color(0xFF3B82F6) else Color(0xFF94A3B8),
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = if (isActive) Color(0xFF3B82F6) else Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun TabNavButton(
    tabCount: Int,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (isActive) Color(0xFF3B82F6) else Color(0xFF94A3B8)
            ),
            modifier = Modifier.size(20.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = tabCount.toString(),
                    color = if (isActive) Color(0xFF3B82F6) else Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text(
            text = "Tabs",
            color = if (isActive) Color(0xFF3B82F6) else Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
