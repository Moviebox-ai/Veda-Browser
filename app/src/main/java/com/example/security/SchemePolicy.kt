package com.example.security

import java.util.Locale

/**
 * Enforces scheme validation and boundaries for Velocity Browser.
 */
object SchemePolicy {

    private val WEB_SCHEMES = setOf("http", "https")
    private val INTERNAL_SCHEMES = setOf("velocity", "about")
    private val SAFE_EXTERNAL_SCHEMES = setOf("tel", "mailto", "sms", "geo", "market")
    private val FORBIDDEN_SCHEMES = setOf("javascript", "file", "content", "jar", "data", "blob")

    fun isWebScheme(scheme: String?): Boolean {
        if (scheme == null) return false
        return WEB_SCHEMES.contains(scheme.lowercase(Locale.ROOT))
    }

    fun isInternalScheme(scheme: String?): Boolean {
        if (scheme == null) return false
        return INTERNAL_SCHEMES.contains(scheme.lowercase(Locale.ROOT))
    }

    fun isSafeExternalScheme(scheme: String?): Boolean {
        if (scheme == null) return false
        return SAFE_EXTERNAL_SCHEMES.contains(scheme.lowercase(Locale.ROOT))
    }

    fun isForbidden(scheme: String?): Boolean {
        if (scheme == null) return true
        return FORBIDDEN_SCHEMES.contains(scheme.lowercase(Locale.ROOT))
    }
}
