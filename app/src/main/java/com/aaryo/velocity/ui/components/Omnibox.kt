package com.aaryo.velocity.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaryo.velocity.model.Tab
import com.aaryo.velocity.ui.theme.GreenSecure
import com.aaryo.velocity.ui.theme.IncognitoPurple
import com.aaryo.velocity.ui.theme.VelocityBlue
import com.aaryo.velocity.ui.theme.VelocityCyan

@Composable
fun Omnibox(
    tab: Tab?,
    tabCount: Int,
    isBookmarked: Boolean,
    onNavigate: (String) -> Unit,
    onReload: () -> Unit,
    onOpenTabSwitcher: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenShield: () -> Unit,
    onOpenSsl: () -> Unit,
    onOpenExtensions: () -> Unit = {},
    onMenuAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val currentUrl = tab?.displayUrl ?: ""

    LaunchedEffect(currentUrl, isEditing) {
        if (!isEditing) {
            inputText = currentUrl
        }
    }

    val isIncognito = tab?.isIncognito == true

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isIncognito) Color(0xFF130E26) else MaterialTheme.colorScheme.surface
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // URL Input Capsule
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .testTag("omnibox_container"),
                color = if (isIncognito) Color(0xFF241B42) else MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Security / SSL lock icon or Search icon
                    if (tab != null && !tab.isNewTab && !isEditing) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable { onOpenSsl() }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (tab.isSecure) Icons.Default.Lock else Icons.Default.Security,
                                contentDescription = if (tab.isSecure) "Connection is secure" else "Connection not secure",
                                tint = if (tab.isSecure) GreenSecure else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isIncognito) IncognitoPurple else VelocityBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Text Input field
                    Box(modifier = Modifier.weight(1f)) {
                        if (inputText.isEmpty() && !isEditing) {
                            Text(
                                text = "Search or type URL",
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged { focusState ->
                                    isEditing = focusState.isFocused
                                }
                                .testTag("omnibox_input"),
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = if (isEditing) FontWeight.Normal else FontWeight.Medium,
                                color = if (isIncognito) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(if (isIncognito) IncognitoPurple else VelocityCyan),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    focusManager.clearFocus()
                                    isEditing = false
                                    onNavigate(inputText)
                                }
                            )
                        )
                    }

                    // Clear button or Reload button
                    if (isEditing && inputText.isNotEmpty()) {
                        IconButton(
                            onClick = { inputText = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear input",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else if (tab != null && !tab.isNewTab) {
                        IconButton(
                            onClick = onReload,
                            modifier = Modifier.size(28.dp).testTag("reload_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload page",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Velocity Shield Badge
                    if (tab != null && tab.adBlockEnabled) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (tab.blockedTrackersCount > 0) VelocityBlue.copy(alpha = 0.2f)
                                    else Color.Transparent
                                )
                                .clickable { onOpenShield() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .testTag("shield_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Velocity Shield",
                                    tint = if (tab.blockedTrackersCount > 0) VelocityCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                if (tab.blockedTrackersCount > 0) {
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${tab.blockedTrackersCount}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VelocityCyan
                                    )
                                }
                            }
                        }
                    }

                    // Active Extensions Badge
                    if (tab != null && !tab.isNewTab && tab.activeExtensionsCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(VelocityCyan.copy(alpha = 0.2f))
                                .clickable { onOpenExtensions() }
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                .testTag("extensions_badge_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Extension,
                                    contentDescription = "Active Extensions",
                                    tint = VelocityCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${tab.activeExtensionsCount}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VelocityCyan
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Bookmark Quick Star
            if (tab != null && !tab.isNewTab) {
                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.size(36.dp).testTag("bookmark_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Tabs Counter Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenTabSwitcher() }
                    .testTag("tab_switcher_button"),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isIncognito) IncognitoPurple else VelocityBlue,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$tabCount",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Overflow Menu
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(36.dp).testTag("overflow_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.width(220.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("New Tab") },
                        onClick = {
                            menuExpanded = false
                            onMenuAction("NEW_TAB")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("New Private Tab") },
                        onClick = {
                            menuExpanded = false
                            onMenuAction("NEW_INCOGNITO_TAB")
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Bookmarks") },
                        onClick = {
                            menuExpanded = false
                            onMenuAction("BOOKMARKS")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("History") },
                        onClick = {
                            menuExpanded = false
                            onMenuAction("HISTORY")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Downloads") },
                        onClick = {
                            menuExpanded = false
                            onMenuAction("DOWNLOADS")
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (tab?.isDesktopMode == true) "✓ Desktop Site" else "Desktop Site"
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onMenuAction("TOGGLE_DESKTOP")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Extensions") },
                        onClick = {
                            menuExpanded = false
                            onMenuAction("EXTENSIONS")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Find in Page") },
                        onClick = {
                            menuExpanded = false
                            onMenuAction("FIND_IN_PAGE")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share Page") },
                        onClick = {
                            menuExpanded = false
                            onMenuAction("SHARE")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Velocity Shield") },
                        onClick = {
                            menuExpanded = false
                            onMenuAction("SHIELD")
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = {
                            menuExpanded = false
                            onMenuAction("SETTINGS")
                        }
                    )
                }
            }
        }

        // Web Loading Progress Bar
        AnimatedVisibility(
            visible = tab?.isLoading == true && tab.progress in 1..99,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LinearProgressIndicator(
                progress = { (tab?.progress ?: 0) / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = if (isIncognito) IncognitoPurple else VelocityCyan,
                trackColor = Color.Transparent
            )
        }
    }
}
