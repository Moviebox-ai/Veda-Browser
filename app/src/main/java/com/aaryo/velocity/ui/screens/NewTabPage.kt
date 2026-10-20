package com.aaryo.velocity.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaryo.velocity.model.BrowserSettings
import com.aaryo.velocity.model.QuickDialShortcut
import com.aaryo.velocity.model.ShieldStats
import com.aaryo.velocity.ui.components.VelocityLogoIcon
import com.aaryo.velocity.ui.theme.VelocityBlue
import com.aaryo.velocity.ui.theme.VelocityCyan

/**
 * Screen 2: Home Page & Screen 6: Incognito Mode
 * Exactly matching the reference design layout:
 * - Centered Velocity 3D Gradient Logo + "Velocity Browser" + "Fast • Secure • Private"
 * - Omnibox pill search input with magnifying glass & microphone
 * - 4x2 Circular Quick Dial icons
 * - Discover cards section
 * - Stealth mode for Incognito with Hat & Glasses illustration
 */
@Composable
fun NewTabPage(
    isIncognito: Boolean,
    shieldStats: ShieldStats,
    settings: BrowserSettings = BrowserSettings(),
    quickDials: List<QuickDialShortcut> = emptyList(),
    onAddQuickDial: (QuickDialShortcut) -> Unit = {},
    onDeleteQuickDial: (String) -> Unit = {},
    onNavigate: (String) -> Unit,
    onOpenBookmarks: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenDownloads: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenShield: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var searchInput by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isIncognito) Color(0xFF070913) else Color(0xFF070B19))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            if (!isIncognito) {
                // Screen 2: Central Velocity Branding
                VelocityLogoIcon(size = 56.dp)

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Velocity Browser",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Fast • Secure • Private",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            } else {
                // Screen 6: Incognito Mode Header
                IncognitoHatGlassesIllustration(modifier = Modifier.size(72.dp))

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "You're in Incognito Mode",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Your browsing activity won't be saved on this device.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Omnibox Search Bar (Dark pill container with search icon and mic)
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF12182B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchInput.isEmpty()) {
                            Text(
                                text = "Search or type URL",
                                color = Color(0xFF64748B),
                                fontSize = 14.sp
                            )
                        }
                        BasicTextField(
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(VelocityCyan),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (searchInput.isNotBlank()) {
                                        onNavigate(searchInput.trim())
                                    }
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_search_input")
                        )
                    }

                    IconButton(
                        onClick = {
                            if (searchInput.isNotBlank()) {
                                onNavigate(searchInput.trim())
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Search",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Quick Dials Grid
            if (!isIncognito) {
                // Screen 2: 8 items (4x2)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Row 1: Google, YouTube, Facebook, Instagram
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        QuickDialItem(
                            label = "Google",
                            badgeType = "google",
                            onClick = { onNavigate("https://www.google.com") }
                        )
                        QuickDialItem(
                            label = "YouTube",
                            badgeType = "youtube",
                            onClick = { onNavigate("https://m.youtube.com") }
                        )
                        QuickDialItem(
                            label = "Facebook",
                            badgeType = "facebook",
                            onClick = { onNavigate("https://m.facebook.com") }
                        )
                        QuickDialItem(
                            label = "Instagram",
                            badgeType = "instagram",
                            onClick = { onNavigate("https://www.instagram.com") }
                        )
                    }

                    // Row 2: Amazon, Wikipedia, Twitter, Add
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        QuickDialItem(
                            label = "Amazon",
                            badgeType = "amazon",
                            onClick = { onNavigate("https://www.amazon.com") }
                        )
                        QuickDialItem(
                            label = "Wikipedia",
                            badgeType = "wikipedia",
                            onClick = { onNavigate("https://en.m.wikipedia.org") }
                        )
                        QuickDialItem(
                            label = "Twitter",
                            badgeType = "twitter",
                            onClick = { onNavigate("https://twitter.com") }
                        )
                        QuickDialItem(
                            label = "Add",
                            badgeType = "add",
                            onClick = { showAddDialog = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Discover Section matching Mockup
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Discover",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    // Discover Card 1: Top 10 Beautiful Places
                    DiscoverCard(
                        title = "Top 10 Beautiful Places to Visit in 2026",
                        sourceAndMeta = "Travel World • 2h ago",
                        imageType = "places",
                        onClick = { onNavigate("https://www.nationalgeographic.com/travel") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Discover Card 2: Latest Tech News
                    DiscoverCard(
                        title = "Latest Tech News and Updates",
                        sourceAndMeta = "Tech Today • 5h ago",
                        imageType = "tech",
                        onClick = { onNavigate("https://news.google.com/topics/CAAqJggKIiBDQkFTRWdvSUwyMHZNRGRqTVhZU0FtVnVHZ0pWVXlnQVAB") }
                    )
                }
            } else {
                // Screen 6 (Incognito Mode): Google, YouTube, Wikipedia, Add
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    QuickDialItem(
                        label = "Google",
                        badgeType = "google",
                        onClick = { onNavigate("https://www.google.com") }
                    )
                    QuickDialItem(
                        label = "YouTube",
                        badgeType = "youtube",
                        onClick = { onNavigate("https://m.youtube.com") }
                    )
                    QuickDialItem(
                        label = "Wikipedia",
                        badgeType = "wikipedia",
                        onClick = { onNavigate("https://en.m.wikipedia.org") }
                    )
                    QuickDialItem(
                        label = "Add",
                        badgeType = "add",
                        onClick = { showAddDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }

        // Add Shortcut Dialog
        if (showAddDialog) {
            var newTitle by remember { mutableStateOf("") }
            var newUrl by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Shortcut", color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text("Name") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VelocityCyan,
                                focusedTextColor = Color.White
                            )
                        )
                        OutlinedTextField(
                            value = newUrl,
                            onValueChange = { newUrl = it },
                            label = { Text("URL (e.g. https://...)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VelocityCyan,
                                focusedTextColor = Color.White
                            )
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newTitle.isNotBlank() && newUrl.isNotBlank()) {
                                val finalUrl = if (newUrl.startsWith("http://") || newUrl.startsWith("https://")) newUrl else "https://$newUrl"
                                onAddQuickDial(QuickDialShortcut(title = newTitle.trim(), url = finalUrl))
                                showAddDialog = false
                            }
                        }
                    ) {
                        Text("Add", color = VelocityCyan)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                },
                containerColor = Color(0xFF131926)
            )
        }
    }
}

/**
 * Renders quick dial circular badge matching the exact mockup
 */
@Composable
private fun QuickDialItem(
    label: String,
    badgeType: String,
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
                .size(54.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when (badgeType) {
                "google" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(26.dp)) {
                            val w = size.width
                            val h = size.height
                            // Draw 4-color Google 'G'
                            drawCircle(
                                color = Color(0xFF4285F4),
                                radius = w * 0.45f,
                                style = Stroke(width = w * 0.22f)
                            )
                            // Crossbar
                            drawLine(
                                color = Color(0xFF4285F4),
                                start = Offset(w * 0.45f, h * 0.5f),
                                end = Offset(w * 0.95f, h * 0.5f),
                                strokeWidth = w * 0.22f,
                                cap = StrokeCap.Square
                            )
                            // Top curve yellow/green
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
                }
                "youtube" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFE50914)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "YouTube",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                "facebook" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1877F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "f",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                    }
                }
                "instagram" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF833AB4),
                                        Color(0xFFFD1D1D),
                                        Color(0xFFFCB045)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(24.dp)) {
                            val w = size.width
                            val stroke = w * 0.12f
                            drawRoundRect(
                                color = Color.White,
                                cornerRadius = CornerRadius(w * 0.25f, w * 0.25f),
                                style = Stroke(width = stroke)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = w * 0.25f,
                                style = Stroke(width = stroke)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = stroke * 0.7f,
                                center = Offset(w * 0.75f, size.height * 0.25f)
                            )
                        }
                    }
                }
                "amazon" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF131921)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "a",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Canvas(modifier = Modifier.size(width = 18.dp, height = 4.dp)) {
                                val path = Path().apply {
                                    moveTo(0f, 0f)
                                    quadraticTo(size.width * 0.5f, size.height * 1.5f, size.width, 0f)
                                }
                                drawPath(
                                    path = path,
                                    color = Color(0xFFFF9900),
                                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }
                    }
                }
                "wikipedia" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "W",
                            color = Color.Black,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                    }
                }
                "twitter" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1DA1F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "𝕏",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E283C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Shortcut",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Discover section card matching the mock cards
 */
