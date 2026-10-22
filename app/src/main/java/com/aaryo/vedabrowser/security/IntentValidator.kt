package com.aaryo.vedabrowser.security

import android.content.Context
import android.content.Intent
import android.net.Uri

object IntentValidator {

    private val BROWSER_PACKAGE_NAMES = setOf(
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.chrome.canary",
        "org.chromium.chrome",
        "com.google.android.apps.chrome",
        "com.google.android.googlequicksearchbox",
        "com.google.android.apps.searchlite",
        "com.sec.android.app.sbrowser",
        "org.mozilla.firefox",
        "org.mozilla.fenix",
        "org.mozilla.focus",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.microsoft.emmx",
        "com.brave.browser",
        "com.duckduckgo.mobile.android",
        "com.android.browser",
        "com.google.android.browser",
        "com.android.internal.app.resolveractivity"
    )

    fun isBrowserPackage(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        val lower = packageName.lowercase()
        return BROWSER_PACKAGE_NAMES.any { lower == it || lower.startsWith("$it.") } ||
                lower.contains("chrome") ||
                (lower.contains("browser") && !lower.contains("vedabrowser") && !lower.contains("velocity"))
    }

    /**
     * Checks if a URI or Intent is intended to launch Google Chrome or another external web browser.
     */
    fun isBrowserIntent(uriString: String): Boolean {
        if (uriString.isBlank()) return false
        val lower = uriString.lowercase()

        // Chrome-specific custom schemes
        if (lower.startsWith("googlechrome://") ||
            lower.startsWith("googlechromes://") ||
            lower.startsWith("chromenative://") ||
            lower.startsWith("chrome://") ||
            lower.startsWith("chrome-search://")
        ) {
            return true
        }

        // Android App Link targeting Chrome or Google search
        if (lower.startsWith("android-app://com.android.chrome") ||
            lower.startsWith("android-app://com.google.android.googlequicksearchbox")
        ) {
            return true
        }

        // Intent specifying Chrome or browser package
        if (lower.startsWith("intent:")) {
            if (lower.contains("package=com.android.chrome") ||
                lower.contains("package=com.google.android.googlequicksearchbox") ||
                lower.contains("package=com.chrome") ||
                lower.contains("category=android.intent.category.browsable")
            ) {
                return true
            }
        }

        return false
    }

