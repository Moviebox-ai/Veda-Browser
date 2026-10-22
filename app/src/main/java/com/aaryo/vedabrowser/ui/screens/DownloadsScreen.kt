package com.aaryo.vedabrowser.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaryo.vedabrowser.model.DownloadItem
import com.aaryo.vedabrowser.model.DownloadStatus

data class MockDownloadItem(
    val id: String,
    val filename: String,
    val sizeMeta: String,
    val iconType: String,
    val iconBg: Color,
    val icon: ImageVector,
    val isDownloading: Boolean = false,
    val progress: Float = 1f
)

/**
 * Screen 8: Download Manager
 * Exact replica of the reference mockup:
 * - Top Bar: "← Downloads"
 * - Filter pills: All, Images, Videos, Documents
 * - Item 1: video_1080p.mp4 (Purple rounded square, progress bar, pause button)
 * - Item 2: App.apk (Green rounded square, Completed, ⋮)
 * - Item 3: Document.pdf (Red/Orange rounded square, Completed, ⋮)
 * - Item 4: Image.jpg (Cyan rounded square, Completed, ⋮)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    downloads: List<DownloadItem>,
    onOpenDownload: (DownloadItem) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onDismiss()
    }

    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val filters = listOf("All", "Images", "Videos", "Documents")

    // Default sample list from mockup
    val sampleDownloads = remember {
        listOf(
            MockDownloadItem(
                id = "1",
                filename = "video_1080p.mp4",
                sizeMeta = "152 MB • 2 mins left",
                iconType = "video",
                iconBg = Color(0xFF8B5CF6),
                icon = Icons.Default.Videocam,
                isDownloading = true,
                progress = 0.65f
            ),
            MockDownloadItem(
                id = "2",
                filename = "App.apk",
                sizeMeta = "28 MB • Completed",
                iconType = "app",
                iconBg = Color(0xFF10B981),
                icon = Icons.Default.Android
            ),
            MockDownloadItem(
                id = "3",
                filename = "Document.pdf",
                sizeMeta = "4.2 MB • Completed",
                iconType = "doc",
                iconBg = Color(0xFFEF4444),
                icon = Icons.Default.Description
            ),
            MockDownloadItem(
                id = "4",
                filename = "Image.jpg",
                sizeMeta = "1.1 MB • Completed",
                iconType = "image",
                iconBg = Color(0xFF06B6D4),
                icon = Icons.Default.Image
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Downloads",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("downloads_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF070B19)
                )
            )
        },
        containerColor = Color(0xFF070B19),
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter Chips Row: All, Images, Videos, Documents
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEachIndexed { index, filter ->
                    val isSelected = selectedFilterIndex == index
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Color(0xFF2563EB) else Color(0xFF13192B),
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E283C)),
                        modifier = Modifier
                            .clickable { selectedFilterIndex = index }
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Download items list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Filter items based on selected category
                val filteredList = sampleDownloads.filter { item ->
                    when (selectedFilterIndex) {
                        1 -> item.iconType == "image"
                        2 -> item.iconType == "video"
                        3 -> item.iconType == "doc"
                        else -> true
                    }
                }

                items(filteredList, key = { it.id }) { item ->
                    DownloadCardItem(item = item)
                }

                // Append any dynamic downloads if present
                items(downloads, key = { it.id }) { realItem ->
                    RealDownloadCardItem(
                        item = realItem,
                        onClick = { onOpenDownload(realItem) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadCardItem(item: MockDownloadItem) {
    var isPaused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colored square icon container
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(item.iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.filename,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = item.sizeMeta,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )

            if (item.isDownloading) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { if (isPaused) 0.65f else item.progress },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF3B82F6),
                    trackColor = Color(0xFF1E283C)
                )
            }
        }

        if (item.isDownloading) {
            IconButton(
                onClick = { isPaused = !isPaused },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = "Pause/Resume",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            IconButton(
                onClick = { },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun RealDownloadCardItem(
    item: DownloadItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2563EB)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.fileName,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "${item.downloadedBytes / 1024} KB • ${item.status.name.lowercase().replaceFirstChar { it.uppercase() }}",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )

            if (item.status == DownloadStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF3B82F6),
                    trackColor = Color(0xFF1E283C)
                )
            }
        }

        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
