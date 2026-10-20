package com.aaryo.velocity.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaryo.velocity.model.BrowserSettings
import com.aaryo.velocity.model.ShieldStats
import com.aaryo.velocity.ui.components.VelocityLogoIcon
import com.aaryo.velocity.ui.components.WallpaperCanvas
import com.aaryo.velocity.ui.theme.BrandAmazon
import com.aaryo.velocity.ui.theme.BrandChatGPT
import com.aaryo.velocity.ui.theme.BrandFacebook
import com.aaryo.velocity.ui.theme.BrandFlipkart
import com.aaryo.velocity.ui.theme.BrandGitHub
import com.aaryo.velocity.ui.theme.BrandGmail
import com.aaryo.velocity.ui.theme.BrandGoogleBlue
import com.aaryo.velocity.ui.theme.BrandGoogleGreen
import com.aaryo.velocity.ui.theme.BrandGoogleRed
import com.aaryo.velocity.ui.theme.BrandGoogleYellow
import com.aaryo.velocity.ui.theme.BrandInstagramEnd
import com.aaryo.velocity.ui.theme.BrandInstagramStart
import com.aaryo.velocity.ui.theme.BrandYouTube
import com.aaryo.velocity.ui.theme.IncognitoDarkBg
import com.aaryo.velocity.ui.theme.IncognitoPurple
import com.aaryo.velocity.ui.theme.VelocityBlue
import com.aaryo.velocity.ui.theme.VelocityCyan
import com.aaryo.velocity.ui.theme.VelocityPurple

import com.aaryo.velocity.model.QuickDialShortcut

data class DiscoverArticle(
    val id: String,
    val title: String,
    val category: String,
    val source: String,
    val timeAgo: String,
    val url: String,
    val gradientColors: List<Color>
)

