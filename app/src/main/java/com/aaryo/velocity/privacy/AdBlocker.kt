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
import java.util.concurrent.CopyOnWriteArrayList

enum class ResourceType {
    SCRIPT,
    IMAGE,
    STYLESHEET,
    SUBDOCUMENT,
    XMLHTTPREQUEST,
    OTHER
}

data class FilterRule(
    val raw: String,
    val isException: Boolean,
    val anchorDomain: String? = null,
    val regex: Regex? = null,
    val thirdPartyOnly: Boolean = false,
    val firstPartyOnly: Boolean = false,
    val resourceTypes: Set<ResourceType>? = null
) {
    fun matches(
        url: String,
        requestHost: String,
        siteHost: String?,
        resourceType: ResourceType
    ): Boolean {
        if (thirdPartyOnly && siteHost != null && !AdBlocker.isThirdParty(requestHost, siteHost)) {
            return false
        }
        if (firstPartyOnly && siteHost != null && AdBlocker.isThirdParty(requestHost, siteHost)) {
            return false
        }
        if (resourceTypes != null && !resourceTypes.contains(resourceType)) {
            return false
        }
        if (anchorDomain != null && !AdBlocker.matchesDomain(requestHost, anchorDomain)) {
            return false
        }
        if (regex != null && !regex.containsMatchIn(url)) {
            return false
        }
        return true
    }
}

object AdBlocker {

    private val blockedDomains = ConcurrentHashMap.newKeySet<String>()
    private val exceptionDomains = ConcurrentHashMap.newKeySet<String>()
    private val allowlist = ConcurrentHashMap.newKeySet<String>()
    private val cosmeticSelectors = ConcurrentHashMap.newKeySet<String>()

    // Indexed complex rules
    private val anchoredRules = ConcurrentHashMap<String, CopyOnWriteArrayList<FilterRule>>()
    private val globalRules = CopyOnWriteArrayList<FilterRule>()

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
        "twitter.com",
        "x.com"
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

    // Base built-in cosmetic selectors
    private val DEFAULT_COSMETIC_SELECTORS = listOf(
        ".adsbygoogle",
        "[id^=\"google_ads_\"]",
        "[id^=\"div-gpt-ad\"]",
        "[id*=\"-ad-\"]",
        "[class*=\"-ad-\"]",
        "[class*=\"ad-container\"]",
        "[class*=\"ad-wrapper\"]",
        "[class*=\"ad-slot\"]",
        "[class*=\"ad-banner\"]",
        "[class*=\"ad_banner\"]",
        "[class*=\"ad-unit\"]",
        "[class*=\"ad_unit\"]",
        "[class*=\"sponsored-post\"]",
        "[class*=\"sponsored_post\"]",
        "[class*=\"taboola-\"]",
        "[class*=\"outbrain-\"]",
        "[data-ad-slot]",
        "[data-ad-client]",
        "[data-ad-unit]",
        "[data-ad]",
        ".ad-placement",
        ".advertisement",
        ".advert",
        ".ad-banner",
        ".ad_box",
        ".ad-box",
        ".ad-container",
        ".ad-holder",
        ".ad-space",
        ".ad-wrapper",
        ".bottom-ad",
        ".top-ad",
        ".sidebar-ad",
        ".header-ad",
        ".footer-ad",
        ".sticky-ad",
        ".floating-ad",
        ".banner-ad",
        ".native-ad",
        ".sponsor-post",
        ".sponsored-content",
        "#ad-banner",
        "#ad-header",
        "#ad-footer",
        "#ad-sidebar",
        "#ad-container",
        "#ad-wrapper",
        "iframe[src*=\"doubleclick\"]",
        "iframe[src*=\"googlesyndication\"]",
        "iframe[src*=\"adservice\"]",
        "iframe[src*=\"amazon-adsystem\"]",
        "iframe[src*=\"taboola\"]",
        "iframe[src*=\"outbrain\"]",
        "iframe[src*=\"adnxs\"]",
        "iframe[src*=\"clmbtech\"]",
        "iframe[id*=\"google_ads\"]"
    )

