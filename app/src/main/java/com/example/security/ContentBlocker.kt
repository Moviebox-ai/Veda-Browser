package com.example.security

import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/**
 * High-speed URL-based tracker and ad blocker for Velocity Browser.
 * Intercepts requests using efficient set lookup without decrypting traffic.
 */
class ContentBlocker {

    var isEnabled: Boolean = true

    private val blockedCount = AtomicInteger(0)

    val totalBlockedCount: Int
        get() = blockedCount.get()

    private val blockedDomains: Set<String> = hashSetOf(
        "doubleclick.net",
        "google-analytics.com",
        "googlesyndication.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        "scorecardresearch.com",
        "adnxs.com",
        "taboola.com",
        "outbrain.com",
        "criteo.com",
        "advertising.com",
        "quantserve.com",
        "analytics.twitter.com",
        "connect.facebook.net/signals",
        "adroll.com",
        "moatads.com",
        "popads.net",
        "smartadserver.com",
        "rubiconproject.com",
        "openx.net",
        "casale-media.com",
        "pubmatic.com",
        "yieldmo.com",
        "tribalfusion.com",
        "trackersim.com"
    )

    fun shouldBlock(uri: Uri): Boolean {
        if (!isEnabled) return false
        val host = uri.host?.lowercase(Locale.ROOT) ?: return false

        for (blocked in blockedDomains) {
            if (host == blocked || host.endsWith(".$blocked")) {
                blockedCount.incrementAndGet()
                return true
            }
        }
        return false
    }

    fun createBlockedResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            204,
            "No Content",
            emptyMap(),
            ByteArrayInputStream(ByteArray(0))
        )
    }

    fun resetCount() {
        blockedCount.set(0)
    }
}
