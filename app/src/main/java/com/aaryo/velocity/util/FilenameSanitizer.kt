package com.aaryo.velocity.util

object FilenameSanitizer {

    private val ILLEGAL_CHARS = Regex("[/\\\\?%*:|\"<>]")

    fun sanitize(fileName: String?, defaultName: String = "download"): String {
        if (fileName.isNullOrBlank()) return defaultName

        var clean = fileName.trim()
            .replace(ILLEGAL_CHARS, "_")
            .replace("..", "_")
            .filter { it.code in 32..126 }

        if (clean.isBlank()) {
            clean = defaultName
        }

        if (clean.length > 128) {
            val extIndex = clean.lastIndexOf('.')
            clean = if (extIndex != -1 && extIndex > clean.length - 10) {
                val ext = clean.substring(extIndex)
                clean.substring(0, 120 - ext.length) + ext
            } else {
                clean.substring(0, 120)
            }
        }

        return clean
    }
}
