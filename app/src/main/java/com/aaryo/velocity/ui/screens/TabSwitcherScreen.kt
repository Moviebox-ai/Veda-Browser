package com.aaryo.velocity.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaryo.velocity.model.Tab as VelocityTab
import com.aaryo.velocity.ui.theme.IncognitoDarkBg
import com.aaryo.velocity.ui.theme.IncognitoPurple
import com.aaryo.velocity.ui.theme.VelocityBlue
import com.aaryo.velocity.ui.theme.VelocityCyan
import com.aaryo.velocity.ui.theme.VelocityPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabSwitcherScreen(
    tabs: List<VelocityTab>,
    activeTabId: String,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onCreateTab: (Boolean) -> Unit,
    onCloseAll: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onDismiss()
    }

    var selectedTabIndex by remember {
        mutableIntStateOf(if (tabs.find { it.id == activeTabId }?.isIncognito == true) 1 else 0)
    }

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    val regularTabs = tabs.filterNot { it.isIncognito }
    val incognitoTabs = tabs.filter { it.isIncognito }
    val currentTabList = if (selectedTabIndex == 1) incognitoTabs else regularTabs
    val isIncognitoMode = selectedTabIndex == 1

    val filteredTabs = if (searchQuery.isBlank()) {
        currentTabList
    } else {
        currentTabList.filter {
            it.title.contains(searchQuery, ignoreCase = true) || it.url.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = if (isIncognitoMode) IncognitoDarkBg else Color(0xFF080B11),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // Header matching Mobile 2 in Mockup:
            // "Tabs", "X Tabs", Search icon, 3-dots menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tabs",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${currentTabList.size} Tabs",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isSearchActive = !isSearchActive },
                        modifier = Modifier.testTag("tab_search_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Tabs",
                            tint = Color.White
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.testTag("tab_more_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Color(0xFF131926))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Close All Tabs", color = Color(0xFFEF4444)) },
                                onClick = {
                                    showMenu = false
                                    onCloseAll(isIncognitoMode)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("New Private Tab", color = IncognitoPurple) },
                                onClick = {
                                    showMenu = false
                                    onCreateTab(true)
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("tab_switcher_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // Search filter field
            AnimatedVisibility(visible = isSearchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search open tabs...", color = Color(0xFF64748B)) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // Standard vs Incognito tabs mode selector
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = if (isIncognitoMode) IncognitoPurple else VelocityBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = if (isIncognitoMode) IncognitoPurple else VelocityBlue,
                        height = 3.dp
                    )
                },
                divider = {}
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = "Standard (${regularTabs.size})",
                            color = if (selectedTabIndex == 0) Color.White else Color(0xFF94A3B8),
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = "Private (${incognitoTabs.size})",
                            color = if (selectedTabIndex == 1) IncognitoPurple else Color(0xFF94A3B8),
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2-Column Tabs Grid matching Mobile 2 in Mockup
            Box(modifier = Modifier.weight(1f)) {
                if (filteredTabs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (isIncognitoMode) Icons.Default.Security else Icons.Default.Public,
                                contentDescription = "Empty",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isIncognitoMode) "No private tabs open" else "No open tabs",
                                color = Color(0xFF94A3B8),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredTabs, key = { it.id }) { tab ->
                            val isSelected = tab.id == activeTabId

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.85f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        onSelectTab(tab.id)
                                        onDismiss()
                                    }
                                    .then(
                                        if (isSelected) Modifier.border(
                                            2.dp,
                                            if (isIncognitoMode) IncognitoPurple else VelocityBlue,
                                            RoundedCornerShape(16.dp)
                                        )
                                        else Modifier.border(
                                            1.dp,
                                            Color(0x3326354D),
                                            RoundedCornerShape(16.dp)
                                        )
                                    )
                                    .testTag("tab_item_${tab.id}"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isIncognitoMode) Color(0xFF150F28) else Color(0xFF131926)
                                )
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Tab Header: Title + Close button
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            tab.favicon?.let { bmp ->
                                                Image(
                                                    bitmap = bmp.asImageBitmap(),
                                                    contentDescription = "Favicon",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }

                                            Text(
                                                text = tab.title.ifBlank { "New Tab" },
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        IconButton(
                                            onClick = { onCloseTab(tab.id) },
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close tab",
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    // URL line
                                    Text(
                                        text = tab.url.removePrefix("https://").removePrefix("http://"),
                                        color = Color(0xFF64748B),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                    )

                                    // Snapshot Preview Body (matching Mockup)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(6.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF090D15)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (tab.screenshot != null) {
                                            Image(
                                                bitmap = tab.screenshot.asImageBitmap(),
                                                contentDescription = "Tab preview",
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = tab.title.take(1).uppercase(),
                                                    color = VelocityCyan,
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = if (tab.isNewTab) "Velocity Start" else tab.title,
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 10.sp,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Bar: Floating circular "+" in center, "Close All" on the right (Matches Mobile 2!)
            Surface(
                color = Color(0xFF080B11),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2238BDF8)),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left spacer
                    Spacer(modifier = Modifier.width(60.dp))

                    // Glowing "+" FAB in Center
                    FloatingActionButton(
                        onClick = {
                            onCreateTab(isIncognitoMode)
                            onDismiss()
                        },
                        containerColor = if (isIncognitoMode) IncognitoPurple else VelocityBlue,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("tab_new_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Tab",
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // "Close All" on Right
                    TextButton(
                        onClick = { onCloseAll(isIncognitoMode) },
                        modifier = Modifier.testTag("tab_close_all_btn")
                    ) {
                        Text(
                            text = "Close All",
                            color = Color(0xFFEF4444),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
