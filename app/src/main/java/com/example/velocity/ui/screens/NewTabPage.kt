package com.example.velocity.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.velocity.model.Bookmark
import com.example.velocity.model.ShieldStats
import com.example.velocity.ui.theme.IncognitoDarkBg
import com.example.velocity.ui.theme.IncognitoPurple
import com.example.velocity.ui.theme.IncognitoSurface
import com.example.velocity.ui.theme.VelocityBlue
import com.example.velocity.ui.theme.VelocityCardDark
import com.example.velocity.ui.theme.VelocityCyan
import com.example.velocity.ui.theme.VelocitySlate

data class SpeedDialItem(
    val title: String,
    val url: String,
    val domain: String,
    val iconColor: Color
)

private val DEFAULT_SPEED_DIALS = listOf(
    SpeedDialItem("DuckDuckGo", "https://duckduckgo.com", "duckduckgo.com", Color(0xFFDE5833)),
    SpeedDialItem("GitHub", "https://github.com", "github.com", Color(0xFF6E40C9)),
    SpeedDialItem("Wikipedia", "https://en.wikipedia.org", "wikipedia.org", Color(0xFF3366CC)),
    SpeedDialItem("Hacker News", "https://news.ycombinator.com", "ycombinator.com", Color(0xFFFF6600)),
    SpeedDialItem("Reddit", "https://reddit.com", "reddit.com", Color(0xFFFF4500)),
    SpeedDialItem("YouTube", "https://youtube.com", "youtube.com", Color(0xFFFF0000)),
    SpeedDialItem("X / Twitter", "https://twitter.com", "twitter.com", Color(0xFF1DA1F2)),
    SpeedDialItem("AI Studio", "https://aistudio.google.com", "google.com", Color(0xFF00E5FF))
)

@Composable
fun NewTabPage(
    isIncognito: Boolean,
    shieldStats: ShieldStats,
    onNavigate: (String) -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenShield: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val backgroundBrush = if (isIncognito) {
        Brush.verticalGradient(
            colors = listOf(IncognitoDarkBg, IncognitoSurface, Color(0xFF0D0A1A))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                MaterialTheme.colorScheme.background
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Velocity Branding Header
            Surface(
                shape = CircleShape,
                color = if (isIncognito) IncognitoPurple.copy(alpha = 0.2f) else VelocityCyan.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isIncognito) IncognitoPurple.copy(alpha = 0.4f) else VelocityCyan.copy(alpha = 0.4f)
                ),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isIncognito) Icons.Default.Lock else Icons.Default.Speed,
                        contentDescription = "Velocity Logo",
                        tint = if (isIncognito) IncognitoPurple else VelocityCyan,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isIncognito) "VELOCITY PRIVATE" else "VELOCITY",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                color = if (isIncognito) IncognitoPurple else VelocityBlue
            )

            Text(
                text = if (isIncognito) "Incognito session: no history, isolated storage" else "Supersonic, private native web experience",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Speed Dial Grid
            Text(
                text = "Favorites & Speed Dial",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = false
            ) {
                items(DEFAULT_SPEED_DIALS) { item ->
                    SpeedDialTile(
                        item = item,
                        onClick = { onNavigate(item.url) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Velocity Shield Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenShield() }
                    .testTag("shield_status_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isIncognito) Color(0xFF1E1638) else MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = VelocityCyan.copy(alpha = 0.2f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Shield",
                                tint = VelocityCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Velocity Shield Active",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${shieldStats.totalAdsBlocked} ads & trackers blocked • Fast page loads",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Privacy details",
                        tint = VelocityCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Access Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickActionButton(
                    icon = Icons.Default.Bookmark,
                    label = "Bookmarks",
                    onClick = onOpenBookmarks,
                    modifier = Modifier.weight(1f).testTag("quick_bookmarks_button")
                )
                Spacer(modifier = Modifier.width(8.dp))
                QuickActionButton(
                    icon = Icons.Default.History,
                    label = "History",
                    onClick = onOpenHistory,
                    modifier = Modifier.weight(1f).testTag("quick_history_button")
                )
                Spacer(modifier = Modifier.width(8.dp))
                QuickActionButton(
                    icon = Icons.Default.Download,
                    label = "Downloads",
                    onClick = onOpenDownloads,
                    modifier = Modifier.weight(1f).testTag("quick_downloads_button")
                )
                Spacer(modifier = Modifier.width(8.dp))
                QuickActionButton(
                    icon = Icons.Default.Settings,
                    label = "Settings",
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f).testTag("quick_settings_button")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SpeedDialTile(
    item: SpeedDialItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .testTag("speed_dial_${item.title.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = item.iconColor.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(1.dp, item.iconColor.copy(alpha = 0.3f)),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = item.title.take(1),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = item.iconColor
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.title,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