    /**
     * Extracts an HTTP/HTTPS URL from an intent:, chrome://, or android-app:// URI if present,
     * including dataUri, scheme=http/https, and S.browser_fallback_url.
     * Prevents search and web queries from escaping into external Chrome browser.
     */
    fun extractWebUrlFromIntent(uriString: String): String? {
        if (uriString.isBlank()) return null
        val trimmed = uriString.trim()

        // 1. Handle googlechrome:// and googlechromes:// navigate schemes
        if (trimmed.startsWith("googlechrome://navigate?url=", ignoreCase = true)) {
            val raw = trimmed.substringAfter("url=")
            val decoded = decodeUrl(raw)
            return cleanExtractedWebUrl(decoded)
        }
        if (trimmed.startsWith("googlechromes://navigate?url=", ignoreCase = true)) {
            val raw = trimmed.substringAfter("url=")
            val decoded = decodeUrl(raw)
            return cleanExtractedWebUrl(decoded)
        }

        // 2. Handle generic googlechrome:// and googlechromes://
        if (trimmed.startsWith("googlechrome://", ignoreCase = true)) {
            val target = trimmed.removePrefix("googlechrome://")
            return cleanExtractedWebUrl(target)
        }
        if (trimmed.startsWith("googlechromes://", ignoreCase = true)) {
            val target = trimmed.removePrefix("googlechromes://")
            return cleanExtractedWebUrl(target, defaultScheme = "https")
        }

        // 3. Handle chromenative://
        if (trimmed.startsWith("chromenative://", ignoreCase = true)) {
            if (trimmed.contains("url=", ignoreCase = true)) {
                val raw = trimmed.substringAfter("url=").substringBefore('&').substringBefore(';')
                val decoded = decodeUrl(raw)
                return cleanExtractedWebUrl(decoded)
            }
        }

        // 4. Handle android-app:// links (e.g. android-app://com.android.chrome/https/www.google.com/search?q=...)
        if (trimmed.startsWith("android-app://", ignoreCase = true)) {
            val afterHost = trimmed.removePrefix("android-app://").substringAfter('/', "")
            if (afterHost.startsWith("https/", ignoreCase = true)) {
                return cleanExtractedWebUrl("https://" + afterHost.removePrefix("https/").removePrefix("https://"))
            } else if (afterHost.startsWith("http/", ignoreCase = true)) {
                return cleanExtractedWebUrl("http://" + afterHost.removePrefix("http/").removePrefix("http://"))
            }
        }

        // 5. Handle intent: URIs
        if (trimmed.startsWith("intent:", ignoreCase = true)) {
            // First attempt: Android platform intent parser
            try {
                val intent = Intent.parseUri(trimmed, Intent.URI_INTENT_SCHEME)

                // Direct web data URI
                val dataStr = intent.dataString
                if (!dataStr.isNullOrBlank() && (dataStr.startsWith("http://", ignoreCase = true) || dataStr.startsWith("https://", ignoreCase = true))) {
                    return dataStr
                }

                // Scheme is http or https
                val scheme = intent.scheme
                if (scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true)) {
                    val dataUri = intent.data
                    if (dataUri != null) {
                        return dataUri.toString()
                    }
                }

                // browser_fallback_url extra
                val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                if (!fallbackUrl.isNullOrBlank() && (fallbackUrl.startsWith("http://", ignoreCase = true) || fallbackUrl.startsWith("https://", ignoreCase = true))) {
                    return fallbackUrl
                }

                // Extra "url" or "target_url"
                val extraUrl = intent.getStringExtra("url") ?: intent.getStringExtra("target_url")
                if (!extraUrl.isNullOrBlank() && (extraUrl.startsWith("http://", ignoreCase = true) || extraUrl.startsWith("https://", ignoreCase = true))) {
                    return extraUrl
                }
            } catch (_: Throwable) {
            }

            // Fallback robust string extraction for intent URIs
            // e.g. intent://www.google.com/search?q=test#Intent;scheme=https;package=com.android.chrome;end
            if (trimmed.contains("#Intent;")) {
                val fragment = trimmed.substringAfter("#Intent;")

                // Check S.browser_fallback_url in fragment
                if (fragment.contains("browser_fallback_url=")) {
                    val raw = fragment.substringAfter("browser_fallback_url=").substringBefore(';')
                    val decoded = decodeUrl(raw)
                    if (decoded.startsWith("http://", ignoreCase = true) || decoded.startsWith("https://", ignoreCase = true)) {
                        return decoded
                    }
                }

                // Check scheme in fragment
                val schemeMatch = Regex("scheme=([a-zA-Z]+);").find(fragment)?.groupValues?.getOrNull(1)
                if (schemeMatch.equals("http", ignoreCase = true) || schemeMatch.equals("https", ignoreCase = true)) {
                    val hostAndPath = trimmed.substringBefore("#Intent;").removePrefix("intent://").removePrefix("intent:")
                    if (hostAndPath.isNotBlank()) {
                        return "$schemeMatch://$hostAndPath"
                    }
                }
            }

            // Direct intent:http or intent:https
            if (trimmed.startsWith("intent:http://", ignoreCase = true)) {
                return trimmed.substringBefore("#Intent;").removePrefix("intent:")
            }
            if (trimmed.startsWith("intent:https://", ignoreCase = true)) {
                return trimmed.substringBefore("#Intent;").removePrefix("intent:")
            }
        }

        return null
    }

    private fun decodeUrl(value: String): String {
        return try {
            java.net.URLDecoder.decode(value, "UTF-8")
        } catch (_: Exception) {
            try {
                Uri.decode(value) ?: value
            } catch (_: Exception) {
                value
            }
        }
    }

    private fun cleanExtractedWebUrl(raw: String, defaultScheme: String = "http"): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            return trimmed
        }
        return "$defaultScheme://$trimmed"
    }

    /**
     * Parses an intent for non-browser native applications (e.g. tel, mailto, sms, maps, whatsapp, upi).
     * Strictly blocks launching external browsers (Chrome, Google App, etc.) so browsing stays in Velocity.
     */
    fun parseSafeIntent(context: Context, uriString: String): Intent? {
        return try {
            val intent = Intent.parseUri(uriString, Intent.URI_INTENT_SCHEME)

            // Never allow an intent targeting Chrome or external web browsers
            val targetPackage = intent.`package`
            if (isBrowserPackage(targetPackage)) {
                return null
            }

            // Never launch web schemes through external apps
            val scheme = intent.scheme?.lowercase()
            if (scheme == "http" || scheme == "https") {
                return null
            }

            val dataStr = intent.dataString?.lowercase()
            if (dataStr != null && (dataStr.startsWith("http://") || dataStr.startsWith("https://"))) {
                return null
            }

            // Strip explicit components to prevent launching unexported internal components
            intent.component = null
            intent.selector = null
            intent.addCategory(Intent.CATEGORY_BROWSABLE)

            // Verify that an activity exists on device to handle this intent
            val resolveInfo = context.packageManager.resolveActivity(intent, 0) ?: return null
            val resolvedPackage = resolveInfo.activityInfo?.packageName
            if (isBrowserPackage(resolvedPackage) || resolvedPackage.equals("android", ignoreCase = true)) {
                return null
            }

            intent.`package` = resolvedPackage
            intent
        } catch (_: Exception) {
            null
        }
    }
}
