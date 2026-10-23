package com.aaryo.vedabrowser.privacy

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import android.webkit.WebView
import com.aaryo.vedabrowser.model.BrowserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

enum class ResourceType {
    SCRIPT,
    IMAGE,
    STYLESHEET,
    SUBDOCUMENT,
    XMLHTTPREQUEST,
    MEDIA,
    FONT,
    WEBSOCKET,
    PING,
    OTHER
}

data class FilterRule(
    val raw: String,
    val isException: Boolean,
    val anchorDomain: String? = null,
    val regex: Regex? = null,
    val isImportant: Boolean = false,
    val thirdPartyOnly: Boolean = false,
    val firstPartyOnly: Boolean = false,
    val permittedDomains: Set<String>? = null,
    val excludedDomains: Set<String>? = null,
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
        if (siteHost != null) {
            val cleanSite = AdBlocker.normalizeDomain(siteHost)
            val baseSite = AdBlocker.getBaseDomain(cleanSite)
            if (excludedDomains != null && (excludedDomains.contains(cleanSite) || excludedDomains.contains(baseSite))) {
                return false
            }
            if (permittedDomains != null && !permittedDomains.contains(cleanSite) && !permittedDomains.contains(baseSite)) {
                return false
            }
        } else if (permittedDomains != null) {
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

    // Fast O(1) domain lookup sets
    private val blockedDomains = ConcurrentHashMap.newKeySet<String>()
    private val thirdPartyBlockedDomains = ConcurrentHashMap.newKeySet<String>()
    private val exceptionDomains = ConcurrentHashMap.newKeySet<String>()
    private val allowlist = ConcurrentHashMap.newKeySet<String>()

    // Indexed network rules
    private val anchoredRules = ConcurrentHashMap<String, CopyOnWriteArrayList<FilterRule>>()
    private val anchoredExceptions = ConcurrentHashMap<String, CopyOnWriteArrayList<FilterRule>>()
    private val globalRules = CopyOnWriteArrayList<FilterRule>()
    private val globalExceptions = CopyOnWriteArrayList<FilterRule>()
    private val importantRules = CopyOnWriteArrayList<FilterRule>()
    private val importantExceptions = CopyOnWriteArrayList<FilterRule>()

    // Cosmetic rules
    private val globalCosmeticSelectors = ConcurrentHashMap.newKeySet<String>()
    private val globalCosmeticExceptions = ConcurrentHashMap.newKeySet<String>()
    private val domainCosmeticSelectors = ConcurrentHashMap<String, MutableSet<String>>()
    private val domainExcludedCosmetics = ConcurrentHashMap<String, MutableSet<String>>()
    private val cosmeticExceptions = ConcurrentHashMap<String, MutableSet<String>>()

    // Default global selectors (curated and specific; over-broad wildcards like [class*="-ad-"] avoided)
    private val DEFAULT_GLOBAL_COSMETIC_SELECTORS = setOf(
        ".adsbygoogle",
        "ins.adsbygoogle",
        "[id^=\"google_ads_\"]",
        "[id^=\"div-gpt-ad\"]",
        "[id^=\"ad-slot-\"]",
        "[id^=\"ad_unit_\"]",
        "[class^=\"ad-container\"]",
        "[class^=\"ad-wrapper\"]",
        "[class^=\"ad-slot\"]",
        "[class^=\"ad-banner\"]",
        "[class^=\"ad_banner\"]",
        "[class^=\"sponsored-post\"]",
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
        ".taboola-placeholder",
        ".outbrain-placeholder",
        ".clmb-placeholder",
        ".clmbe-ad",
        ".colombia-ad",
        ".inmobi-ad",
        ".adpushup-container",
        ".mgid-container",
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
        "iframe[id*=\"google_ads\"]",
        "#tads",
        "#tadsb",
        "#bottomads",
        "div[data-text-ad=\"1\"]",
        ".commercial-unit-mobile-top",
        ".commercial-unit-mobile-bottom",
        ".commercial-unit-desktop-top",
        ".commercial-unit-desktop-bottom",
        "[aria-label=\"Ads\"]",
        "[aria-label=\"Ad\"]",
        "[aria-label=\"Sponsored\"]",
        ".uEierd",
        ".pla-unit",
        ".pla-unit-container",
        "div[jscontroller][data-ved*=\"ad\"]",
        ".trc_related_container",
        ".trc_rbox_div"
    )

    // 1x1 transparent PNG
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

    // 1x1 transparent GIF
    private val TRANSPARENT_1X1_GIF = byteArrayOf(
        0x47.toByte(), 0x49.toByte(), 0x46.toByte(), 0x38.toByte(), 0x39.toByte(), 0x61.toByte(),
        0x01.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte(), 0x80.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xFF.toByte(),
        0xFF.toByte(), 0x21.toByte(), 0xF9.toByte(), 0x04.toByte(), 0x01.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x2C.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x02.toByte(), 0x02.toByte(), 0x44.toByte(), 0x01.toByte(), 0x00.toByte(),
        0x3B.toByte()
    )

    @Volatile
    var isStrictMode: Boolean = false

    @Volatile
    var globalEnabled: Boolean = true
        private set

    fun setGlobalEnabled(enabled: Boolean) {
        globalEnabled = enabled
    }

    @Volatile
    private var isInitialized = false

    init {
        populateDefaultDomains()
        globalCosmeticSelectors.addAll(DEFAULT_GLOBAL_COSMETIC_SELECTORS)
    }

    fun syncAllowlist(allowlistedDomains: Set<String>) {
        allowlist.clear()
        for (d in allowlistedDomains) {
            val norm = normalizeDomain(d)
            if (norm.isNotEmpty()) {
                allowlist.add(norm)
            }
        }
    }

    fun initialize(context: Context, settings: BrowserSettings? = null) {
        if (isInitialized) return
        isInitialized = true
        isStrictMode = settings?.adBlockStrictMode ?: false
        globalEnabled = settings?.adBlockGlobalEnabled ?: true
        settings?.allowlistedDomains?.let { syncAllowlist(it) }

        CoroutineScope(Dispatchers.IO).launch {
            loadRules(context, settings, forceReload = false)
        }
    }

    suspend fun loadRules(
        context: Context,
        settings: BrowserSettings? = null,
        forceReload: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val listsDir = File(context.filesDir, "filter_lists")

        val easyListFile = File(listsDir, "easylist.txt")
        val easyPrivacyFile = File(listsDir, "easyprivacy.txt")
        val ublockFile = File(listsDir, "ublock_filters.txt")
        val indianFile = File(listsDir, "indian_filters.txt")

        val easyListEnabled = settings?.easyListEnabled ?: true
        val easyPrivacyEnabled = settings?.easyPrivacyEnabled ?: true
        val uBlockEnabled = settings?.uBlockFiltersEnabled ?: true
        val indianEnabled = settings?.indianFiltersEnabled ?: true

        // Reset rule collections
        blockedDomains.clear()
        thirdPartyBlockedDomains.clear()
        exceptionDomains.clear()
        anchoredRules.clear()
        anchoredExceptions.clear()
        globalRules.clear()
        globalExceptions.clear()
        importantRules.clear()
        importantExceptions.clear()
        domainCosmeticSelectors.clear()
        domainExcludedCosmetics.clear()
        cosmeticExceptions.clear()
        globalCosmeticExceptions.clear()
        globalCosmeticSelectors.clear()

        // Re-seed defaults
        populateDefaultDomains()
        globalCosmeticSelectors.addAll(DEFAULT_GLOBAL_COSMETIC_SELECTORS)

        fun loadList(localFile: File, assetName: String) {
            val stream: InputStream? = try {
                if (localFile.exists() && localFile.length() > 0L) {
                    localFile.inputStream()
                } else {
                    context.assets.open(assetName)
                }
            } catch (_: Exception) {
                null
            }

            stream?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).useLines { lines ->
                    for (line in lines) {
                        parseRuleLine(line)
                    }
                }
            }
        }

        if (easyListEnabled) {
            loadList(easyListFile, "easylist.txt")
        }
        if (easyPrivacyEnabled) {
            loadList(easyPrivacyFile, "easyprivacy.txt")
        }
        if (uBlockEnabled) {
            loadList(ublockFile, "ublock_filters.txt")
        }
        if (indianEnabled) {
            loadList(indianFile, "indian_filters.txt")
        }
    }

    fun parseRuleLine(rawLine: String) {
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("!") || line.startsWith("[")) {
            return
        }

        // 1. Cosmetic rules (##, #@#)
        if (line.contains("##") || line.contains("#@#")) {
            parseCosmeticRule(line)
            return
        }

        if (line.startsWith("#")) {
            return
        }

        // 2. Exception rules (@@)
        val isException = line.startsWith("@@")
        val cleanLine = if (isException) line.removePrefix("@@").trim() else line

        // 3. Modifiers ($)
        val patternPart = if (cleanLine.contains("$")) cleanLine.substringBefore("$").trim() else cleanLine
        val modifiersPart = if (cleanLine.contains("$")) cleanLine.substringAfter("$").trim() else ""

        var isImportant = false
        var thirdPartyOnly = false
        var firstPartyOnly = false
        var permittedDomains: MutableSet<String>? = null
        var excludedDomains: MutableSet<String>? = null
        val resourceTypes = mutableSetOf<ResourceType>()

        if (modifiersPart.isNotEmpty()) {
            val mods = modifiersPart.split(',')
            for (mod in mods) {
                val m = mod.trim().lowercase()
                when {
                    m == "important" -> isImportant = true
                    m == "third-party" || m == "3p" -> thirdPartyOnly = true
                    m == "~third-party" || m == "~3p" || m == "first-party" || m == "1p" -> firstPartyOnly = true
                    m == "script" -> resourceTypes.add(ResourceType.SCRIPT)
                    m == "image" -> resourceTypes.add(ResourceType.IMAGE)
                    m == "stylesheet" || m == "css" -> resourceTypes.add(ResourceType.STYLESHEET)
                    m == "subdocument" || m == "document" || m == "frame" -> resourceTypes.add(ResourceType.SUBDOCUMENT)
                    m == "xmlhttprequest" || m == "xhr" -> resourceTypes.add(ResourceType.XMLHTTPREQUEST)
                    m == "media" -> resourceTypes.add(ResourceType.MEDIA)
                    m == "font" -> resourceTypes.add(ResourceType.FONT)
                    m == "websocket" -> resourceTypes.add(ResourceType.WEBSOCKET)
                    m == "ping" -> resourceTypes.add(ResourceType.PING)
                    m == "other" -> resourceTypes.add(ResourceType.OTHER)
                    m.startsWith("domain=") -> {
                        val domList = m.substringAfter("domain=").split('|')
                        for (domEntry in domList) {
                            val cleanD = domEntry.trim().lowercase()
                            if (cleanD.startsWith("~")) {
                                if (excludedDomains == null) excludedDomains = mutableSetOf()
                                excludedDomains.add(cleanD.removePrefix("~"))
                            } else if (cleanD.isNotEmpty()) {
                                if (permittedDomains == null) permittedDomains = mutableSetOf()
                                permittedDomains.add(cleanD)
                            }
                        }
                    }
                }
            }
        }

        val resTypes = if (resourceTypes.isNotEmpty()) resourceTypes.toSet() else null

        // 4. Simple domain rule without path/wildcard -> store directly for O(1) fast lookup
        if (patternPart.startsWith("||") && !patternPart.contains("/") && !patternPart.contains("*")) {
            val rawDomain = patternPart.removePrefix("||").removeSuffix("^").trim().lowercase()
            if (rawDomain.isNotEmpty()) {
                if (isException && resTypes == null && !thirdPartyOnly && !firstPartyOnly && permittedDomains == null && excludedDomains == null && !isImportant) {
                    exceptionDomains.add(rawDomain)
                    return
                } else if (!isException && resTypes == null && permittedDomains == null && excludedDomains == null && !isImportant) {
                    if (thirdPartyOnly) {
                        thirdPartyBlockedDomains.add(rawDomain)
                        return
                    } else if (!firstPartyOnly) {
                        blockedDomains.add(rawDomain)
                        return
                    }
                } else {
                    val filterRule = FilterRule(
                        raw = line,
                        isException = isException,
                        anchorDomain = rawDomain,
                        regex = null,
                        isImportant = isImportant,
                        thirdPartyOnly = thirdPartyOnly,
                        firstPartyOnly = firstPartyOnly,
                        permittedDomains = permittedDomains,
                        excludedDomains = excludedDomains,
                        resourceTypes = resTypes
                    )
                    registerRule(filterRule, rawDomain)
                    return
                }
            }
        }

        // 5. Complex anchored or wildcard path rule
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
            isImportant = isImportant,
            thirdPartyOnly = thirdPartyOnly,
            firstPartyOnly = firstPartyOnly,
            permittedDomains = permittedDomains,
            excludedDomains = excludedDomains,
            resourceTypes = resTypes
        )

        registerRule(rule, anchorDomain)
    }

    private fun registerRule(rule: FilterRule, anchorDomain: String?) {
        if (rule.isImportant) {
            if (rule.isException) {
                importantExceptions.add(rule)
            } else {
                importantRules.add(rule)
            }
            return
        }

        if (rule.isException) {
            if (anchorDomain != null) {
                anchoredExceptions.computeIfAbsent(anchorDomain) { CopyOnWriteArrayList() }.add(rule)
            } else {
                globalExceptions.add(rule)
            }
        } else {
            if (anchorDomain != null) {
                anchoredRules.computeIfAbsent(anchorDomain) { CopyOnWriteArrayList() }.add(rule)
            } else {
                globalRules.add(rule)
            }
        }
    }

    private fun parseCosmeticRule(line: String) {
        val isUnhide = line.contains("#@#")
        val delimiter = if (isUnhide) "#@#" else "##"
        val domainPart = line.substringBefore(delimiter).trim()
        val selector = line.substringAfter(delimiter).trim()

        if (selector.isEmpty()) return

        // Skip unsupported procedural selectors that break browser CSS parsers
        if (selector.contains(":has-text(") ||
            selector.contains(":-abp-") ||
            selector.contains(":matches-path(") ||
            selector.contains(":xpath(") ||
            selector.contains("[-abp-") ||
            selector.contains(":matches-css(") ||
            selector.startsWith("^")
        ) {
            return
        }

        if (domainPart.isEmpty()) {
            if (isUnhide) {
                globalCosmeticExceptions.add(selector)
            } else {
                globalCosmeticSelectors.add(selector)
            }
        } else {
            val domains = domainPart.split(',')
            for (d in domains) {
                val cleanD = d.trim().lowercase()
                if (cleanD.isEmpty()) continue
                if (cleanD.startsWith("~")) {
                    val excluded = cleanD.removePrefix("~")
                    if (!isUnhide) {
                        domainExcludedCosmetics.computeIfAbsent(excluded) { ConcurrentHashMap.newKeySet() }.add(selector)
                    }
                } else {
                    if (isUnhide) {
                        cosmeticExceptions.computeIfAbsent(cleanD) { ConcurrentHashMap.newKeySet() }.add(selector)
                    } else {
                        domainCosmeticSelectors.computeIfAbsent(cleanD) { ConcurrentHashMap.newKeySet() }.add(selector)
                    }
                }
            }
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
                '^' -> sb.append("""(?:[^\w\d_.-]|$)""")
                '|' -> if (i == len - 1) sb.append("$") else sb.append("""\|""")
                '.', '?', '+', '(', ')', '[', ']', '{', '}', '$', '\\' -> sb.append('\\').append(c)
                else -> sb.append(c)
            }
            i++
        }

        return Regex(sb.toString(), RegexOption.IGNORE_CASE)
    }

    private fun populateDefaultDomains() {
        val defaults = listOf(
            "doubleclick.net", "googleadservices.com", "googlesyndication.com",
            "adservice.google.com", "pagead2.googlesyndication.com", "googletagservices.com",
            "googletagmanager.com", "google-analytics.com", "analytics.google.com",
            "admob.com", "ads.google.com", "amazon-adsystem.com", "adnxs.com",
            "criteo.com", "criteo.net", "taboola.com", "outbrain.com", "rubiconproject.com",
            "pubmatic.com", "openx.net", "casalemedia.com", "yieldmo.com", "smartadserver.com",
            "media.net", "sovrn.com", "lijit.com", "sharethrough.com", "sonobi.com",
            "spotxchange.com", "teads.tv", "teads.com", "tribalfusion.com", "undertone.com",
            "adblade.com", "adbutler.com", "adform.net", "adfox.ru", "adriver.ru",
            "adsafeprotected.com", "adserver.org", "adspirit.de", "adsrvr.org", "adsystem.com",
            "adtarget.io", "adtech.de", "adtechus.com", "advertising.com", "advertserve.com",
            "adroll.com", "bidswitch.net", "contextweb.com", "exponential.com", "mathtag.com",
            "mgid.com", "revcontent.com", "indexexchange.com", "gumgum.com", "kargo.com",
            "liveintent.com", "connatix.com", "aniview.com", "aniview.net", "adzerk.net",
            "yieldlove.com", "yieldoptimizer.com", "zedo.com", "zemanta.com", "turn.com",
            "truvid.com", "brid.tv", "primis.tech", "vi-serve.com", "targetspot.com",
            "dianomi.com", "speakol.com", "simpli.fi", "snigelweb.com", "stackadapt.com",
            "tapad.com", "trustx.org", "unrulymedia.com", "viglink.com", "applovin.com",
            "unityads.unity3d.com", "chartboost.com", "inmobi.com", "vungle.com", "ironsrc.com",
            "adcolony.com", "pangle.io", "mintegral.com", "fyber.com", "tapjoy.com", "mobfox.com",
            "popads.net", "popcash.net", "propellerads.com", "exoclick.com", "trafficjunky.com",
            "adsterra.com", "richads.com", "trafficstars.com", "monetag.com", "hilltopads.com",
            "clickadu.com", "adcash.com", "yllix.com", "bidvertiser.com", "adkernel.com",
            "adtrue.com", "bet365affiliates.com", "revenuehits.com", "ad-maven.com", "admaven.com",
            "connect.facebook.net", "hotjar.com", "clarity.ms", "scorecardresearch.com",
            "quantserve.com", "ads.yahoo.com", "analytics.yahoo.com", "gemini.yahoo.com",
            "colombiaonline.com", "clmbtech.com", "clmbe.com", "adelement.com", "adpushup.com",
            "vserv.com", "vserv.mobi", "jioads.com", "paytmads.com", "tyroo.com", "affinity.com",
            "admagnet.com", "komli.com", "adnetworkindia.com", "adsolut.in", "zapr.in", "silverpush.co"
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

    fun isHostThirdPartyBlocked(normalizedHost: String): Boolean {
        var checkHost = normalizedHost
        while (checkHost.isNotEmpty()) {
            if (thirdPartyBlockedDomains.contains(checkHost)) {
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
        if (isForMainFrame) return ResourceType.SUBDOCUMENT
        if (url == null) return ResourceType.OTHER

        // 1. Check Sec-Fetch-Dest header
        val fetchDest = headers?.let { h -> (h["Sec-Fetch-Dest"] ?: h["sec-fetch-dest"])?.lowercase() }
        if (fetchDest != null) {
            when (fetchDest) {
                "script" -> return ResourceType.SCRIPT
                "image" -> return ResourceType.IMAGE
                "style" -> return ResourceType.STYLESHEET
                "iframe", "embed", "frame", "document" -> return ResourceType.SUBDOCUMENT
                "empty" -> return ResourceType.XMLHTTPREQUEST
                "video", "audio" -> return ResourceType.MEDIA
                "font" -> return ResourceType.FONT
                "websocket" -> return ResourceType.WEBSOCKET
            }
        }

        // 2. Check Accept Header
        val accept = headers?.let { h -> (h["Accept"] ?: h["accept"])?.lowercase() }
        if (accept != null) {
            when {
                accept.contains("text/css") -> return ResourceType.STYLESHEET
                accept.contains("image/") -> return ResourceType.IMAGE
                accept.contains("text/javascript") || accept.contains("application/javascript") -> return ResourceType.SCRIPT
                accept.contains("text/html") -> return ResourceType.SUBDOCUMENT
                accept.contains("application/json") -> return ResourceType.XMLHTTPREQUEST
            }
        }

        // 3. Fallback to clean URL path extension
        val cleanPath = url.substringBefore('?').substringBefore('#').lowercase()
        return when {
            cleanPath.endsWith(".js") || cleanPath.endsWith(".mjs") -> ResourceType.SCRIPT
            cleanPath.endsWith(".png") || cleanPath.endsWith(".jpg") || cleanPath.endsWith(".jpeg") ||
                    cleanPath.endsWith(".gif") || cleanPath.endsWith(".webp") || cleanPath.endsWith(".svg") ||
                    cleanPath.endsWith(".ico") -> ResourceType.IMAGE
            cleanPath.endsWith(".css") -> ResourceType.STYLESHEET
            cleanPath.endsWith(".mp4") || cleanPath.endsWith(".webm") || cleanPath.endsWith(".mp3") -> ResourceType.MEDIA
            cleanPath.endsWith(".woff") || cleanPath.endsWith(".woff2") || cleanPath.endsWith(".ttf") || cleanPath.endsWith(".otf") -> ResourceType.FONT
            cleanPath.endsWith(".html") || cleanPath.endsWith(".htm") -> ResourceType.SUBDOCUMENT
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

    /**
     * Narrows login exemptions strictly to authentic OAuth/login endpoints.
     * Prevents tracking scripts (like connect.facebook.net/en_US/fbevents.js) from escaping ad blocking.
     */
    fun isLoginEndpoint(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("accounts.google.com/o/oauth2") ||
                lower.contains("accounts.google.com/signin") ||
                lower.contains("accounts.google.com/servicelogin") ||
                lower.contains("appleid.apple.com/auth") ||
                lower.contains("github.com/login") ||
                lower.contains("github.com/session") ||
                lower.contains("x.com/i/oauth") ||
                lower.contains("twitter.com/i/oauth") ||
                (lower.contains("facebook.com") && lower.contains("/dialog/oauth"))
    }

    fun isAdOrTracker(
        url: String?,
        siteHost: String? = null,
        resourceType: ResourceType? = null,
        requestHeaders: Map<String, String>? = null
    ): Boolean {
        if (!globalEnabled) return false
        if (url.isNullOrBlank()) return false

        // 1. Check user site allowlist
        if (!siteHost.isNullOrEmpty()) {
            val normalizedSite = normalizeDomain(siteHost)
            if (isAllowlisted(normalizedSite)) {
                return false
            }
        }

        // 2. Extract request host
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

        // 3. Narrowed login check (authentic OAuth endpoints exempted in non-strict mode)
        if (!isStrictMode && isLoginEndpoint(url)) {
            return false
        }

        val resType = resourceType ?: detectResourceType(url, requestHeaders)
        val is3p = isThirdParty(normalizedHost, siteHost)

        // 4. IMPORTANT rules check
        for (rule in importantExceptions) {
            if (rule.matches(url, normalizedHost, siteHost, resType)) {
                return false
            }
        }

        for (rule in importantRules) {
            if (rule.matches(url, normalizedHost, siteHost, resType)) {
                return true
            }
        }

        // 5. Host-level exceptions (@@ rules)
        if (isHostException(normalizedHost)) {
            return false
        }

        // 6. Check anchored standard exception rules
        var checkDomain = normalizedHost
        while (checkDomain.isNotEmpty()) {
            val rules = anchoredExceptions[checkDomain]
            if (rules != null) {
                for (rule in rules) {
                    if (rule.matches(url, normalizedHost, siteHost, resType)) {
                        return false
                    }
                }
            }
            val dot = checkDomain.indexOf('.')
            if (dot == -1) break
            checkDomain = checkDomain.substring(dot + 1)
        }

        // 7. Check global standard exception rules
        for (rule in globalExceptions) {
            if (rule.matches(url, normalizedHost, siteHost, resType)) {
                return false
            }
        }

        // 8. Fast O(1) Suffix & Exact Domain Lookup
        if (isHostBlocked(normalizedHost)) {
            return true
        }

        // 8b. Fast O(1) Third-Party Domain Lookup
        if (is3p && isHostThirdPartyBlocked(normalizedHost)) {
            return true
        }

        // 9. Check anchored standard blocking rules
        checkDomain = normalizedHost
        while (checkDomain.isNotEmpty()) {
            val rules = anchoredRules[checkDomain]
            if (rules != null) {
                for (rule in rules) {
                    if (rule.matches(url, normalizedHost, siteHost, resType)) {
                        return true
                    }
                }
            }
            val dot = checkDomain.indexOf('.')
            if (dot == -1) break
            checkDomain = checkDomain.substring(dot + 1)
        }

        // 10. Check global standard blocking rules
        for (rule in globalRules) {
            if (rule.matches(url, normalizedHost, siteHost, resType)) {
                return true
            }
        }

        // 11. Third-party ad subdomain heuristics
        if (is3p && isAdSubdomain(normalizedHost)) {
            return true
        }

        // 12. Tightly tuned ad path rules
        val path = try {
            Uri.parse(url).path?.lowercase() ?: ""
        } catch (_: Exception) {
            val afterScheme = if (url.contains("://")) url.substringAfter("://") else url
            val rawPath = afterScheme.substringAfter('/', "")
            if (rawPath.isNotEmpty()) "/${rawPath.substringBefore('?')}" else ""
        }

        if (matchesTightAdPath(path, is3p)) {
            return true
        }

        return false
    }

    fun isAdSubdomain(normalizedHost: String): Boolean {
        val parts = normalizedHost.split('.')
        if (parts.size <= 2) return false
        val prefix = parts.first()
        return prefix == "ad" || prefix == "ads" || prefix == "adserver" ||
                prefix == "adservice" || prefix == "pagead" || prefix == "banner" ||
                prefix == "banners" || prefix == "track" || prefix == "tracker" ||
                prefix == "tracking" || prefix == "telemetry" || prefix == "pixel"
    }

    private fun matchesTightAdPath(path: String, isThirdParty: Boolean): Boolean {
        if (path.isEmpty()) return false

        if (path.endsWith("/ga.js") || path.contains("/ga.js?") ||
            path.endsWith("/gtm.js") || path.contains("/gtm.js?") ||
            path.endsWith("/fbevents.js") || path.contains("/fbevents.js?") ||
            path.endsWith("/prebid.js") || path.contains("/prebid.js?") ||
            path.endsWith("/adsbygoogle.js") || path.contains("/adsbygoogle.js?") ||
            path.endsWith("/show_ads.js") || path.contains("/show_ads.js?") ||
            path.startsWith("/tr/") || path == "/tr" || path.contains("/tr?") || path.contains("/tr/?")
        ) {
            return true
        }

        if (path.contains("/pagead/js/") || path.contains("/pagead/ads") || path.contains("/pagead/lvz")) {
            return true
        }
        if (path.contains("/gampad/ads") || path.contains("/tag/js/gpt.js")) {
            return true
        }

        if (isThirdParty) {
            if (path.contains("/adserver.") || path.contains("/adserver/") ||
                path.contains("/ad-loader") || path.contains("/popunder") ||
                path.contains("/popupad") || path.contains("/advertisement.js") ||
                path.contains("/adsbygoogle") || path.contains("/pagead/") ||
                path.contains("amazon-adsystem") || path.contains("doubleclick") ||
                path.contains("/ad-tag") || path.contains("/ad-frame")
            ) {
                return true
            }
        }

        return false
    }

    fun getMimeTypeForResource(url: String? = null, resourceType: ResourceType? = null): String {
        val resType = resourceType ?: detectResourceType(url)
        return when (resType) {
            ResourceType.SCRIPT -> "application/javascript"
            ResourceType.IMAGE -> if (url?.lowercase()?.contains(".gif") == true) "image/gif" else "image/png"
            ResourceType.STYLESHEET -> "text/css"
            else -> "text/plain"
        }
    }

    fun isPopupAllowed(url: String?, isUserGesture: Boolean, sourceHost: String?): Boolean {
        if (!globalEnabled) return true
        if (url.isNullOrBlank()) return isUserGesture

        if (isAdOrTracker(url, sourceHost)) {
            return false
        }

        // Block popups triggered without direct user gesture
        if (!isUserGesture) {
            return false
        }

        return true
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

    /**
     * Generates proper HTTP responses:
     * - SCRIPT -> empty JS
     * - IMAGE -> 1x1 transparent PNG / GIF
     * - STYLESHEET -> empty CSS
     * - Subdocuments, XHR, Fonts, Media, and others -> HTTP 204 No Content with empty body
     */
    fun createEmptyResponse(url: String? = null, resourceType: ResourceType? = null): WebResourceResponse {
        val resType = resourceType ?: detectResourceType(url)

        val response = when (resType) {
            ResourceType.SCRIPT -> {
                WebResourceResponse(
                    "application/javascript",
                    "UTF-8",
                    ByteArrayInputStream("/* blocked by velocity */".toByteArray(Charsets.UTF_8))
                )
            }
            ResourceType.IMAGE -> {
                val isGif = url?.lowercase()?.contains(".gif") == true
                val bytes = if (isGif) TRANSPARENT_1X1_GIF else TRANSPARENT_1X1_PNG
                val mime = if (isGif) "image/gif" else "image/png"
                WebResourceResponse(mime, "UTF-8", ByteArrayInputStream(bytes))
            }
            ResourceType.STYLESHEET -> {
                WebResourceResponse(
                    "text/css",
                    "UTF-8",
                    ByteArrayInputStream("/* blocked by velocity */".toByteArray(Charsets.UTF_8))
                )
            }
            else -> {
                try {
                    WebResourceResponse(
                        "text/plain",
                        "UTF-8",
                        204,
                        "No Content",
                        mapOf(
                            "Access-Control-Allow-Origin" to "*",
                            "Cache-Control" to "no-cache, no-store, must-revalidate"
                        ),
                        ByteArrayInputStream(ByteArray(0))
                    )
                } catch (_: Throwable) {
                    WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                }
            }
        }

        try {
            response.responseHeaders = mapOf(
                "Access-Control-Allow-Origin" to "*",
                "Cache-Control" to "no-cache, no-store, must-revalidate"
            )
        } catch (_: Throwable) {
        }

        return response
    }

    /**
     * Resolves all applicable cosmetic selectors for siteHost.
     * Evaluates global selectors, domain specific rules, domain exclusions (~), and #@# unhide exceptions.
     */
    fun getCosmeticSelectors(siteHost: String? = null): Set<String> {
        val activeSelectors = mutableSetOf<String>()
        val exceptions = mutableSetOf<String>()
        exceptions.addAll(globalCosmeticExceptions)

        val cleanHost = siteHost?.let { normalizeDomain(it) }
        val baseHost = cleanHost?.let { getBaseDomain(it) }

        if (cleanHost != null) {
            cosmeticExceptions[cleanHost]?.let { exceptions.addAll(it) }
            domainExcludedCosmetics[cleanHost]?.let { exceptions.addAll(it) }
        }
        if (baseHost != null && baseHost != cleanHost) {
            cosmeticExceptions[baseHost]?.let { exceptions.addAll(it) }
            domainExcludedCosmetics[baseHost]?.let { exceptions.addAll(it) }
        }

        for (selector in globalCosmeticSelectors) {
            if (!exceptions.contains(selector)) {
                activeSelectors.add(selector)
            }
        }

        if (cleanHost != null) {
            domainCosmeticSelectors[cleanHost]?.let { set ->
                for (sel in set) {
                    if (!exceptions.contains(sel)) activeSelectors.add(sel)
                }
            }
        }
        if (baseHost != null && baseHost != cleanHost) {
            domainCosmeticSelectors[baseHost]?.let { set ->
                for (sel in set) {
                    if (!exceptions.contains(sel)) activeSelectors.add(sel)
                }
            }
        }

        return activeSelectors
    }

    /**
     * Builds dynamic cosmetic stylesheet in small chunks (10 selectors per rule) so that
     * if an invalid selector appears, it does not invalidate the entire stylesheet.
     */
    fun getCosmeticCss(siteHost: String? = null): String {
        val selectors = getCosmeticSelectors(siteHost)
        if (selectors.isEmpty()) return ""

        val sb = StringBuilder()
        val chunk = mutableListOf<String>()
        for (sel in selectors) {
            chunk.add(sel)
            if (chunk.size >= 10) {
                sb.append(chunk.joinToString(", "))
                    .append(" { display: none !important; visibility: hidden !important; height: 0px !important; min-height: 0px !important; max-height: 0px !important; padding: 0px !important; margin: 0px !important; opacity: 0 !important; pointer-events: none !important; }\n")
                chunk.clear()
            }
        }
        if (chunk.isNotEmpty()) {
            sb.append(chunk.joinToString(", "))
                .append(" { display: none !important; visibility: hidden !important; height: 0px !important; min-height: 0px !important; max-height: 0px !important; padding: 0px !important; margin: 0px !important; opacity: 0 !important; pointer-events: none !important; }\n")
        }
        return sb.toString()
    }

    /**
     * Document start script with:
     * 1. YouTube player JSON.parse and player fetch/XHR defuser (strips adPlacements, playerAds, adSlots)
     * 2. Anti-adblock defusers
     * 3. Chunked early CSS injection
     * 4. Video ad skip and fast-forward
     * 5. MutationObserver
     */
    fun escapeJsonString(input: String): String {
        val sb = StringBuilder("\"")
        for (c in input) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                '<' -> sb.append("\\u003c")
                '>' -> sb.append("\\u003e")
                '&' -> sb.append("\\u0026")
                else -> {
                    if (c.code in 0..0x1F) {
                        sb.append(String.format("\\u%04x", c.code))
                    } else {
                        sb.append(c)
                    }
                }
            }
        }
        sb.append("\"")
        return sb.toString()
    }

    fun getCosmeticDocumentStartScript(siteHost: String? = null): String {
        val rawCss = getCosmeticCss(siteHost)
        val quotedCss = escapeJsonString(rawCss)
        val isYouTube = siteHost?.contains("youtube.com") == true

        return """
            (function() {
                if (window.__velocityAdBlockDisabled) return;

                // 1. YouTube Player Ad Defuser
                ${if (isYouTube) """
                try {
                    var originalJsonParse = JSON.parse;
                    JSON.parse = function(text, reviver) {
                        var parsed = originalJsonParse.call(this, text, reviver);
                        if (parsed && typeof parsed === 'object') {
                            if (parsed.adPlacements) delete parsed.adPlacements;
                            if (parsed.playerAds) delete parsed.playerAds;
                            if (parsed.adSlots) delete parsed.adSlots;
                            if (parsed.adBreakHeartbeatParams) delete parsed.adBreakHeartbeatParams;
                            if (parsed.playerResponse) {
                                if (parsed.playerResponse.adPlacements) delete parsed.playerResponse.adPlacements;
                                if (parsed.playerResponse.playerAds) delete parsed.playerResponse.playerAds;
                                if (parsed.playerResponse.adSlots) delete parsed.playerResponse.adSlots;
                            }
                        }
                        return parsed;
                    };

                    if (window.fetch) {
                        var origFetch = window.fetch;
                        window.fetch = function(input, init) {
                            var url = typeof input === 'string' ? input : (input && input.url ? input.url : '');
                            if (url && (url.indexOf('/api/stats/ads') !== -1 || url.indexOf('/youtubei/v1/player/ad_break') !== -1 || url.indexOf('/pagead/') !== -1 || url.indexOf('ptracking') !== -1)) {
                                return Promise.resolve(new Response('{}', { status: 200, headers: { 'Content-Type': 'application/json' } }));
                            }
                            return origFetch.apply(this, arguments).then(function(resp) {
                                if (url && (url.indexOf('/youtubei/v1/player') !== -1)) {
                                    return resp.clone().json().then(function(data) {
                                        if (data) {
                                            if (data.adPlacements) delete data.adPlacements;
                                            if (data.playerAds) delete data.playerAds;
                                            if (data.adSlots) delete data.adSlots;
                                            if (data.adBreakHeartbeatParams) delete data.adBreakHeartbeatParams;
                                        }
                                        return new Response(JSON.stringify(data), {
                                            status: resp.status,
                                            statusText: resp.statusText,
                                            headers: resp.headers
                                        });
                                    }).catch(function() { return resp; });
                                }
                                return resp;
                            });
                        };
                    }

                    if (window.XMLHttpRequest) {
                        var origOpen = XMLHttpRequest.prototype.open;
                        XMLHttpRequest.prototype.open = function(method, url) {
                            this._velocityUrl = url;
                            return origOpen.apply(this, arguments);
                        };
                        var origSend = XMLHttpRequest.prototype.send;
                        XMLHttpRequest.prototype.send = function() {
                            var self = this;
                            if (this._velocityUrl && (this._velocityUrl.indexOf('/api/stats/ads') !== -1 || this._velocityUrl.indexOf('/youtubei/v1/player/ad_break') !== -1 || this._velocityUrl.indexOf('/pagead/') !== -1)) {
                                setTimeout(function() {
                                    if (self.onload) self.onload();
                                }, 0);
                                return;
                            }
                            return origSend.apply(this, arguments);
                        };
                    }
                } catch(e) {}
                """ else ""}

                // 2. Anti-adblock Defusers
                try {
                    window.canRunAds = true;
                    window.isAdBlockActive = false;
                    window.adblock = false;
                } catch(e) {}

                // 3. Early CSS Injection (Chunked and resilient)
                var styleId = 'velocity-shield-ad-hide';
                function injectEarlyStyle() {
                    if (window.__velocityAdBlockDisabled) return;
                    if (!document.getElementById(styleId)) {
                        var root = document.head || document.documentElement;
                        if (root) {
                            var style = document.createElement('style');
                            style.id = styleId;
                            style.textContent = $quotedCss;
                            root.appendChild(style);
                        }
                    }
                }
                injectEarlyStyle();

                // 4. Video Ad Skipper & Fast-Forward Fallback
                function handleVideoAds() {
                    try {
                        var skipBtn = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .videoAdUiSkipButton, [id^="skip-button"], .ytp-ad-preview-container');
                        if (skipBtn) skipBtn.click();

                        var video = document.querySelector('video');
                        var isAdPlaying = document.querySelector('.ad-showing, .ad-interrupting, .ytp-ad-player-overlay') != null;
                        if (video && isAdPlaying) {
                            video.muted = true;
                            video.playbackRate = 16.0;
                            if (isFinite(video.duration) && video.duration > 0) {
                                video.currentTime = video.duration;
                            }
                        }
                    } catch(e) {}
                }

                function collapseAds() {
                    if (window.__velocityAdBlockDisabled) return;
                    injectEarlyStyle();
                    handleVideoAds();
                }

                collapseAds();

                // 5. Dynamic MutationObserver
                if (window.MutationObserver && !window._velocityObserverAttached) {
                    window._velocityObserverAttached = true;
                    var observer = new MutationObserver(function() {
                        collapseAds();
                    });
                    var root = document.documentElement || document.body || document;
                    if (root) {
                        try {
                            observer.observe(root, { childList: true, subtree: true });
                        } catch(e) {}
                    }
                }

                if (document.readyState === 'loading') {
                    document.addEventListener('DOMContentLoaded', collapseAds);
                }
                window.addEventListener('load', collapseAds);
            })();
        """.trimIndent()
    }

    fun injectCosmeticAdBlock(webView: WebView, siteHost: String? = null) {
        val script = getCosmeticDocumentStartScript(siteHost)
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

    fun setCosmeticAdBlockState(webView: WebView, enabled: Boolean, siteHost: String? = null) {
        if (enabled) {
            injectCosmeticAdBlock(webView, siteHost)
        } else {
            removeCosmeticAdBlock(webView)
        }
    }
}
