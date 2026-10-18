package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DownloadItem
import com.example.domain.model.DownloadStatus
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SecureGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    downloads: List<DownloadItem>,
    onOpenFile: (DownloadItem) -> Unit,
    onPauseDownload: (Long) -> Unit,
    onResumeDownload: (Long) -> Unit,
    onCancelDownload: (Long) -> Unit,
    onDeleteDownload: (DownloadItem) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads (${downloads.size})", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val context = androidx.compose.ui.platform.LocalContext.current
        val folderPath = com.example.util.StorageUtils.getDisplayFolderPath(context)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Folder: $folderPath",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (downloads.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No downloads yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    items(downloads, key = { it.id }) { item ->
                        DownloadCardItem(
                            item = item,
                            onOpen = { onOpenFile(item) },
                            onPause = { onPauseDownload(item.id) },
                            onResume = { onResumeDownload(item.id) },
                            onCancel = { onCancelDownload(item.id) },
                            onDelete = { onDeleteDownload(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadCardItem(
    item: DownloadItem,
    onOpen: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val sizeText = if (item.totalBytes > 0) {
        val totalMb = item.totalBytes / (1024f * 1024f)
        val downloadedMb = item.downloadedBytes / (1024f * 1024f)
        "%.1f MB / %.1f MB".format(downloadedMb, totalMb)
    } else if (item.downloadedBytes > 0) {
        "%.1f MB".format(item.downloadedBytes / (1024f * 1024f))
    } else {
        "Starting..."
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(enabled = item.status == DownloadStatus.COMPLETED, onClick = onOpen)
            .testTag("download_card_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (item.status) {
                        DownloadStatus.COMPLETED -> Icons.Default.CheckCircle
                        DownloadStatus.FAILED -> Icons.Default.Error
                        DownloadStatus.RUNNING -> Icons.Default.Bolt
                        DownloadStatus.CANCELED -> Icons.Default.Cancel
                        DownloadStatus.QUEUED -> Icons.Default.HourglassEmpty
                        DownloadStatus.PAUSED -> Icons.Default.Pause
                    },
                    contentDescription = null,
                    tint = when (item.status) {
                        DownloadStatus.COMPLETED -> SecureGreen
                        DownloadStatus.FAILED -> DangerRed
                        DownloadStatus.RUNNING -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.filename,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$sizeText • ${item.status.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (item.formattedSpeed.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SecureGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = item.formattedSpeed,
                                    color = SecureGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                if (item.status == DownloadStatus.RUNNING) {
                    IconButton(onClick = onPause) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause Download",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Cancel Download",
                            tint = DangerRed
                        )
                    }
                } else if (item.status == DownloadStatus.PAUSED) {
                    IconButton(onClick = onResume) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume Download",
                            tint = SecureGreen
                        )
                    }
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Cancel Download",
                            tint = DangerRed
                        )
                    }
                } else if (item.status == DownloadStatus.COMPLETED) {
                    IconButton(onClick = onOpen) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = "Open File",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete File",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove Record",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (item.status == DownloadStatus.RUNNING || item.status == DownloadStatus.PAUSED) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { (item.progressPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (item.status == DownloadStatus.PAUSED) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
