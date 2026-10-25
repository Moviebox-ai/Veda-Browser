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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaryo.vedabrowser.model.DownloadItem
import com.aaryo.vedabrowser.model.DownloadStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    downloads: List<DownloadItem>,
    onOpenDownload: (DownloadItem) -> Unit,
    onPauseDownload: (DownloadItem) -> Unit = {},
    onResumeDownload: (DownloadItem, Boolean) -> Unit = { _, _ -> },
    onShareDownload: (DownloadItem) -> Unit = {},
    onRemoveDownload: (DownloadItem) -> Unit = {},
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onDismiss()
    }

    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val filters = listOf("All", "Images", "Videos", "Documents")
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var itemToRestartPrompt by remember { mutableStateOf<DownloadItem?>(null) }

    // Filter real items based on selected category
    val filteredList = remember(downloads, selectedFilterIndex) {
        downloads.filter { item ->
            when (selectedFilterIndex) {
                1 -> item.category == "image"
                2 -> item.category == "video"
                3 -> item.category == "doc"
                else -> true
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Download History?", color = Color.White) },
            text = { Text("This will clear your download history list. Downloaded files on disk will remain untouched.", color = Color(0xFF94A3B8)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmDialog = false
                        onClearAll()
                    }
                ) {
                    Text("Clear All", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF13192B)
        )
    }

    // Server non-resumable confirmation dialog
    if (itemToRestartPrompt != null) {
        val nonResumableItem = itemToRestartPrompt!!
        AlertDialog(
            onDismissRequest = { itemToRestartPrompt = null },
            title = { Text("Resume Not Supported", color = Color.White) },
            text = {
                Text(
                    "The server hosting \"${nonResumableItem.fileName}\" does not support resuming interrupted downloads.\n\nWould you like to restart downloading this file from the beginning?",
                    color = Color(0xFF94A3B8)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        itemToRestartPrompt = null
                        onResumeDownload(nonResumableItem, true)
                    }
                ) {
                    Text("Restart from Beginning", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToRestartPrompt = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF13192B)
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
                actions = {
                    if (downloads.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier.testTag("downloads_clear_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear all downloads",
                                tint = Color(0xFF94A3B8)
                            )
                        }
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

            // Storage location indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Save folder: Internal Storage/Veda Browser",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Direct Internal Storage (/storage/emulated/0/Veda Browser)",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (filteredList.isEmpty()) {
                // Clean and helpful empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF13192B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (downloads.isEmpty()) "No downloads yet" else "No matching downloads",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (downloads.isEmpty()) "Files you download while browsing will appear here" else "Try selecting another category above",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        RealDownloadRow(
                            item = item,
                            onClick = { onOpenDownload(item) },
                            onPause = { onPauseDownload(item) },
                            onResume = {
                                if (!item.supportsResume) {
                                    itemToRestartPrompt = item
                                } else {
                                    onResumeDownload(item, false)
                                }
                            },
                            onShare = { onShareDownload(item) },
                            onRemove = { onRemoveDownload(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RealDownloadRow(
    item: DownloadItem,
    onClick: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onShare: () -> Unit,
    onRemove: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val (iconBg, iconVector) = getCategoryVisuals(item.category)
    val isDownloading = item.status == DownloadStatus.DOWNLOADING
    val isPaused = item.status == DownloadStatus.PAUSED

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                if (item.status == DownloadStatus.COMPLETED) {
                    onClick()
                } else if (isPaused) {
                    onResume()
                } else if (isDownloading) {
                    onPause()
                }
            }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colored squircle icon container
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = item.category,
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
                text = item.formattedProgressMeta,
                color = when (item.status) {
                    DownloadStatus.DOWNLOADING -> Color(0xFF38BDF8)
                    DownloadStatus.PAUSED -> Color(0xFFF59E0B)
                    DownloadStatus.FAILED -> Color(0xFFEF4444)
                    else -> Color(0xFF94A3B8)
                },
                fontSize = 12.sp
            )

            if (isDownloading || isPaused) {
                Spacer(modifier = Modifier.height(6.dp))
                val barColor = if (isPaused) Color(0xFFF59E0B) else Color(0xFF3B82F6)
                if (item.totalBytes > 0) {
                    LinearProgressIndicator(
                        progress = { item.progress },
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = barColor,
                        trackColor = Color(0xFF1E283C)
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = barColor,
                        trackColor = Color(0xFF1E283C)
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isDownloading) {
                // Pause button
                IconButton(
                    onClick = onPause,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("download_row_pause_btn_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause download",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                }
                // Cancel button
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("download_row_cancel_btn_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel download",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else if (isPaused) {
                // Resume button
                IconButton(
                    onClick = onResume,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("download_row_resume_btn_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume download",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(22.dp)
                    )
                }
                // Delete button
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("download_row_delete_btn_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete download",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("download_row_menu_btn_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(Color(0xFF13192B))
                ) {
                    DropdownMenuItem(
                        text = { Text("Open", color = Color.White) },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = Color(0xFF38BDF8))
                        },
                        onClick = {
                            menuExpanded = false
                            onClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share", color = Color.White) },
                        leadingIcon = {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF10B981))
                        },
                        onClick = {
                            menuExpanded = false
                            onShare()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color(0xFFEF4444)) },
                        leadingIcon = {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFEF4444))
                        },
                        onClick = {
                            menuExpanded = false
                            onRemove()
                        }
                    )
                }
            }
        }
    }
}

private fun getCategoryVisuals(category: String): Pair<Color, ImageVector> {
    return when (category) {
        "video" -> Pair(Color(0xFF8B5CF6), Icons.Default.Videocam)
        "image" -> Pair(Color(0xFF06B6D4), Icons.Default.Image)
        "app" -> Pair(Color(0xFF10B981), Icons.Default.Android)
        "doc" -> Pair(Color(0xFFEF4444), Icons.Default.Description)
        "audio" -> Pair(Color(0xFFF59E0B), Icons.Default.Audiotrack)
        else -> Pair(Color(0xFF2563EB), Icons.Default.FileDownload)
    }
}
