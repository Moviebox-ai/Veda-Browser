package com.aaryo.vedabrowser.security

import android.net.Uri

object SchemePolicy {

    private val WEB_SCHEMES = setOf("http", "https")
    private val SAFE_INTERNAL_SCHEMES = setOf("about", "javascript", "data", "blob", "velocity")
    private val EXTERNAL_INTENT_SCHEMES = setOf("mailto", "tel", "sms", "geo", "intent", "market", "whatsapp")
    private val BROWSER_CUSTOM_SCHEMES = setOf("googlechrome", "googlechromes", "chromenative", "chrome", "chrome-search", "android-app")
    private val BLOCKED_SCHEMES = setOf("file", "content")

    private fun extractScheme(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val colonIdx = url.indexOf(':')
        if (colonIdx <= 0) return null
        val candidate = url.substring(0, colonIdx).trim().lowercase()
        return if (candidate.all { it.isLetterOrDigit() || it == '+' || it == '.' || it == '-' }) {
            candidate
        } else {
            null
        }
    }

    fun isWebScheme(url: String?): Boolean {
        val scheme = extractScheme(url) ?: return false
        return scheme in WEB_SCHEMES
    }

    fun isExternalScheme(url: String?): Boolean {
        val scheme = extractScheme(url) ?: return false
        return scheme in EXTERNAL_INTENT_SCHEMES && scheme !in BROWSER_CUSTOM_SCHEMES
    }

    fun isBrowserCustomScheme(url: String?): Boolean {
        val scheme = extractScheme(url) ?: return false
        return scheme in BROWSER_CUSTOM_SCHEMES
    }

    fun isSafeInternal(url: String?): Boolean {
        val scheme = extractScheme(url) ?: return false
        return scheme in SAFE_INTERNAL_SCHEMES
    }

    fun isBlockedScheme(url: String?): Boolean {
        val scheme = extractScheme(url) ?: return false
        return scheme in BLOCKED_SCHEMES
    }
}
