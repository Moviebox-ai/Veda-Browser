package com.aaryo.vedabrowser.model

import java.util.UUID

enum class SniffedMediaType {
    VIDEO,
    AUDIO,
    STREAM
}

data class SniffedMediaItem(
    val id: String = UUID.randomUUID().toString(),
    val tabId: String,
    val url: String,
    val pageUrl: String = "",
    val pageTitle: String = "",
    val filename: String,
    val mimeType: String = "video/mp4",
    val mediaType: SniffedMediaType = SniffedMediaType.VIDEO,
    val sizeBytes: Long = -1L,
    val qualityOrFormat: String = "MP4 Video",
    val detectedTimestamp: Long = System.currentTimeMillis()
) {
    val formattedSize: String
        get() = when {
            sizeBytes <= 0 -> if (mediaType == SniffedMediaType.STREAM) "Live / HLS Stream" else "Size unknown"
            sizeBytes < 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f KB", sizeBytes / 1024.0)
            sizeBytes < 1024 * 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", sizeBytes / (1024.0 * 1024.0))
            else -> String.format(java.util.Locale.US, "%.2f GB", sizeBytes / (1024.0 * 1024.0 * 1024.0))
        }

    val displayTitle: String
        get() = when {
            filename.isNotBlank() && !filename.startsWith("media_") -> filename
            pageTitle.isNotBlank() -> pageTitle
            else -> filename
        }
}