    @Volatile
    var isStrictMode: Boolean = false

    @Volatile
    private var isInitialized = false

    init {
        populateDefaultDomains()
        cosmeticSelectors.addAll(DEFAULT_COSMETIC_SELECTORS)
    }

    fun initialize(context: Context) {
        if (isInitialized) return
        CoroutineScope(Dispatchers.IO).launch {
            loadRulesFromAssets(context)
            isInitialized = true
        }
    }

    fun loadRulesFromAssets(context: Context) {
        try {
            context.assets.open("easylist_subset.txt").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                    for (rawLine in lines) {
                        parseRuleLine(rawLine)
                    }
                }
            }
        } catch (_: Exception) {
            // Safe fallback to defaults in init
        }
    }

    fun parseRuleLine(rawLine: String) {
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("!") || line.startsWith("[")) {
            return
        }
        if (line.startsWith("#") && !line.startsWith("##")) {
            return
        }

        // 1. Cosmetic rules (##)
        if (line.contains("##")) {
            val selector = line.substringAfter("##").trim()
            if (selector.isNotEmpty()) {
                cosmeticSelectors.add(selector)
            }
            return
        }

        // 2. Exception rules (@@)
        val isException = line.startsWith("@@")
        val cleanLine = if (isException) line.removePrefix("@@").trim() else line

        // 3. Modifiers ($)
        val patternPart = if (cleanLine.contains("$")) cleanLine.substringBefore("$").trim() else cleanLine
        val modifiersPart = if (cleanLine.contains("$")) cleanLine.substringAfter("$").trim() else ""

        var thirdPartyOnly = false
        var firstPartyOnly = false
        val resourceTypes = mutableSetOf<ResourceType>()

        if (modifiersPart.isNotEmpty()) {
            val mods = modifiersPart.split(',')
            for (mod in mods) {
                val m = mod.trim().lowercase()
                when (m) {
                    "third-party", "3p" -> thirdPartyOnly = true
                    "~third-party", "~3p" -> firstPartyOnly = true
                    "script" -> resourceTypes.add(ResourceType.SCRIPT)
                    "image" -> resourceTypes.add(ResourceType.IMAGE)
                    "stylesheet", "css" -> resourceTypes.add(ResourceType.STYLESHEET)
                    "subdocument" -> resourceTypes.add(ResourceType.SUBDOCUMENT)
                    "xmlhttprequest", "xhr" -> resourceTypes.add(ResourceType.XMLHTTPREQUEST)
                }
            }
        }

        val resTypes = if (resourceTypes.isNotEmpty()) resourceTypes.toSet() else null

        // 4. Simple domain rules without paths or wildcards -> Store directly for O(1) lookup
        if (patternPart.startsWith("||") && !patternPart.contains("/") && !patternPart.contains("*")) {
            val rawDomain = patternPart.removePrefix("||").removeSuffix("^").trim().lowercase()
            if (rawDomain.isNotEmpty()) {
                if (isException) {
                    exceptionDomains.add(rawDomain)
                } else if (resTypes == null && !thirdPartyOnly && !firstPartyOnly) {
                    if (!LOGIN_CRITICAL_DOMAINS.contains(rawDomain)) {
                        blockedDomains.add(rawDomain)
                    }
                } else {
                    // Domain rule has modifiers
                    val filterRule = FilterRule(
                        raw = line,
                        isException = isException,
                        anchorDomain = rawDomain,
                        regex = null,
                        thirdPartyOnly = thirdPartyOnly,
                        firstPartyOnly = firstPartyOnly,
                        resourceTypes = resTypes
                    )
                    anchoredRules.computeIfAbsent(rawDomain) { CopyOnWriteArrayList() }.add(filterRule)
                }
                return
            }
        }

        // 5. Complex anchored / path / wildcard rules
        var anchorDomain: String? = null
        if (patternPart.startsWith("||")) {
            val afterAnchor = patternPart.removePrefix("||")
            val rawAnchor = afterAnchor.substringBefore("/").substringBefore("^").substringBefore("*").trim().lowercase()
            if (rawAnchor.contains(".") && !rawAnchor.contains("*")) {
                anchorDomain = rawAnchor
            }
        }

        val regex = try {
            convertAbpPatternToRegex(patternPart)
        } catch (_: Exception) {
            null
        }

        val rule = FilterRule(
            raw = line,
            isException = isException,
            anchorDomain = anchorDomain,
            regex = regex,
            thirdPartyOnly = thirdPartyOnly,
            firstPartyOnly = firstPartyOnly,
            resourceTypes = resTypes
        )

        if (anchorDomain != null) {
            anchoredRules.computeIfAbsent(anchorDomain) { CopyOnWriteArrayList() }.add(rule)
        } else {
            globalRules.add(rule)
        }
    }

    private fun convertAbpPatternToRegex(pattern: String): Regex {
        val sb = StringBuilder()
        var i = 0
        val len = pattern.length

        if (pattern.startsWith("||")) {
            sb.append("""^(?:https?|ftp|ws|wss):\/\/(?:[a-zA-Z0-9_.-]+\.)?""")
            i = 2
        } else if (pattern.startsWith("|")) {
            sb.append("^")
            i = 1
        }

        while (i < len) {
            val c = pattern[i]
            when (c) {
                '*' -> sb.append(".*")
                '^' -> sb.append("""(?:[^a-zA-Z0-9._%-]|$)""")
                '|' -> {
                    if (i == len - 1) {
                        sb.append("$")
                    } else {
                        sb.append("\\|")
                    }
                }
                '.', '\\', '+', '?', '{', '}', '(', ')', '[', ']', '$' -> {
                    sb.append('\\').append(c)
                }
                else -> sb.append(c)
            }
            i++
        }

        return Regex(sb.toString(), RegexOption.IGNORE_CASE)
    }

    private fun populateDefaultDomains() {
        val defaults = listOf(
            // Google Ads & Syndication
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
            "pubads.g.doubleclick.net",

            // Global Exchanges & Networks
            "amazon-adsystem.com",
            "aax.amazon-adsystem.com",
            "aax-us-east.amazon-adsystem.com",
            "c.amazon-adsystem.com",
            "adnxs.com",
            "adnxs-simple.com",
            "ib.adnxs.com",
            "criteo.com",
            "criteo.net",
            "static.criteo.net",
            "taboola.com",
            "cdn.taboola.com",
            "outbrain.com",
            "widgets.outbrain.com",
            "scorecardresearch.com",
            "moatads.com",
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
            "teads.com",
            "tribalfusion.com",
            "undertone.com",
            "adblade.com",
            "adbutler.com",
            "adform.net",
            "adfox.ru",
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
            "jsc.mgid.com",
            "revcontent.com",
            "zergnet.com",
            "indexexchange.com",
            "gumgum.com",
            "kargo.com",
            "liveintent.com",
            "connatix.com",
            "aniview.com",
            "aniview.net",
            "truvid.com",
            "brid.tv",
            "primis.tech",
            "vi-serve.com",
            "targetspot.com",
            "dianomi.com",
            "speakol.com",

            // India & South Asia Curated Ad Networks
            "colombiaonline.com",
            "clmbtech.com",
            "static.clmbtech.com",
            "clmbe.com",
            "adelement.com",
            "adpushup.com",
            "vserv.com",
            "vserv.mobi",
            "tyroo.com",
            "affinity.com",
            "admagnet.com",
            "komli.com",
            "adnetworkindia.com",
            "pokkt.com",
            "cubecdn.net",
            "jioads.com",
            "paytmads.com",
            "adsolut.in",
            "zapr.in",
            "silverpush.co",
            "verseads.in",
            "adgebra.co.in",
            "inuxu.co.in",

            // Mobile & Video Ad Networks
            "applovin.com",
            "unityads.unity3d.com",
            "chartboost.com",
            "inmobi.com",
            "inmobi.net",
            "vungle.com",
            "ironsrc.com",
            "adcolony.com",
            "pangle.io",
            "mintegral.com",
            "fyber.com",
            "tapjoy.com",
            "mobfox.com",
            "flurry.com",
            "appsflyer.com",
            "adjust.com",

            // Pop-up & High-Frequency Redirect Networks
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
            "adtrue.com",
            "bet365affiliates.com",
            "revenuehits.com",
            "ero-advertising.com",
            "juicyads.com",
            "clicksor.com",
            "ad-maven.com",
            "admaven.com",

            // Trackers & Analytics
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

    fun isHostBlocked(normalizedHost: String): Boolean {
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

    fun isHostException(normalizedHost: String): Boolean {
        var checkHost = normalizedHost
        while (checkHost.isNotEmpty()) {
            if (exceptionDomains.contains(checkHost)) {
                return true
            }
            val dotIndex = checkHost.indexOf('.')
            if (dotIndex == -1) break
            checkHost = checkHost.substring(dotIndex + 1)
        }
        return false
    }

    fun matchesDomain(host: String, targetDomain: String): Boolean {
        if (host.equals(targetDomain, ignoreCase = true)) return true
        if (host.endsWith(".$targetDomain", ignoreCase = true)) return true
        return false
    }

    fun getBaseDomain(host: String): String {
        val clean = host.lowercase().removePrefix("www.").trim().trimEnd('.')
        val parts = clean.split('.')
        if (parts.size <= 2) return clean

        val secondToLast = parts[parts.size - 2]
        val last = parts.last()
        val isTwoLevelTld = (last.length == 2 && (secondToLast == "co" || secondToLast == "com" ||
                secondToLast == "org" || secondToLast == "net" || secondToLast == "gov" ||
                secondToLast == "edu" || secondToLast == "ac" || secondToLast == "gen"))

        return if (isTwoLevelTld && parts.size >= 3) {
            parts.takeLast(3).joinToString(".")
        } else {
            parts.takeLast(2).joinToString(".")
        }
    }

    fun isThirdParty(requestHost: String, siteHost: String?): Boolean {
        if (siteHost.isNullOrBlank()) return true
        val reqBase = getBaseDomain(requestHost)
        val siteBase = getBaseDomain(siteHost)
        return reqBase != siteBase
    }

    fun detectResourceType(
        url: String?,
        headers: Map<String, String>? = null,
        isForMainFrame: Boolean = false
    ): ResourceType {
        if (isForMainFrame) return ResourceType.OTHER
        if (url == null) return ResourceType.OTHER

        // Check Sec-Fetch-Dest
        val fetchDest = headers?.let { h -> (h["Sec-Fetch-Dest"] ?: h["sec-fetch-dest"])?.lowercase() }
        if (fetchDest != null) {
            when (fetchDest) {
                "script" -> return ResourceType.SCRIPT
                "image" -> return ResourceType.IMAGE
                "style" -> return ResourceType.STYLESHEET
                "iframe", "embed" -> return ResourceType.SUBDOCUMENT
                "empty" -> return ResourceType.XMLHTTPREQUEST
            }
        }

        // Check Accept Header
        val accept = headers?.let { h -> (h["Accept"] ?: h["accept"])?.lowercase() }
        if (accept != null) {
            when {
                accept.contains("text/css") -> return ResourceType.STYLESHEET
                accept.contains("image/") -> return ResourceType.IMAGE
                accept.contains("text/javascript") || accept.contains("application/javascript") -> return ResourceType.SCRIPT
            }
        }

        // Fallback to URL extension
        val lower = url.lowercase().substringBefore('?').substringBefore('#')
        return when {
            lower.endsWith(".js") || lower.endsWith(".mjs") -> ResourceType.SCRIPT
            lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                    lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".svg") ||
                    lower.endsWith(".ico") -> ResourceType.IMAGE
            lower.endsWith(".css") -> ResourceType.STYLESHEET
            else -> ResourceType.OTHER
        }
    }

    fun normalizeDomain(domainOrUrl: String): String {
        val input = domainOrUrl.trim()
        val rawHost = if (input.contains("://")) {
            try {
                Uri.parse(input).host ?: input.substringAfter("://").substringBefore('/').substringBefore(':')
            } catch (_: Exception) {
                input.substringAfter("://").substringBefore('/').substringBefore(':')
            }
        } else {
            input.substringBefore('/').substringBefore(':')
        }
        return rawHost.trim().lowercase().removePrefix("www.")
    }

    fun isAdOrTracker(
        url: String?,
        siteHost: String? = null,
        resourceType: ResourceType? = null,
        requestHeaders: Map<String, String>? = null
    ): Boolean {
        if (url.isNullOrBlank()) return false

        // 1. Check user site allowlist
        if (!siteHost.isNullOrEmpty()) {
            val normalizedSite = normalizeDomain(siteHost)
            if (isAllowlisted(normalizedSite)) {
                return false
            }
        }

        // 2. Extract and normalize request host
        val requestHost = try {
            val uri = Uri.parse(url)
            uri.host?.lowercase()
        } catch (_: Exception) {
            null
        } ?: try {
            val afterScheme = if (url.contains("://")) url.substringAfter("://") else url
            val raw = afterScheme.substringBefore('/').substringBefore('?').substringBefore(':')
            raw.trim().lowercase().ifBlank { null }
        } catch (_: Exception) {
            null
        } ?: return false

        val normalizedHost = requestHost.removePrefix("www.")

        // 3. Prevent breaking logins for essential OAuth/Identity providers in standard mode
        if (!isStrictMode && isLoginCritical(normalizedHost)) {
            return false
        }

        // 4. Check domain exceptions (@@ rules)
        if (isHostException(normalizedHost)) {
            return false
        }

        val resType = resourceType ?: detectResourceType(url, requestHeaders)
        val is3p = isThirdParty(normalizedHost, siteHost)

        // 5. Check anchored exception rules
        var checkDomain = normalizedHost
        while (checkDomain.isNotEmpty()) {
            val rules = anchoredRules[checkDomain]
            if (rules != null) {
                for (rule in rules) {
                    if (rule.isException && rule.matches(url, normalizedHost, siteHost, resType)) {
                        return false
                    }
                }
            }
            val dot = checkDomain.indexOf('.')
            if (dot == -1) break
            checkDomain = checkDomain.substring(dot + 1)
        }

        // 6. Check global exception rules
        for (rule in globalRules) {
            if (rule.isException && rule.matches(url, normalizedHost, siteHost, resType)) {
                return false
            }
        }

        // 7. Fast O(1) Suffix & Exact Domain Lookup
        if (isHostBlocked(normalizedHost)) {
            return true
        }

        // 8. Check anchored blocking rules
        checkDomain = normalizedHost
        while (checkDomain.isNotEmpty()) {
            val rules = anchoredRules[checkDomain]
            if (rules != null) {
                for (rule in rules) {
                    if (!rule.isException && rule.matches(url, normalizedHost, siteHost, resType)) {
                        return true
                    }
                }
            }
            val dot = checkDomain.indexOf('.')
            if (dot == -1) break
            checkDomain = checkDomain.substring(dot + 1)
        }

        // 9. Check global blocking rules
        for (rule in globalRules) {
            if (!rule.isException && rule.matches(url, normalizedHost, siteHost, resType)) {
                return true
            }
        }

        // 10. Tightly tuned path & keyword heuristics (prevents false positives like vega.js or broadserver)
        val path = try {
            Uri.parse(url).path?.lowercase() ?: ""
        } catch (_: Exception) {
            val afterScheme = if (url.contains("://")) url.substringAfter("://") else url
            val rawPath = afterScheme.substringAfter('/', "")
            if (rawPath.isNotEmpty()) "/${rawPath.substringBefore('?')}" else ""
        }

        if (matchesTightAdPath(path, url.lowercase(), is3p)) {
            return true
        }

        return false
    }

    private fun matchesTightAdPath(path: String, lowerUrl: String, isThirdParty: Boolean): Boolean {
        if (path.isEmpty()) return false

        // Precise script filenames that must match on segment boundaries
        if (path.endsWith("/ga.js") || path.contains("/ga.js?") ||
            path.endsWith("/gtm.js") || path.contains("/gtm.js?") ||
            path.endsWith("/fbevents.js") || path.contains("/fbevents.js?") ||
            path.endsWith("/prebid.js") || path.contains("/prebid.js?") ||
            path.endsWith("/adsbygoogle.js") || path.contains("/adsbygoogle.js?") ||
            path.endsWith("/show_ads.js") || path.contains("/show_ads.js?")
        ) {
            return true
        }

        // Standard Google ad paths
        if (path.contains("/pagead/js/") || path.contains("/pagead/ads") || path.contains("/pagead/lvz")) {
            return true
        }
        if (path.contains("/gampad/ads") || path.contains("/tag/js/gpt.js")) {
            return true
        }

        // Third-party adserver / sponsor / popup patterns
        if (isThirdParty) {
            if (path.contains("/adserver.") || path.contains("/adserver/") ||
                path.contains("/ad-loader") || path.contains("/popunder") ||
                path.contains("/popupad") || path.contains("/advertisement.js")
            ) {
                return true
            }
        }

        return false
    }

    fun isLoginCritical(host: String): Boolean {
        for (critical in LOGIN_CRITICAL_DOMAINS) {
            if (host == critical || host.endsWith(".$critical")) {
                return true
            }
        }
        return false
    }

    fun addToAllowlist(domainOrUrl: String) {
        val clean = normalizeDomain(domainOrUrl)
        if (clean.isNotEmpty()) {
            allowlist.add(clean)
        }
    }

    fun removeFromAllowlist(domainOrUrl: String) {
        val clean = normalizeDomain(domainOrUrl)
        allowlist.remove(clean)
        // Also check if root domain was in allowlist
        val base = getBaseDomain(clean)
        allowlist.remove(base)
    }

    fun isAllowlisted(domainOrUrl: String): Boolean {
        var clean = normalizeDomain(domainOrUrl)
        while (clean.isNotEmpty()) {
            if (allowlist.contains(clean)) {
                return true
            }
            val dotIndex = clean.indexOf('.')
            if (dotIndex == -1) break
            clean = clean.substring(dotIndex + 1)
        }
        return false
    }

    fun getAllowlist(): Set<String> = allowlist.toSet()

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

    fun getCosmeticCss(): String {
        val allSelectors = (DEFAULT_COSMETIC_SELECTORS + cosmeticSelectors).distinct()
        val selectorsJoined = allSelectors.joinToString(", ")
        return "$selectorsJoined { display: none !important; visibility: hidden !important; height: 0px !important; min-height: 0px !important; max-height: 0px !important; padding: 0px !important; margin: 0px !important; opacity: 0 !important; pointer-events: none !important; }"
    }

    fun getCosmeticDocumentStartScript(): String {
        val css = getCosmeticCss().replace("'", "\\'").replace("\n", " ")
        return """
            (function() {
                if (window.__velocityAdBlockDisabled) return;

                // 1. Defuse anti-adblock detection and provide minimal safe globals
                try {
                    window.canRunAds = true;
                    window.isAdBlockActive = false;
                    window.adblock = false;
                    if (!window.adsbygoogle) {
                        try {
                            var adsbygoogle = [];
                            adsbygoogle.loaded = true;
                            adsbygoogle.push = function() { return 0; };
                            Object.defineProperty(window, 'adsbygoogle', {
                                get: function() { return adsbygoogle; },
                                set: function() {},
                                configurable: true
                            });
                        } catch(e) {
                            window.adsbygoogle = { loaded: true, push: function() { return 0; } };
                        }
                    }
                } catch(e) {}

                // 2. Inject early CSS stylesheet to prevent banner flashing
                var styleId = 'velocity-shield-ad-hide';
                function injectEarlyStyle() {
                    if (window.__velocityAdBlockDisabled) return;
                    if (!document.getElementById(styleId)) {
                        var style = document.createElement('style');
                        style.id = styleId;
                        style.textContent = '$css';
                        var root = document.head || document.documentElement;
                        if (root) {
                            root.appendChild(style);
                        }
                    }
                }

                injectEarlyStyle();
                if (document.readyState === 'loading') {
                    document.addEventListener('DOMContentLoaded', injectEarlyStyle);
                }

                // 3. Actively collapse any visible ad frames and placeholder elements
                function collapseAdElements() {
                    if (window.__velocityAdBlockDisabled) return;
                    var selectors = [
                        '.adsbygoogle', '[id^="google_ads_"]', '[id^="div-gpt-ad"]',
                        '[data-ad-slot]', '[data-ad-client]', '.ad-banner', '.ad-container',
                        '.ad-wrapper', '.ad-slot', '.advertisement', '.taboola-placeholder',
                        '.outbrain-placeholder', '[class*="sponsored-post"]',
                        'iframe[src*="doubleclick"]', 'iframe[src*="googlesyndication"]',
                        'iframe[id*="google_ads"]', '.ad-box', '.ad_box', '#ad-banner'
                    ];
                    try {
                        var nodes = document.querySelectorAll(selectors.join(','));
                        for (var i = 0; i < nodes.length; i++) {
                            nodes[i].style.setProperty('display', 'none', 'important');
                            nodes[i].style.setProperty('height', '0px', 'important');
                            nodes[i].style.setProperty('min-height', '0px', 'important');
                            nodes[i].style.setProperty('visibility', 'hidden', 'important');
                        }
                    } catch(e) {}
                }

                collapseAdElements();
                window.addEventListener('load', collapseAdElements);

                // 4. MutationObserver for dynamic infinite scrolls and single-page apps
                if (window.MutationObserver && !window._velocityObserverAttached) {
                    window._velocityObserverAttached = true;
                    var observer = new MutationObserver(collapseAdElements);
                    var target = document.body || document.documentElement;
                    if (target) {
                        observer.observe(target, { childList: true, subtree: true });
                    } else {
                        document.addEventListener('DOMContentLoaded', function() {
                            var t = document.body || document.documentElement;
                            if (t) observer.observe(t, { childList: true, subtree: true });
                        });
                    }
                }
            })();
        """.trimIndent()
    }

    fun injectCosmeticAdBlock(webView: WebView) {
        val script = """
            (function() {
                try {
                    window.__velocityAdBlockDisabled = false;
                    var styleId = 'velocity-shield-ad-hide';
                    if (!document.getElementById(styleId)) {
                        var style = document.createElement('style');
                        style.id = styleId;
                        style.textContent = '${getCosmeticCss().replace("'", "\\'").replace("\n", " ")}';
                        (document.head || document.documentElement).appendChild(style);
                    }
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
                } catch(e) {}
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    fun removeCosmeticAdBlock(webView: WebView) {
        val script = """
            (function() {
                try {
                    window.__velocityAdBlockDisabled = true;
                    var style = document.getElementById('velocity-shield-ad-hide');
                    if (style && style.parentNode) {
                        style.parentNode.removeChild(style);
                    }
                } catch(e) {}
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    fun setCosmeticAdBlockState(webView: WebView, enabled: Boolean) {
        if (enabled) {
            injectCosmeticAdBlock(webView)
        } else {
            removeCosmeticAdBlock(webView)
        }
    }
}
