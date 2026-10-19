package com.aaryo.velocity.privacy

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

object AdBlocker {

    private val blockedDomains = ConcurrentHashMap.newKeySet<String>()
    private val blockedPathPatterns = ConcurrentHashMap.newKeySet<String>()
    private val allowlist = ConcurrentHashMap.newKeySet<String>()

    // Login-critical domains that must NEVER be blocked in standard mode to prevent broken logins
    private val LOGIN_CRITICAL_DOMAINS = setOf(
        "facebook.net",
        "connect.facebook.net",
        "branch.io",
        "segment.com",
        "segment.io",
        "accounts.google.com",
        "appleid.apple.com",
        "github.com",
        "twitter.com"
    )

    @Volatile
    var isStrictMode: Boolean = false

    @Volatile
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        CoroutineScope(Dispatchers.IO).launch {
            loadRulesFromAssets(context)
            isInitialized = true
        }
    }

    private fun loadRulesFromAssets(context: Context) {
        try {
            context.assets.open("easylist_subset.txt").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                    for (rawLine in lines) {
                        val line = rawLine.trim()
                        if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) {
                            continue
                        }

                        if (line.startsWith("||") && line.endsWith("^")) {
                            val domain = line.substring(2, line.length - 1).lowercase()
                            if (!LOGIN_CRITICAL_DOMAINS.contains(domain)) {
                                blockedDomains.add(domain)
                            }
                        } else if (line.startsWith("/") && line.endsWith("/")) {
                            val pattern = line.substring(1, line.length - 1).lowercase()
                            blockedPathPatterns.add(pattern)
                        } else if (!line.startsWith("[")) {
                            blockedDomains.add(line.lowercase())
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback in case of asset read error
            populateDefaultDomains()
        }
    }

    private fun populateDefaultDomains() {
        val defaults = listOf(
            "doubleclick.net",
            "googleadservices.com",
            "googlesyndication.com",
            "adservice.google.com",
            "pagead2.googlesyndication.com",
            "adnxs.com",
            "criteo.com",
            "taboola.com",
            "outbrain.com",
            "scorecardresearch.com",
            "moatads.com"
        )
        blockedDomains.addAll(defaults)
    }

    fun isAdOrTracker(url: String?, siteHost: String? = null): Boolean {
        if (url.isNullOrEmpty()) return false

        // 1. Check site allowlist
        if (!siteHost.isNullOrEmpty()) {
            val normalizedSite = siteHost.lowercase().removePrefix("www.")
            if (isAllowlisted(normalizedSite)) {
                return false
            }
        }

        val uri = try {
            Uri.parse(url)
        } catch (_: Exception) {
            return false
        }

        val requestHost = uri.host?.lowercase() ?: return false
        val normalizedHost = requestHost.removePrefix("www.")

        // 2. Prevent breaking logins for essential OAuth/Identity providers
        if (!isStrictMode && isLoginCritical(normalizedHost)) {
            return false
        }

        // 3. Exact or suffix domain match against EasyList database
        if (blockedDomains.contains(normalizedHost)) return true
        for (blocked in blockedDomains) {
            if (normalizedHost.endsWith(".$blocked")) {
                return true
            }
        }

        // 4. URL path pattern match
        val path = uri.path?.lowercase() ?: ""
        for (pattern in blockedPathPatterns) {
            if (path.contains(pattern)) {
                return true
            }
        }

        return false
    }

    private fun isLoginCritical(host: String): Boolean {
        for (critical in LOGIN_CRITICAL_DOMAINS) {
            if (host == critical || host.endsWith(".$critical")) {
                return true
            }
        }
        return false
    }

    fun addToAllowlist(domain: String) {
        val clean = domain.trim().lowercase().removePrefix("www.")
        if (clean.isNotEmpty()) {
            allowlist.add(clean)
        }
    }

    fun removeFromAllowlist(domain: String) {
        val clean = domain.trim().lowercase().removePrefix("www.")
        allowlist.remove(clean)
    }

    fun isAllowlisted(domain: String): Boolean {
        val clean = domain.trim().lowercase().removePrefix("www.")
        return allowlist.contains(clean)
    }

    fun getAllowlist(): Set<String> = allowlist.toSet()

    fun createEmptyResponse(): WebResourceResponse {
        val emptyStream = ByteArrayInputStream(ByteArray(0))
        return WebResourceResponse("text/plain", "UTF-8", emptyStream).apply {
            responseHeaders = mapOf(
                "Access-Control-Allow-Origin" to "*",
                "Cache-Control" to "no-cache, no-store, must-revalidate"
            )
        }
    }
}
