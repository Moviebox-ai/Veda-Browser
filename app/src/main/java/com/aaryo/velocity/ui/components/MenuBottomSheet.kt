package com.aaryo.velocity.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaryo.velocity.model.Tab
import com.aaryo.velocity.ui.theme.IncognitoPurple
import com.aaryo.velocity.ui.theme.VelocityBlue
import com.aaryo.velocity.ui.theme.VelocityCyan

/**
 * Modern Right-Side Floating Overlay Menu
 * Renders directly as an overlay on top of the home screen / webpage,
 * keeping the background screen clearly visible through a soft transparent scrim.
 */
@Composable
fun VelocityMenuSheet(
    tab: Tab?,
    isDesktopMode: Boolean,
    tabCount: Int = 1,
    isBookmarked: Boolean = false,
    onToggleDesktop: () -> Unit,
    onMenuAction: (String) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler {
        onDismiss()
    }

    val transitionState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }

    // Full screen overlay with very light scrim so the home screen/page is clearly visible
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.TopEnd
    ) {
        // Soft backdrop scrim: keeps background home screen vividly visible
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.22f))
        )

        // Floating Right-Side List Overlay Card
        AnimatedVisibility(
            visibleState = transitionState,
            enter = fadeIn(animationSpec = tween(180)) +
                    scaleIn(
                        initialScale = 0.88f,
                        transformOrigin = TransformOrigin(0.95f, 0.05f),
                        animationSpec = tween(200, easing = FastOutSlowInEasing)
                    ) +
                    slideInHorizontally(
                        initialOffsetX = { it / 3 },
                        animationSpec = tween(200, easing = FastOutSlowInEasing)
                    ),
            exit = fadeOut(animationSpec = tween(140)) +
                    scaleOut(
                        targetScale = 0.88f,
                        transformOrigin = TransformOrigin(0.95f, 0.05f),
                        animationSpec = tween(160, easing = FastOutSlowInEasing)
                    )
        ) {
            Surface(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 4.dp, end = 10.dp, bottom = 16.dp)
                    .widthIn(min = 265.dp, max = 295.dp)
                    .heightIn(max = 620.dp)
                    .navigationBarsPadding()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume clicks inside the floating menu
                    )
                    .testTag("right_side_menu_overlay"),
                color = Color(0xF70C1222), // 97% opaque deep glass dark slate
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66334155)),
                shadowElevation = 18.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header: Title & Close Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            VelocityLogoIcon(size = 22.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Menu",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("menu_overlay_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Quick Action Bar (Forward, Reload, Bookmark, Downloads)
                    Surface(
                        color = Color(0xFF131B2E),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2D4A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Forward
                            IconButton(
                                onClick = {
                                    onDismiss()
                                    onMenuAction("FORWARD")
                                },
                                enabled = tab?.canGoForward == true,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Forward",
                                    tint = if (tab?.canGoForward == true) Color.White else Color(0xFF475569),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Reload
                            IconButton(
                                onClick = {
                                    onDismiss()
                                    onMenuAction("RELOAD")
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reload",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Bookmark
                            IconButton(
                                onClick = {
                                    onMenuAction("TOGGLE_BOOKMARK")
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Bookmark",
                                    tint = if (isBookmarked) Color(0xFFF59E0B) else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Downloads
                            IconButton(
                                onClick = {
                                    onDismiss()
                                    onMenuAction("DOWNLOADS")
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Downloads",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Scrollable Vertical List of Menu Options
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 4.dp)
                    ) {
                        // Section: Tabs
                        MenuItemRow(
                            icon = Icons.Default.Add,
                            title = "New Tab",
                            onClick = {
                                onDismiss()
                                onMenuAction("NEW_TAB")
                            }
                        )
                        MenuItemRow(
                            icon = Icons.Default.VisibilityOff,
                            title = "New Private Tab",
                            iconTint = IncognitoPurple,
                            badge = "Private",
                            onClick = {
                                onDismiss()
                                onMenuAction("NEW_INCOGNITO_TAB")
                            }
                        )
                        MenuItemRow(
                            icon = Icons.Default.Tab,
                            title = "Recent Tabs",
                            badge = if (tabCount > 1) "$tabCount tabs" else "1 tab",
                            onClick = {
                                onDismiss()
                                onMenuAction("TAB_SWITCHER")
                            }
                        )

                        HorizontalDivider(
                            color = Color(0xFF1E283C),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // Section: Library
                        MenuItemRow(
                            icon = Icons.Default.BookmarkBorder,
                            title = "Bookmarks",
                            onClick = {
                                onDismiss()
                                onMenuAction("BOOKMARKS")
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
                            icon = Icons.Default.FileDownload,
                            title = "Downloads",
                            onClick = {
                                onDismiss()
                                onMenuAction("DOWNLOADS")
                            }
                        )
                        MenuItemRow(
                            icon = Icons.Default.Lock,
                            title = "Passwords",
                            onClick = {
                                onDismiss()
                                onMenuAction("PASSWORDS")
                            }
                        )

                        HorizontalDivider(
                            color = Color(0xFF1E283C),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // Section: Page Tools
                        MenuItemRow(
                            icon = Icons.Default.DesktopWindows,
                            title = "Desktop Site",
                            trailing = {
                                Switch(
                                    checked = isDesktopMode,
                                    onCheckedChange = { onToggleDesktop() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = VelocityBlue,
                                        uncheckedThumbColor = Color(0xFF94A3B8),
                                        uncheckedTrackColor = Color(0xFF1E293B)
                                    ),
                                    modifier = Modifier.scale(0.78f)
                                )
                            },
                            onClick = { onToggleDesktop() }
                        )
                        MenuItemRow(
                            icon = Icons.Default.Search,
                            title = "Find in Page",
                            onClick = {
                                onDismiss()
                                onMenuAction("FIND_IN_PAGE")
                            }
                        )
                        MenuItemRow(
                            icon = Icons.Default.Translate,
                            title = "Translate Page",
                            onClick = {
                                onDismiss()
                                onMenuAction("TRANSLATE")
                            }
                        )
                        MenuItemRow(
                            icon = Icons.Default.Share,
                            title = "Share",
                            onClick = {
                                onDismiss()
                                onMenuAction("SHARE")
                            }
                        )
                        MenuItemRow(
                            icon = Icons.Default.Bookmark,
                            title = "Add to Home Screen",
                            onClick = {
                                onDismiss()
                                onMenuAction("ADD_HOME_SCREEN")
                            }
                        )

                        HorizontalDivider(
                            color = Color(0xFF1E283C),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // Section: Privacy & Smart Features
                        MenuItemRow(
                            icon = Icons.Default.Shield,
                            title = "Velocity Shield",
                            iconTint = VelocityCyan,
                            badge = "Protected",
                            onClick = {
                                onDismiss()
                                onMenuAction("SHIELD")
                            }
                        )
                        MenuItemRow(
                            icon = Icons.Default.AutoAwesome,
                            title = "AI Assistant",
                            iconTint = VelocityCyan,
                            badge = "Smart",
                            onClick = {
                                onDismiss()
                                onMenuAction("AI_ASSISTANT")
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

                        HorizontalDivider(
                            color = Color(0xFF1E283C),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // Section: Settings & About
                        MenuItemRow(
                            icon = Icons.Default.Palette,
                            title = "Appearance & Themes",
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
                        MenuItemRow(
                            icon = Icons.Default.StarBorder,
                            title = "Rate App",
                            onClick = {
                                onDismiss()
                                onMenuAction("RATE_APP")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItemRow(
    icon: ImageVector,
    title: String,
    iconTint: Color = Color(0xFF94A3B8),
    badge: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                color = Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = iconTint.copy(alpha = 0.18f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, iconTint.copy(alpha = 0.35f)),
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    Text(
                        text = badge,
                        color = iconTint,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (trailing != null) {
                trailing()
            }
        }
    }
}
