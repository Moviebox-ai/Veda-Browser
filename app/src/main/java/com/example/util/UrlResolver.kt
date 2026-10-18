package com.example.util

import com.example.domain.model.SearchEngine
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.regex.Pattern

/**
 * Robust URL resolver and validator for Velocity Browser.
 * Handles HTTPS default escalation, IP/localhost detection, internal protocols,
 * and dangerous scheme blocking.
 */
object UrlResolver {

    const val ABOUT_BLANK = "about:blank"
    const val VELOCITY_HOME = "velocity:home"
    const val VELOCITY_BOOKMARKS = "velocity:bookmarks"
    const val VELOCITY_HISTORY = "velocity:history"
    const val VELOCITY_DOWNLOADS = "velocity:downloads"
    const val VELOCITY_SETTINGS = "velocity:settings"

    private val DANGEROUS_SCHEMES = setOf(
        "javascript", "file", "content", "jar", "data", "blob"
    )

    private val DOMAIN_PATTERN = Pattern.compile(
        "^(https?://)?" +
                "(([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,63}|localhost|(\\d{1,3}\\.){3}\\d{1,3}|\\[[0-9a-fA-F:]+\\])" +
                "(:\\d{1,5})?" +
                "(/.*)?$"
    )

    private val IP_OR_LOCAL_PATTERN = Pattern.compile(
        "^((localhost)|(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})|(\\[[0-9a-fA-F:]+\\]))(:\\d{1,5})?(/.*)?$"
    )

    /**
     * Resolves raw text from the address bar into either a normalized web URL or search URL.
     */
    fun resolve(
        input: String,
        searchEngine: SearchEngine = SearchEngine.GOOGLE,
        httpsFirst: Boolean = false
    ): ResolvedUrl {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return ResolvedUrl(url = VELOCITY_HOME, type = ResolvedType.INTERNAL)
        }

        val lower = trimmed.lowercase(Locale.ROOT)

        // Internal velocity navigation
        if (lower == "home" || lower == VELOCITY_HOME || lower == "about:home") {
            return ResolvedUrl(url = VELOCITY_HOME, type = ResolvedType.INTERNAL)
        }
        if (lower == "about:blank") {
            return ResolvedUrl(url = ABOUT_BLANK, type = ResolvedType.INTERNAL)
        }
        if (lower.startsWith("velocity:")) {
            return ResolvedUrl(url = lower, type = ResolvedType.INTERNAL)
        }

        // Dangerous scheme check
        val colonIndex = trimmed.indexOf(':')
        if (colonIndex > 0) {
            val scheme = trimmed.substring(0, colonIndex).lowercase(Locale.ROOT)
            if (DANGEROUS_SCHEMES.contains(scheme)) {
                return ResolvedUrl(
                    url = trimmed,
                    type = ResolvedType.BLOCKED_SCHEME,
                    error = "Direct execution of $scheme: scheme is blocked for your security."
                )
            }
        }

        // Standard https
        if (lower.startsWith("https://")) {
            return ResolvedUrl(url = trimmed, type = ResolvedType.WEB_URL)
        }

        // Standard http - upgrade to https if httpsFirst is enabled and not local
        if (lower.startsWith("http://")) {
            val hostPart = trimmed.substring(7).substringBefore("/").substringBefore(":")
            val isLocal = hostPart.equals("localhost", ignoreCase = true) ||
                    IP_OR_LOCAL_PATTERN.matcher(hostPart).matches()
            val finalUrl = if (httpsFirst && !isLocal) {
                "https://" + trimmed.substring(7)
            } else {
                trimmed
            }
            return ResolvedUrl(url = finalUrl, type = ResolvedType.WEB_URL)
        }

        // Detect if it is an IP or localhost with optional port
        if (IP_OR_LOCAL_PATTERN.matcher(trimmed).matches()) {
            return ResolvedUrl(url = "http://$trimmed", type = ResolvedType.WEB_URL)
        }

        // Domain matching (e.g. example.com, github.com/user, news.bbc.co.uk)
        // Ensure there are no spaces within the domain part
        if (!trimmed.contains(" ") && DOMAIN_PATTERN.matcher(trimmed).matches()) {
            // Default to HTTPS first
            return ResolvedUrl(url = "https://$trimmed", type = ResolvedType.WEB_URL)
        }

        // Fallback: search query
        return ResolvedUrl(
            url = searchEngine.buildSearchUrl(trimmed),
            type = ResolvedType.SEARCH_QUERY,
            searchQuery = trimmed
        )
    }

    /**
     * Determines whether a given URL is a secure HTTPS connection.
     */
    fun isSecure(url: String): Boolean {
        return url.startsWith("https://", ignoreCase = true)
    }

    /**
     * Extracts a clean, human-readable host/domain for display in the address bar.
     */
    fun extractDisplayHost(url: String): String {
        if (url.isEmpty() || url == ABOUT_BLANK || url.startsWith("velocity:")) {
            return ""
        }
        return try {
            val uri = URI(url)
            val host = uri.host ?: return url
            if (host.startsWith("www.")) host.substring(4) else host
        } catch (_: Exception) {
            url.substringBefore("/").substringAfter("://").removePrefix("www.")
        }
    }

    /**
     * Extracts full display URL with query decoded safely for editing.
     */
    fun getEditableUrl(url: String): String {
        if (url.startsWith("velocity:") || url == ABOUT_BLANK) return ""
        return try {
            val protectedUrl = url.replace("+", "%2B")
            URLDecoder.decode(protectedUrl, StandardCharsets.UTF_8.name())
        } catch (_: Exception) {
            url
        }
    }

    data class ResolvedUrl(
        val url: String,
        val type: ResolvedType,
        val searchQuery: String? = null,
        val error: String? = null
    )

    enum class ResolvedType {
        WEB_URL,
        INTERNAL,
        SEARCH_QUERY,
        BLOCKED_SCHEME
    }
}
