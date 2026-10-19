package com.example.velocity.model

import java.util.UUID

enum class DownloadStatus {
    DOWNLOADING,
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
    val localFilePath: String? = null
) {
    val progress: Float
        get() = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f

    val formattedSize: String
        get() {
            if (totalBytes <= 0) return "Unknown size"
            val kb = totalBytes / 1024.0
            val mb = kb / 1024.0
            return if (mb >= 1.0) String.format("%.1f MB", mb) else String.format("%.0f KB", kb)
        }
}
