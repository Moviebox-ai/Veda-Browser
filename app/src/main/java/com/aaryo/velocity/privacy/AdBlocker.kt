package com.aaryo.velocity.privacy

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import android.webkit.WebView
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

    // 1x1 transparent PNG byte array for blocking image ads without showing broken image icons
    private val TRANSPARENT_1X1_PNG = byteArrayOf(
        0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0D.toByte(), 0x49.toByte(), 0x48.toByte(), 0x44.toByte(), 0x52.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(),
        0x08.toByte(), 0x06.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x1F.toByte(), 0x15.toByte(), 0xC4.toByte(),
        0x89.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0A.toByte(), 0x49.toByte(), 0x44.toByte(), 0x41.toByte(),
        0x54.toByte(), 0x78.toByte(), 0x9C.toByte(), 0x63.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x05.toByte(), 0x00.toByte(), 0x01.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x2D.toByte(), 0xB4.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x49.toByte(), 0x45.toByte(), 0x4E.toByte(), 0x44.toByte(), 0xAE.toByte(),
        0x42.toByte(), 0x60.toByte(), 0x82.toByte()
    )

    // Cosmetic Ad Blocking CSS to hide ad containers, banners, slots, and sponsored elements
    private val COSMETIC_AD_HIDE_CSS = """
        .adsbygoogle,
        [id^="google_ads_"],
        [id^="div-gpt-ad"],
        [id*="-ad-"],
        [class*="-ad-"],
        [class*="ad-container"],
        [class*="ad-wrapper"],
        [class*="ad-slot"],
        [class*="ad-banner"],
        [class*="ad_banner"],
        [class*="ad-unit"],
        [class*="ad_unit"],
        [class*="sponsored-post"],
        [class*="sponsored_post"],
        [class*="taboola-"],
        [class*="outbrain-"],
        [data-ad-slot],
        [data-ad-client],
        [data-ad-unit],
        [data-ad],
        .ad-placement,
        .advertisement,
        .advert,
        .ad-banner,
        .ad_box,
        .ad-box,
        .ad-container,
        .ad-holder,
        .ad-space,
        .ad-wrapper,
        .bottom-ad,
        .top-ad,
        .sidebar-ad,
        .header-ad,
        .footer-ad,
        .sticky-ad,
        .floating-ad,
        .banner-ad,
        .native-ad,
        .sponsor-post,
        .sponsored-content,
        #ad-banner,
        #ad-header,
        #ad-footer,
        #ad-sidebar,
        #ad-container,
        #ad-wrapper,
        iframe[src*="doubleclick"],
        iframe[src*="googlesyndication"],
        iframe[src*="adservice"],
        iframe[src*="amazon-adsystem"],
        iframe[src*="taboola"],
        iframe[src*="outbrain"],
        iframe[src*="adnxs"],
        iframe[id*="google_ads"] {
            display: none !important;
            visibility: hidden !important;
            height: 0px !important;
            min-height: 0px !important;
            max-height: 0px !important;
            padding: 0px !important;
            margin: 0px !important;
            opacity: 0 !important;
            pointer-events: none !important;
        }
    """.trimIndent().replace("\n", " ")

    @Volatile
    var isStrictMode: Boolean = false

    @Volatile
    private var isInitialized = false

    init {
        // Populate core default domains & path patterns immediately so ad blocking works from the first millisecond
        populateDefaultDomains()
        populateDefaultPathPatterns()
    }

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
                        if (line.isEmpty() || line.startsWith("#") || line.startsWith("!") || line.startsWith("[")) {
                            continue
                        }

                        // Remove rule modifiers like $third-party, $script, $image
                        val ruleWithoutModifiers = if (line.contains("$")) line.substringBefore("$") else line

                        if (ruleWithoutModifiers.startsWith("||")) {
                            var clean = ruleWithoutModifiers.removePrefix("||")
                            if (clean.endsWith("^")) {
                                clean = clean.substring(0, clean.length - 1)
                            }
                            if (clean.contains("/")) {
                                val domainPart = clean.substringBefore("/").lowercase()
                                val pathPart = clean.substringAfter("/").lowercase()
                                if (domainPart.isNotEmpty() && !LOGIN_CRITICAL_DOMAINS.contains(domainPart)) {
                                    blockedDomains.add(domainPart)
                                }
                                if (pathPart.isNotEmpty()) {
                                    blockedPathPatterns.add(pathPart)
                                }
                            } else {
                                val domain = clean.lowercase()
                                if (!LOGIN_CRITICAL_DOMAINS.contains(domain)) {
                                    blockedDomains.add(domain)
                                }
                            }
                        } else if (ruleWithoutModifiers.startsWith("/")) {
                            val pattern = ruleWithoutModifiers.trim('/').lowercase()
                            if (pattern.isNotEmpty()) {
                                blockedPathPatterns.add(pattern)
                            }
                        } else {
                            val clean = ruleWithoutModifiers.trim('^', '/').lowercase()
                            if (clean.contains(".") && !clean.contains("/")) {
                                blockedDomains.add(clean)
                            } else if (clean.isNotEmpty()) {
                                blockedPathPatterns.add(clean)
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Already initialized with comprehensive defaults in init
        }
    }

    private fun populateDefaultDomains() {
        val defaults = listOf(
            // Google Ads, DoubleClick, Syndication & Analytics
            "doubleclick.net",
            "googleadservices.com",
            "googlesyndication.com",
            "adservice.google.com",
            "pagead2.googlesyndication.com",
            "googletagservices.com",
            "googletagmanager.com",
            "google-analytics.com",
            "analytics.google.com",
            "admob.com",
            "ads.google.com",
            "googleads.g.doubleclick.net",
            "securepubads.g.doubleclick.net",
            "adclick.g.doubleclick.net",
            "stats.g.doubleclick.net",
            "tpc.googlesyndication.com",
            "partner.googleadservices.com",

            // Major Display, Header Bidding & Exchange Networks
            "adnxs.com",
            "adnxs-simple.com",
            "criteo.com",
            "criteo.net",
            "static.criteo.net",
            "taboola.com",
            "outbrain.com",
            "scorecardresearch.com",
            "moatads.com",
            "amazon-adsystem.com",
            "aax-us-east.amazon-adsystem.com",
            "rubiconproject.com",
            "pubmatic.com",
            "openx.net",
            "casalemedia.com",
            "yieldmo.com",
            "smartadserver.com",
            "media.net",
            "sovrn.com",
            "lijit.com",
            "sharethrough.com",
            "sonobi.com",
            "spotxchange.com",
            "teads.tv",
            "tribalfusion.com",
            "undertone.com",
            "adblade.com",
            "adbutler.com",
            "adform.net",
            "adfox.ru",
            "adgrx.com",
            "adition.com",
            "adlooxtracking.com",
            "admanmedia.com",
            "adriver.ru",
            "adsafeprotected.com",
            "adserver.org",
            "adspirit.de",
            "adsrvr.org",
            "adsystem.com",
            "adtarget.io",
            "adtech.de",
            "adtechus.com",
            "advertising.com",
            "advertserve.com",
            "adroll.com",
            "adroll.net",
            "bidswitch.net",
            "contextweb.com",
            "exponential.com",
            "mathtag.com",
            "mgid.com",
            "revcontent.com",
            "zergnet.com",
            "indexexchange.com",
            "gumgum.com",
            "kargo.com",
            "liveintent.com",
            "connatix.com",
            "aniview.com",

            // Mobile, In-App & Video Ad Networks
            "applovin.com",
            "unityads.unity3d.com",
            "chartboost.com",
            "inmobi.com",
            "vungle.com",
            "ironsrc.com",
            "adcolony.com",
            "pangle.io",
            "mintegral.com",
            "fyber.com",
            "tapjoy.com",
            "mobfox.com",

            // Pop-under, Interstitial & High-Frequency Ad Networks
            "popads.net",
            "popcash.net",
            "propellerads.com",
            "exoclick.com",
            "trafficjunky.com",
            "trafficjunky.net",
            "adsterra.com",
            "richads.com",
            "trafficstars.com",
            "monetag.com",
            "hilltopads.com",
            "clickadu.com",
            "adcash.com",
            "yllix.com",
            "bidvertiser.com",
            "adkernel.com",
            "adpushup.com",
            "adtrue.com",
            "bet365affiliates.com",

            // Analytics, Tracking & Session Recorders
            "flurry.com",
            "appsflyer.com",
            "amplitude.com",
            "mixpanel.com",
            "hotjar.com",
            "mouseflow.com",
            "crazyegg.com",
            "clarity.ms",
            "optimizely.com",
            "quantserve.com",
            "quantcount.com",
            "newrelic.com",
            "crwdcntrl.net",
            "demdex.net",
            "statcounter.com",
            "histats.com",
            "chartbeat.com",
            "an.yandex.ru",
            "mc.yandex.ru"
        )
        blockedDomains.addAll(defaults)
    }

    private fun populateDefaultPathPatterns() {
        val patterns = listOf(
            "pagead/js",
            "adsbygoogle",
            "gpt.js",
            "gtm.js",
            "ga.js",
            "fbevents.js",
            "prebid.js",
            "adserver",
            "advertising.js",
            "sponsor-banners",
            "ad-frame",
            "show_ads.js",
            "ad_box",
            "ad-loader",
            "popunder",
            "popupad",
            "advertisement.js",
            "banner_ad"
        )
        blockedPathPatterns.addAll(patterns)
    }

    /**
     * Checks if a given host matches or is a subdomain of any blocked domain.
     * Uses O(1) domain suffix hierarchy inspection instead of linear search over the entire database.
     */
    private fun isHostBlocked(normalizedHost: String): Boolean {
        var checkHost = normalizedHost
        while (checkHost.isNotEmpty()) {
            if (blockedDomains.contains(checkHost)) {
                return true
            }
            val dotIndex = checkHost.indexOf('.')
            if (dotIndex == -1) break
            checkHost = checkHost.substring(dotIndex + 1)
        }
        return false
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

        val requestHost = try {
            java.net.URI(url).host?.lowercase()
        } catch (_: Exception) {
            null
        } ?: try {
            val afterScheme = if (url.contains("://")) url.substringAfter("://") else url
            val rawHost = afterScheme.substringBefore('/').substringBefore('?').substringBefore(':')
            rawHost.trim().lowercase().ifBlank { null }
        } catch (_: Exception) {
            null
        } ?: return false

        val normalizedHost = requestHost.removePrefix("www.")

        // 2. Prevent breaking logins for essential OAuth/Identity providers
        if (!isStrictMode && isLoginCritical(normalizedHost)) {
            return false
        }

        // 3. Fast O(1) Suffix & Exact Domain Lookup
        if (isHostBlocked(normalizedHost)) {
            return true
        }

        // 4. URL path pattern match
        val path = try {
            java.net.URI(url).path?.lowercase() ?: ""
        } catch (_: Exception) {
            val afterScheme = if (url.contains("://")) url.substringAfter("://") else url
            val rawPath = afterScheme.substringAfter('/', "")
            if (rawPath.isNotEmpty()) "/${rawPath.substringBefore('?')}" else ""
        }

        for (pattern in blockedPathPatterns) {
            if (path.contains(pattern)) {
                return true
            }
        }

        // 5. Query / full URL keyword match for ubiquitous ad engines
        val lowerUrl = url.lowercase()
        if (lowerUrl.contains("adsbygoogle") ||
            lowerUrl.contains("/pagead/") ||
            lowerUrl.contains("securepubads") ||
            lowerUrl.contains("amazon-adsystem") ||
            lowerUrl.contains("doubleclick.net") ||
            lowerUrl.contains("googlesyndication.com")
        ) {
            return true
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

    /**
     * Creates an appropriate empty response based on the requested resource type
     * (e.g. 1x1 transparent PNG for image ads, no-op script for JS, empty CSS for styles).
     */
    fun createEmptyResponse(url: String? = null): WebResourceResponse {
        val lowerUrl = url?.lowercase() ?: ""

        val (mimeType, dataBytes) = when {
            lowerUrl.endsWith(".png") || lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg") ||
            lowerUrl.endsWith(".gif") || lowerUrl.endsWith(".webp") || lowerUrl.endsWith(".svg") ||
            lowerUrl.contains("/image") || lowerUrl.contains("/ad-img") -> {
                "image/png" to TRANSPARENT_1X1_PNG
            }
            lowerUrl.endsWith(".js") || lowerUrl.contains("/js") || lowerUrl.contains("script") -> {
                "application/javascript" to "/* blocked by velocity shield */".toByteArray(Charsets.UTF_8)
            }
            lowerUrl.endsWith(".css") || lowerUrl.contains("style") -> {
                "text/css" to "/* blocked */".toByteArray(Charsets.UTF_8)
            }
            lowerUrl.endsWith(".html") || lowerUrl.endsWith(".htm") -> {
                "text/html" to "<!-- blocked -->".toByteArray(Charsets.UTF_8)
            }
            else -> {
                "text/plain" to ByteArray(0)
            }
        }

        return WebResourceResponse(mimeType, "UTF-8", ByteArrayInputStream(dataBytes)).apply {
            responseHeaders = mapOf(
                "Access-Control-Allow-Origin" to "*",
                "Cache-Control" to "no-cache, no-store, must-revalidate"
            )
        }
    }

    /**
     * Injects cosmetic ad-hiding CSS and defuses anti-adblock detection scripts.
     * Can be invoked at document start, page finished, or progress events.
     */
    fun injectCosmeticAdBlock(webView: WebView) {
        val script = """
            (function() {
                try {
                    // 1. Defuse anti-adblock checks and mock ad globals so websites don't break
                    window.canRunAds = true;
                    window.isAdBlockActive = false;
                    window.adblock = false;
                    if (!window.adsbygoogle) {
                        window.adsbygoogle = { loaded: true, push: function() {} };
                    }

                    // 2. Inject global cosmetic ad-hiding CSS style element
                    var styleId = 'velocity-shield-ad-hide';
                    if (!document.getElementById(styleId)) {
                        var style = document.createElement('style');
                        style.id = styleId;
                        style.textContent = '$COSMETIC_AD_HIDE_CSS';
                        (document.head || document.documentElement).appendChild(style);
                    }

                    // 3. Actively collapse any visible ad frames and placeholder elements
                    function collapseAdElements() {
                        var selectors = [
                            '.adsbygoogle', '[id^="google_ads_"]', '[id^="div-gpt-ad"]',
                            '[data-ad-slot]', '[data-ad-client]', '.ad-banner', '.ad-container',
                            '.ad-wrapper', '.ad-slot', '.advertisement', '.taboola-placeholder',
                            '.outbrain-placeholder', '[class*="sponsored-post"]',
                            'iframe[src*="doubleclick"]', 'iframe[src*="googlesyndication"]',
                            'iframe[id*="google_ads"]', '.ad-box', '.ad_box', '#ad-banner'
                        ];
                        var nodes = document.querySelectorAll(selectors.join(','));
                        for (var i = 0; i < nodes.length; i++) {
                            nodes[i].style.setProperty('display', 'none', 'important');
                            nodes[i].style.setProperty('height', '0px', 'important');
                            nodes[i].style.setProperty('min-height', '0px', 'important');
                            nodes[i].style.setProperty('visibility', 'hidden', 'important');
                        }
                    }

                    collapseAdElements();
                    if (document.readyState === 'loading') {
                        document.addEventListener('DOMContentLoaded', collapseAdElements);
                    }
                    window.addEventListener('load', collapseAdElements);

                    // 4. Watch for dynamically inserted ad containers (infinite scroll, AJAX)
                    if (window.MutationObserver && !window._velocityObserverAttached) {
                        window._velocityObserverAttached = true;
                        var observer = new MutationObserver(collapseAdElements);
                        if (document.body) {
                            observer.observe(document.body, { childList: true, subtree: true });
                        } else {
                            document.addEventListener('DOMContentLoaded', function() {
                                if (document.body) {
                                    observer.observe(document.body, { childList: true, subtree: true });
                                }
                            });
                        }
                    }
                } catch(e) {}
            })();
        """.trimIndent()

        webView.evaluateJavascript(script, null)
    }
}

