package com.example.util

import android.webkit.MimeTypeMap
import java.io.File
import java.util.Locale

/**
 * Sanitizes download filenames to prevent directory traversal and filesystem attacks.
 */
object FilenameSanitizer {

    private const val MAX_FILENAME_LENGTH = 120
    private val ILLEGAL_CHARS = Regex("[\\\\/:*?\"<>|\\x00-\\x1F]")

    private val COMMON_MIME_EXTENSIONS = mapOf(
        "image/jpeg" to "jpg",
        "image/jpg" to "jpg",
        "image/png" to "png",
        "image/webp" to "webp",
        "image/gif" to "gif",
        "image/svg+xml" to "svg",
        "application/pdf" to "pdf",
        "application/zip" to "zip",
        "application/json" to "json",
        "text/html" to "html",
        "text/plain" to "txt",
        "audio/mpeg" to "mp3",
        "video/mp4" to "mp4"
    )

    fun sanitize(rawName: String?, mimeType: String? = null, fallbackDefault: String = "download"): String {
        var name = rawName?.trim() ?: ""

        // Extract filename from path by taking only the last component
        name = name.substringAfterLast("/").substringAfterLast("\\")
        name = name.replace("..", "")
        name = name.replace(ILLEGAL_CHARS, "_")

        // Strip leading/trailing underscores or dots
        name = name.trim('.', '_', ' ')

        if (name.isBlank()) {
            name = fallbackDefault
        }

        // Split base and extension
        val dotIndex = name.lastIndexOf('.')
        var baseName: String
        var extension: String

        if (dotIndex in 1 until name.length - 1) {
            baseName = name.substring(0, dotIndex)
            extension = name.substring(dotIndex + 1).lowercase(Locale.ROOT)
        } else {
            baseName = name
            extension = getExtensionFromMime(mimeType) ?: "bin"
        }

        // Truncate base name if it exceeds limit
        if (baseName.length > MAX_FILENAME_LENGTH) {
            baseName = baseName.substring(0, MAX_FILENAME_LENGTH)
        }

        return if (extension.isNotBlank()) "$baseName.$extension" else baseName
    }

    /**
     * Resolves a non-colliding filename in a given directory by appending index (1), (2), etc.
     */
    fun getUniqueFile(directory: File, sanitizedName: String): File {
        var file = File(directory, sanitizedName)
        if (!file.exists()) return file

        val dotIndex = sanitizedName.lastIndexOf('.')
        val base = if (dotIndex > 0) sanitizedName.substring(0, dotIndex) else sanitizedName
        val ext = if (dotIndex > 0) sanitizedName.substring(dotIndex) else ""

        var counter = 1
        while (file.exists() && counter < 1000) {
            file = File(directory, "$base ($counter)$ext")
            counter++
        }
        return file
    }

    private fun getExtensionFromMime(mimeType: String?): String? {
        if (mimeType.isNullOrBlank()) return null
        val cleanMime = mimeType.substringBefore(";").trim().lowercase(Locale.ROOT)
        COMMON_MIME_EXTENSIONS[cleanMime]?.let { return it }

        return try {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(cleanMime)
        } catch (_: Throwable) {
            null
        }
    }
}