@Composable
private fun DiscoverCard(
    title: String,
    sourceAndMeta: String,
    imageType: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF12182B),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E283C)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail image with scenic art
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (imageType == "places") {
                            Brush.linearGradient(listOf(Color(0xFF0D9488), Color(0xFF0284C7), Color(0xFF1E3A8A)))
                        } else {
                            Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFF1E1B4B)))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (imageType == "places") {
                    Icon(
                        imageVector = Icons.Default.Landscape,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = sourceAndMeta,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Incognito Hat & Glasses Illustration matching Screen 6
 */
@Composable
private fun IncognitoHatGlassesIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Hat brim
        val brimPath = Path().apply {
            moveTo(w * 0.1f, h * 0.38f)
            quadraticTo(w * 0.5f, h * 0.32f, w * 0.9f, h * 0.38f)
        }
        drawPath(
            path = brimPath,
            color = Color.White,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Hat crown
        val crownPath = Path().apply {
            moveTo(w * 0.28f, h * 0.35f)
            lineTo(w * 0.32f, h * 0.15f)
            cubicTo(w * 0.42f, h * 0.10f, w * 0.58f, h * 0.10f, w * 0.68f, h * 0.15f)
            lineTo(w * 0.72f, h * 0.35f)
        }
        drawPath(
            path = crownPath,
            color = Color.White,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Glasses lenses
        val leftLensCenter = Offset(w * 0.35f, h * 0.62f)
        val rightLensCenter = Offset(w * 0.65f, h * 0.62f)
        val lensRadius = w * 0.14f

        drawCircle(
            color = Color.White,
            radius = lensRadius,
            center = leftLensCenter,
            style = Stroke(width = 2.5.dp.toPx())
        )
        drawCircle(
            color = Color.White,
            radius = lensRadius,
            center = rightLensCenter,
            style = Stroke(width = 2.5.dp.toPx())
        )

        // Glasses bridge
        drawLine(
            color = Color.White,
            start = Offset(w * 0.45f, h * 0.60f),
            end = Offset(w * 0.55f, h * 0.60f),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
