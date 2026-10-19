package temp_legacy_to_port

import android.net.Uri

/**
 * Validates and classifies URL schemes to prevent scheme-based attacks
 * and direct external URLs to safe handlers.
 */
object SchemePolicy {

    private val WEB_SCHEMES = setOf("http", "https")
    private val SAFE_INTERNAL_SCHEMES = setOf("about", "javascript", "data", "blob", "velocity")
    private val EXTERNAL_INTENT_SCHEMES = setOf("mailto", "tel", "sms", "geo", "intent", "market")
    private val BLOCKED_SCHEMES = setOf("file", "content")

    fun isWebScheme(url: String?): Boolean {
        if (url == null) return false
        val scheme = Uri.parse(url).scheme?.lowercase() ?: return false
        return scheme in WEB_SCHEMES
    }

    fun isExternalScheme(url: String?): Boolean {
        if (url == null) return false
        val scheme = Uri.parse(url).scheme?.lowercase() ?: return false
        return scheme in EXTERNAL_INTENT_SCHEMES
    }

    fun isSafeInternal(url: String?): Boolean {
        if (url == null) return false
        val scheme = Uri.parse(url).scheme?.lowercase() ?: return false
        return scheme in SAFE_INTERNAL_SCHEMES
    }

    fun isBlockedScheme(url: String?): Boolean {
        if (url == null) return false
        val scheme = Uri.parse(url).scheme?.lowercase() ?: return false
        return scheme in BLOCKED_SCHEMES
    }
}