@Composable
fun NewTabPage(
    isIncognito: Boolean,
    shieldStats: ShieldStats,
    settings: BrowserSettings = BrowserSettings(),
    quickDials: List<QuickDialShortcut> = emptyList(),
    onAddQuickDial: (QuickDialShortcut) -> Unit = {},
    onDeleteQuickDial: (String) -> Unit = {},
    onNavigate: (String) -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenShield: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var searchInput by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    var selectedDiscoverCategory by remember { mutableIntStateOf(0) }
    val discoverCategories = listOf("All", "News", "Technology", "Business", "Sports", "Entertainment")

    val articles = remember {
        listOf(
            DiscoverArticle(
                id = "1",
                title = "The future of web browsing with on-device AI and enhanced privacy",
                category = "Technology",
                source = "Velocity Tech",
                timeAgo = "1h ago",
                url = "https://blog.google/technology/ai/",
                gradientColors = listOf(Color(0xFF1E3A8A), Color(0xFF6B21A8), Color(0xFF0F172A))
            ),
            DiscoverArticle(
                id = "2",
                title = "Global summits announce new cyber defense frameworks for 2026",
                category = "News",
                source = "Reuters",
                timeAgo = "3h ago",
                url = "https://www.reuters.com",
                gradientColors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF334155))
            ),
            DiscoverArticle(
                id = "3",
                title = "Quantum computing chips make massive leap in energy efficiency",
                category = "Technology",
                source = "TechCrunch",
                timeAgo = "4h ago",
                url = "https://techcrunch.com",
                gradientColors = listOf(Color(0xFF701A75), Color(0xFF9333EA), Color(0xFF1E1B4B))
            ),
            DiscoverArticle(
                id = "4",
                title = "Global markets surge as renewable tech investments hit record high",
                category = "Business",
                source = "Bloomberg",
                timeAgo = "6h ago",
                url = "https://www.bloomberg.com",
                gradientColors = listOf(Color(0xFF9A3412), Color(0xFFC2410C), Color(0xFF431407))
            ),
            DiscoverArticle(
                id = "5",
                title = "Champions League thriller ends in dramatic penalty shootout victory",
                category = "Sports",
                source = "ESPN",
                timeAgo = "8h ago",
                url = "https://www.espn.com",
                gradientColors = listOf(Color(0xFF14532D), Color(0xFF15803D), Color(0xFF052E16))
            ),
            DiscoverArticle(
                id = "6",
                title = "New sci-fi blockbuster breaks global box office records opening weekend",
                category = "Entertainment",
                source = "Variety",
                timeAgo = "10h ago",
                url = "https://variety.com",
                gradientColors = listOf(Color(0xFF831843), Color(0xFFDB2777), Color(0xFF500724))
            ),
            DiscoverArticle(
                id = "7",
                title = "Top 10 productivity and automation tools transforming work in 2026",
                category = "Business",
                source = "Wired",
                timeAgo = "1d ago",
                url = "https://wired.com",
                gradientColors = listOf(Color(0xFF065F46), Color(0xFF0284C7), Color(0xFF064E3B))
            )
        )
    }

    val filteredArticles = remember(selectedDiscoverCategory, articles) {
        if (selectedDiscoverCategory == 0) articles
        else {
            val selectedCat = discoverCategories[selectedDiscoverCategory]
            articles.filter { it.category.equals(selectedCat, ignoreCase = true) }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Wallpaper Canvas Background (Mountains by default, or user setting)
        if (isIncognito) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(IncognitoDarkBg, Color(0xFF150E28), Color(0xFF0A0714))
                        )
                    )
            )
        } else {
            WallpaperCanvas(
                theme = settings.wallpaperTheme,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Main Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Weather / Time & Settings Icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Weather / Location Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x660F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3338BDF8))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = "Weather",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "28°C",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "New Delhi",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                // Settings button on top right
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0x660F172A), CircleShape)
                        .border(1.dp, Color(0x3338BDF8), CircleShape)
                        .testTag("home_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Central Velocity Branding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Curved "V" logo
                VelocityLogoIcon(size = 56.dp)

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isIncognito) "Velocity Private" else "Velocity",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isIncognito) "Isolated Session • No History • Shield Max" else "Fast  •  Secure  •  Smart",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    color = if (isIncognito) IncognitoPurple else Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Modern Pill Search Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .testTag("home_search_bar"),
                color = Color(0xE6111827),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x443B82F6)),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Google 4-color G or Search icon
                    GoogleGLogo(modifier = Modifier.size(22.dp))

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (searchInput.isEmpty()) {
                            Text(
                                text = "Search the web...",
                                color = Color(0xFF94A3B8),
                                fontSize = 15.sp
                            )
                        }
                        BasicTextField(
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(VelocityCyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_search_input")
                        )
                    }

                    if (searchInput.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val query = searchInput.trim()
                                if (query.isNotEmpty()) {
                                    onNavigate(query)
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Go",
                                tint = VelocityCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Shortcuts Grid (4 Columns, matching mockup)
            val gridRows = ((quickDials.size + 1) + 3) / 4
            val gridHeight = (gridRows * 80).coerceAtLeast(160).dp

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeight)
                    .widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = false
            ) {
                items(quickDials, key = { it.id }) { item ->
                    QuickDialItemTile(
                        item = item,
                        onClick = { onNavigate(item.url) }
                    )
                }

                // Add button tile
                item {
                    Column(
                        modifier = Modifier
                            .clickable { showAddDialog = true }
                            .padding(4.dp)
                            .testTag("add_shortcut_btn"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0x331E293B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3394A3B8)),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add shortcut",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Velocity Shield Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .clickable { onOpenShield() }
                    .testTag("shield_status_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isIncognito) Color(0xCC1A1032) else Color(0xCC0E1726)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2238BDF8))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = VelocityCyan.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Shield",
                                tint = VelocityCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Velocity Shield Active",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = "${shieldStats.totalAdsBlocked} ads & trackers blocked",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Details",
                        tint = VelocityCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Discover Feed Section (from mockup)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Discover",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "See all",
                    color = VelocityCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigate("https://news.google.com") }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Pills
            val categoryScroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .horizontalScroll(categoryScroll),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                discoverCategories.forEachIndexed { index, cat ->
                    val isSelected = selectedDiscoverCategory == index
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) VelocityBlue else Color(0x331E293B),
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0x2294A3B8)),
                        modifier = Modifier.clickable { selectedDiscoverCategory = index }
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Discover Articles Cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredArticles.forEach { article ->
                    DiscoverArticleCard(
                        article = article,
                        onClick = { onNavigate(article.url) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Add Shortcut Dialog
    if (showAddDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newUrl by remember { mutableStateOf("https://") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Shortcut", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newUrl,
                        onValueChange = { newUrl = it },
                        label = { Text("URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTitle.isNotBlank() && newUrl.isNotBlank()) {
                            onAddQuickDial(
                                QuickDialShortcut(
                                    id = System.currentTimeMillis().toString(),
                                    title = newTitle.trim(),
                                    url = if (newUrl.startsWith("http")) newUrl.trim() else "https://${newUrl.trim()}",
                                    brandType = "CUSTOM"
                                )
                            )
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Add", color = VelocityCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF131926)
        )
    }
}

/**
 * Renders an exact shortcut icon with proper branding from the mockup.
 */
@Composable
private fun QuickDialItemTile(
    item: QuickDialShortcut,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
            .testTag("quick_dial_${item.id}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF131926),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3326354D)),
            shadowElevation = 4.dp,
            modifier = Modifier.size(54.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                when (item.brandType) {
                    "YOUTUBE" -> YouTubeLogo(modifier = Modifier.size(32.dp))
                    "GOOGLE" -> GoogleGLogo(modifier = Modifier.size(26.dp))
                    "GMAIL" -> GmailLogo(modifier = Modifier.size(26.dp))
                    "CHATGPT" -> ChatGPTLogo(modifier = Modifier.size(28.dp))
                    "GITHUB" -> GitHubLogo(modifier = Modifier.size(26.dp))
                    "FACEBOOK" -> FacebookLogo(modifier = Modifier.size(26.dp))
                    "INSTAGRAM" -> InstagramLogo(modifier = Modifier.size(28.dp))
                    "AMAZON" -> AmazonLogo(modifier = Modifier.size(26.dp))
                    "FLIPKART" -> FlipkartLogo(modifier = Modifier.size(26.dp))
                    else -> {
                        Text(
                            text = item.title.take(1).uppercase(),
                            color = VelocityCyan,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Article card matching Discover feed in the mockup.
 */
@Composable
private fun DiscoverArticleCard(
    article: DiscoverArticle,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("discover_article_${article.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xCC111827)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2238BDF8))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stylized rich thumbnail
            Box(
                modifier = Modifier
                    .size(width = 90.dp, height = 70.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(article.gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = article.category.take(3).uppercase(),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = article.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = article.source,
                        color = VelocityCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = " • ${article.timeAgo}",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// Vector Logos for Shortcuts
// ==========================================

@Composable
fun GoogleGLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Draw colorful circle sectors for Google G
        drawArc(
            color = BrandGoogleBlue,
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = true
        )
        drawArc(
            color = BrandGoogleGreen,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = true
        )
        drawArc(
            color = BrandGoogleYellow,
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = true
        )
        drawArc(
            color = BrandGoogleRed,
            startAngle = 270f,
            sweepAngle = 90f,
            useCenter = true
        )
        drawCircle(color = Color(0xFF131926), radius = w * 0.35f)
        drawLine(
            color = BrandGoogleBlue,
            start = Offset(w * 0.5f, h * 0.5f),
            end = Offset(w * 0.95f, h * 0.5f),
            strokeWidth = h * 0.22f
        )
    }
}

@Composable
fun YouTubeLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BrandYouTube),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(14.dp)) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, size.height * 0.5f)
                lineTo(0f, size.height)
                close()
            }
            drawPath(path, Color.White)
        }
    }
}

@Composable
fun GmailLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "M",
            color = BrandGmail,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp
        )
    }
}

@Composable
fun ChatGPTLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(BrandChatGPT),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "✱",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

@Composable
fun GitHubLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(BrandGitHub),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "🐙",
            fontSize = 14.sp
        )
    }
}

@Composable
fun FacebookLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(BrandFacebook),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "f",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
fun InstagramLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(listOf(BrandInstagramStart, BrandInstagramEnd))
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "📷",
            fontSize = 12.sp
        )
    }
}

@Composable
fun AmazonLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF232F3E)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "a",
            color = BrandAmazon,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp
        )
    }
}

@Composable
fun FlipkartLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BrandFlipkart),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "f",
            color = Color(0xFF2874F0),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp
        )
    }
}
