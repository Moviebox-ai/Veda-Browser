package com.example.velocity.privacy

import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

object AdBlocker {

    private val BLOCKED_DOMAINS = hashSetOf(
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        "securepubads.g.doubleclick.net",
        "ads.google.com",
        "admob.com",
        "adservice.google.ca",
        "adservice.google.co.uk",
        "facebook.net",
        "connect.facebook.net",
        "an.facebook.com",
        "ads-twitter.com",
        "static.ads-twitter.com",
        "analytics.twitter.com",
        "adzerk.net",
        "criteo.com",
        "static.criteo.net",
        "criteo.net",
        "taboola.com",
        "outbrain.com",
        "trafficjunior.com",
        "scorecardresearch.com",
        "moatads.com",
        "adnxs.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "casalemedia.com",
        "yieldmo.com",
        "smartadserver.com",
        "applovin.com",
        "unityads.unity3d.com",
        "chartboost.com",
        "flurry.com",
        "branch.io",
        "appsflyer.com",
        "amplitude.com",
        "mixpanel.com",
        "hotjar.com",
        "mouseflow.com",
        "crazyegg.com",
        "segment.com",
        "segment.io",
        "optimizely.com",
        "quantserve.com",
        "quantcount.com",
        "newrelic.com",
        "adcolony.com",
        "ironsrc.com",
        "vungle.com",
        "inmobi.com",
        "mgid.com",
        "revcontent.com",
        "popads.net",
        "popcash.net",
        "propellerads.com",
        "exoclick.com",
        "zergnet.com",
        "adtechus.com",
        "advertising.com",
        "adroll.com",
        "adroll.net",
        "bidswitch.net",
        "contextweb.com",
        "exponential.com",
        "lijit.com",
        "mathtag.com",
        "media.net",
        "sharethrough.com",
        "sonobi.com",
        "spotxchange.com",
        "teads.tv",
        "tribalfusion.com",
        "undertone.com"
    )

    fun isAdOrTracker(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        val uri = try {
            Uri.parse(url)
        } catch (_: Exception) {
            return false
        }

        val host = uri.host?.lowercase() ?: return false

        // Check exact match or suffix subdomain match
        if (BLOCKED_DOMAINS.contains(host)) return true

        for (domain in BLOCKED_DOMAINS) {
            if (host.endsWith(".$domain")) {
                return true
            }
        }

        // Common ad keywords in path/query for aggressive heuristic
        val path = uri.path?.lowercase() ?: ""
        if (path.contains("/pagead/") || path.contains("/adserver/") || path.contains("/ads.js") || path.contains("/prebid.js")) {
            return true
        }

        return false
    }

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
