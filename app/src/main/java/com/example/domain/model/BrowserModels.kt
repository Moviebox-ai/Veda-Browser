package com.example.domain.model

data class Bookmark(
    val id: Long = 0,
    val title: String,
    val url: String,
    val folder: String = "Default",
    val createdAt: Long = System.currentTimeMillis()
)

data class HistoryItem(
    val id: Long = 0,
    val title: String,
    val url: String,
    val visitedAt: Long = System.currentTimeMillis(),
    val visitCount: Int = 1
)

data class Shortcut(
    val id: Long = 0,
    val title: String,
    val url: String,
    val position: Int = 0
)

data class DownloadItem(
    val id: Long = 0,
    val url: String,
    val filename: String,
    val filePath: String,
    val mimeType: String,
    val totalBytes: Long = -1,
    val downloadedBytes: Long = 0,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val errorMessage: String? = null,
    val speedBytesPerSec: Long = 0
) {
    val progressPercent: Int
        get() = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0

    val formattedSpeed: String
        get() {
            if (status != DownloadStatus.RUNNING || speedBytesPerSec <= 0) return ""
            val mbPerSec = speedBytesPerSec / (1024f * 1024f)
            return if (mbPerSec >= 1f) "%.1f MB/s".format(mbPerSec) else "${speedBytesPerSec / 1024} KB/s"
        }
}

enum class DownloadStatus {
    QUEUED,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELED;

    companion object {
        fun fromString(str: String): DownloadStatus {
            return entries.find { it.name.equals(str, ignoreCase = true) } ?: QUEUED
        }
    }
}
