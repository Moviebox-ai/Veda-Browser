package com.aaryo.vedabrowser.model

import java.util.Locale
import java.util.UUID

enum class DownloadStatus {
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class DownloadItem(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val url: String,
    val mimeType: String = "",
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val status: DownloadStatus = DownloadStatus.DOWNLOADING,
    val timestamp: Long = System.currentTimeMillis(),
    val localFilePath: String? = null,
    val userAgent: String? = null,
    val supportsResume: Boolean = true
) {
    val progress: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f

    val category: String
        get() {
            val lowerName = fileName.lowercase(Locale.ROOT)
            val lowerMime = mimeType.lowercase(Locale.ROOT)
            return when {
                lowerMime.startsWith("video/") || lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") || lowerName.endsWith(".webm") || lowerName.endsWith(".avi") || lowerName.endsWith(".mov") || lowerName.endsWith(".3gp") -> "video"
                lowerMime.startsWith("image/") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") || lowerName.endsWith(".webp") || lowerName.endsWith(".gif") || lowerName.endsWith(".svg") -> "image"
                lowerMime.startsWith("audio/") || lowerName.endsWith(".mp3") || lowerName.endsWith(".wav") || lowerName.endsWith(".m4a") || lowerName.endsWith(".aac") || lowerName.endsWith(".ogg") -> "audio"
                lowerName.endsWith(".apk") || lowerMime.contains("android.package-archive") -> "app"
                lowerMime.startsWith("text/") || lowerMime.contains("pdf") || lowerMime.contains("document") || lowerName.endsWith(".pdf") || lowerName.endsWith(".doc") || lowerName.endsWith(".docx") || lowerName.endsWith(".txt") || lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx") || lowerName.endsWith(".ppt") || lowerName.endsWith(".pptx") -> "doc"
                else -> "other"
            }
        }

    val formattedSize: String
        get() = formatBytes(if (totalBytes > 0) totalBytes else downloadedBytes)

    val formattedProgressMeta: String
        get() = when (status) {
            DownloadStatus.DOWNLOADING -> {
                if (totalBytes > 0) {
                    "${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)} • ${(progress * 100).toInt()}%"
                } else if (downloadedBytes > 0) {
                    "${formatBytes(downloadedBytes)} • Downloading"
                } else {
                    "Starting download…"
                }
            }
            DownloadStatus.PAUSED -> {
                if (totalBytes > 0) {
                    "${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)} • Paused"
                } else {
                    "${formatBytes(downloadedBytes)} • Paused"
                }
            }
            DownloadStatus.COMPLETED -> "${formattedSize} • Completed"
            DownloadStatus.FAILED -> "Download failed"
            DownloadStatus.CANCELLED -> "Cancelled"
        }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 KB"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                else -> String.format(Locale.US, "%.0f KB", kb)
            }
        }
    }
}
