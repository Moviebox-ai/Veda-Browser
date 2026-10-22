package com.aaryo.vedabrowser.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaryo.vedabrowser.model.Tab as VelocityTab
import com.aaryo.vedabrowser.ui.theme.VelocityBlue

/**
 * Screen 3: Tab View
 * Exact match to the reference mockup:
 * - Header: "Tabs" on left, "+" and "⋮" on right
 * - Tab cards in dark container with site favicon on left, title in white, URL in blue, close "✕" on right
 * - Floating wide glowing gradient pill button: "+ New Tab"
 */
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

    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B19))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
        ) {
            // Header: "Tabs" on left, "+" and "⋮" on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tabs",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            onCreateTab(false)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("tab_view_add_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Tab",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.testTag("tab_view_menu_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Tab options",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Color(0xFF131926))
                        ) {
                            DropdownMenuItem(
                                text = { Text("New Incognito Tab", color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    onCreateTab(true)
                                    onDismiss()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Close All Tabs", color = Color(0xFFEF4444)) },
                                onClick = {
                                    showMenu = false
                                    onCloseAll(false)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }

            val displayTabs = remember(tabs) {
                val nonNewTabs = tabs.filterNot { it.isNewTab }
                if (nonNewTabs.isNotEmpty()) nonNewTabs else tabs
            }

            // Tab Cards List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayTabs, key = { it.id }) { tab ->
                    TabCardItem(
                        tab = tab,
                        isSelected = tab.id == activeTabId,
                        onSelect = {
                            onSelectTab(tab.id)
                            onDismiss()
                        },
                        onClose = {
                            onCloseTab(tab.id)
                        }
                    )
                }
            }
        }

        // Bottom Button: + New Tab (Wide Glowing Gradient Pill Button)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clickable {
                        onCreateTab(false)
                        onDismiss()
                    }
                    .testTag("tab_view_bottom_new_tab_btn")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF2563EB),
                                    Color(0xFF3B82F6),
                                    Color(0xFF4F46E5)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "New Tab",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual Tab Card matching Screen 3:
 * - Rounded dark container
 * - Site logo inside rounded square box
 * - Title in bold white
 * - URL in electric blue (#3B82F6)
 * - Close button '✕' on right
 */
@Composable
private fun TabCardItem(
    tab: VelocityTab,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    val isYouTube = tab.url.contains("youtube", ignoreCase = true)
    val isNews = tab.url.contains("news", ignoreCase = true)
    val isOpenAi = tab.url.contains("openai", ignoreCase = true) || tab.url.contains("chatgpt", ignoreCase = true)
    val isGoogle = tab.url.contains("google", ignoreCase = true) && !isNews && !isYouTube

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF111827),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isYouTube) 1.5.dp else if (isSelected) 1.dp else 1.dp,
            color = if (isYouTube) Color(0xFFEF4444) else if (isSelected) Color(0xFF3B82F6) else Color(0xFF1E283C)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Site Favicon Box (rounded square)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            isYouTube -> Color(0xFFE50914)
                            isGoogle -> Color.White
                            isOpenAi -> Color.White
                            isNews -> Color(0xFF1E293B)
                            else -> Color(0xFF1E293B)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isYouTube -> {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "YouTube",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    isGoogle -> {
                        Canvas(modifier = Modifier.size(22.dp)) {
                            val w = size.width
                            val h = size.height
                            drawLine(
                                color = Color(0xFF4285F4),
                                start = Offset(w * 0.45f, h * 0.5f),
                                end = Offset(w * 0.95f, h * 0.5f),
                                strokeWidth = w * 0.22f
                            )
                            drawArc(
                                color = Color(0xFF4285F4),
                                startAngle = -45f,
                                sweepAngle = 45f,
                                useCenter = false,
                                style = Stroke(width = w * 0.22f)
                            )
                            drawArc(
                                color = Color(0xFFEA4335),
                                startAngle = 180f,
                                sweepAngle = 90f,
                                useCenter = false,
                                style = Stroke(width = w * 0.22f)
                            )
                            drawArc(
                                color = Color(0xFFFBBC05),
                                startAngle = 90f,
                                sweepAngle = 90f,
                                useCenter = false,
                                style = Stroke(width = w * 0.22f)
                            )
                            drawArc(
                                color = Color(0xFF34A853),
                                startAngle = 0f,
                                sweepAngle = 90f,
                                useCenter = false,
                                style = Stroke(width = w * 0.22f)
                            )
                        }
                    }
                    isNews -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White)
                                .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Newspaper,
                                contentDescription = "News",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    isOpenAi -> {
                        Canvas(modifier = Modifier.size(24.dp)) {
                            val w = size.width
                            val h = size.height
                            for (i in 0 until 6) {
                                val angle = Math.toRadians(i * 60.0)
                                val r = w * 0.24f
                                val cx = w * 0.5f + (Math.cos(angle) * r).toFloat()
                                val cy = h * 0.5f + (Math.sin(angle) * r).toFloat()
                                drawCircle(
                                    color = Color.Black,
                                    radius = r * 0.55f,
                                    center = Offset(cx, cy),
                                    style = Stroke(width = 1.8.dp.toPx())
                                )
                            }
                        }
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Title & URL
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (tab.title.isNotBlank()) tab.title else if (tab.isNewTab) "Velocity Start" else "Web Page",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (tab.isNewTab) "velocity://newtab" else tab.url,
                    color = Color(0xFF3B82F6),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Close Button '✕'
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("close_tab_item_${tab.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Tab",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
