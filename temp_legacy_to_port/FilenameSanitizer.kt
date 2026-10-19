package temp_legacy_to_port

/**
 * Sanitizes download file names to prevent path traversal and filesystem injection.
 */
object FilenameSanitizer {

    private val ILLEGAL_CHARS = Regex("[/\\\\?%*:|\"<>]")

    fun sanitize(fileName: String, defaultName: String = "download"): String {
        var clean = fileName.trim()
            .replace(ILLEGAL_CHARS, "_")
            .replace("..", "_")
            .filter { it.code in 32..126 } // ASCII printable only

        if (clean.isBlank()) {
            clean = defaultName
        }

        // Limit filename length to 128 characters while preserving extension
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
