package com.aaryo.vedabrowser.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaryo.vedabrowser.download.DownloadNotificationHelper
import com.aaryo.vedabrowser.model.DownloadItem
import com.aaryo.vedabrowser.model.DownloadStatus

@Composable
fun DownloadBanner(
    item: DownloadItem?,
    onOpen: (DownloadItem) -> Unit,
    onViewAll: () -> Unit,
    onDismiss: () -> Unit,
    onPause: ((DownloadItem) -> Unit)? = null,
    onResume: ((DownloadItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = item != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        if (item == null) return@AnimatedVisibility

        val isComplete = item.status == DownloadStatus.COMPLETED
        val isFailed = item.status == DownloadStatus.FAILED
        val isDownloading = item.status == DownloadStatus.DOWNLOADING
        val isPaused = item.status == DownloadStatus.PAUSED

        val percent = if (item.totalBytes > 0) {
            ((item.downloadedBytes * 100) / item.totalBytes).toInt().coerceIn(0, 100)
        } else {
            0
        }

        val iconColor = when {
            isComplete -> Color(0xFF10B981)
            isFailed -> Color(0xFFEF4444)
            isPaused -> Color(0xFFF59E0B)
            else -> MaterialTheme.colorScheme.primary
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
            border = BorderStroke(
                1.dp,
                when {
                    isComplete -> Color(0xFF10B981).copy(alpha = 0.5f)
                    isFailed -> Color(0xFFEF4444).copy(alpha = 0.5f)
                    isPaused -> Color(0xFFF59E0B).copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                }
            ),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("download_in_app_banner")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Top row: Status icon, title, Pause/Resume action, dismiss button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(iconColor.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = when {
                                isComplete -> Icons.Default.CheckCircle
                                isFailed -> Icons.Default.Error
                                isPaused -> Icons.Default.PauseCircle
                                else -> Icons.Default.Download
                            },
                            contentDescription = "Download Status",
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                isComplete -> "Download Complete"
                                isFailed -> "Download Failed"
                                isPaused -> "Download Paused"
                                else -> "Downloading ($percent%)"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = iconColor
                            )
                        )
                        Text(
                            text = item.fileName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Quick Pause / Resume Icon in top bar
                    if (isDownloading && onPause != null) {
                        IconButton(
                            onClick = { onPause(item) },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("download_banner_quick_pause_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else if (isPaused && onResume != null) {
                        IconButton(
                            onClick = { onResume(item) },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("download_banner_quick_resume_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("dismiss_download_banner_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Progress Bar or Folder Info
                if (isDownloading || isPaused) {
                    val progressColor = if (isPaused) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary

                    if (item.totalBytes > 0) {
                        LinearProgressIndicator(
                            progress = { percent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = progressColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = progressColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val progressText = if (item.totalBytes > 0) {
                            "${DownloadNotificationHelper.formatBytes(item.downloadedBytes)} / ${DownloadNotificationHelper.formatBytes(item.totalBytes)}" +
                                    if (isPaused) " • Paused" else ""
                        } else {
                            DownloadNotificationHelper.formatBytes(item.downloadedBytes) +
                                    if (isPaused) " • Paused" else ""
                        }
                        Text(
                            text = progressText,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = if (isPaused) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "Internal Storage/Veda Browser",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (isComplete) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Internal Storage/Veda Browser • ${DownloadNotificationHelper.formatBytes(item.totalBytes)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onViewAll,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("download_banner_details_button")
                    ) {
                        Text("All Downloads", style = MaterialTheme.typography.labelMedium)
                    }

                    if (isDownloading && onPause != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { onPause(item) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("download_banner_pause_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pause", style = MaterialTheme.typography.labelMedium)
                        }
                    } else if (isPaused && onResume != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onResume(item) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("download_banner_resume_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resume", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    if (isComplete) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onOpen(item) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("download_banner_open_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
