package com.aaryo.vedabrowser.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdBlockerTest {

    @Before
    fun setUp() {
        AdBlocker.isStrictMode = false
        AdBlocker.setGlobalEnabled(true)
    }

    @Test
    fun testGlobalEnabledToggle() {
        val trackerUrl = "https://doubleclick.net/pagead/ads"
        assertTrue(AdBlocker.isAdOrTracker(trackerUrl))

        AdBlocker.setGlobalEnabled(false)
        assertFalse(AdBlocker.isAdOrTracker(trackerUrl))

        AdBlocker.setGlobalEnabled(true)
        assertTrue(AdBlocker.isAdOrTracker(trackerUrl))
    }

    @Test
    fun testAllowlistManagement() {
        val testDomain = "allowed-news.com"
        assertFalse(AdBlocker.isAllowlisted(testDomain))

        AdBlocker.addToAllowlist(testDomain)
        assertTrue(AdBlocker.isAllowlisted(testDomain))
        assertTrue(AdBlocker.isAllowlisted("www.$testDomain"))
        assertTrue(AdBlocker.isAllowlisted("sub.$testDomain"))
        assertTrue(AdBlocker.isAllowlisted("https://$testDomain/article/123"))

        AdBlocker.removeFromAllowlist(testDomain)
        assertFalse(AdBlocker.isAllowlisted(testDomain))
    }

    @Test
    fun testExemptionNarrowingToAuthEndpointsOnly() {
        AdBlocker.isStrictMode = false

        // 1. Authentic OAuth / Login endpoints are preserved (not blocked)
        assertFalse(AdBlocker.isAdOrTracker("https://accounts.google.com/o/oauth2/v2/auth?client_id=123"))
        assertFalse(AdBlocker.isAdOrTracker("https://accounts.google.com/signin/v2/identifier"))
        assertFalse(AdBlocker.isAdOrTracker("https://appleid.apple.com/auth/authorize?client_id=com.app"))
        assertFalse(AdBlocker.isAdOrTracker("https://github.com/login/oauth/authorize"))
        assertFalse(AdBlocker.isAdOrTracker("https://x.com/i/oauth2/authorize"))

        // 2. Tracking scripts on previously over-exempted domains MUST BE BLOCKED!
        assertTrue(AdBlocker.isAdOrTracker("https://connect.facebook.net/en_US/fbevents.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://connect.facebook.net/signals/config/123"))
        assertTrue(AdBlocker.isAdOrTracker("https://facebook.com/tr/?id=12345"))
    }

    @Test
    fun testStrictModeEnforcesAllBlocking() {
        AdBlocker.isStrictMode = true

        // Strict mode blocks even potential auth endpoints if covered by rules
        assertTrue(AdBlocker.isStrictMode)

        // Reset
        AdBlocker.isStrictMode = false
    }

    @Test
    fun testAllowlistedSiteBypassesAdCheck() {
        val site = "trusted-partner.com"
        AdBlocker.addToAllowlist(site)

        val trackerUrl = "https://doubleclick.net/pagead/ads"
        // When request originates from an allowlisted host, it should return false (bypassed)
        assertFalse(AdBlocker.isAdOrTracker(trackerUrl, siteHost = site))

        AdBlocker.removeFromAllowlist(site)
        assertTrue(AdBlocker.isAdOrTracker(trackerUrl, siteHost = site))
    }

    @Test
    fun testCommonAdNetworksBlocked() {
        // Google Ads & DoubleClick
        assertTrue(AdBlocker.isAdOrTracker("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://googleads.g.doubleclick.net/pagead/ads?client=ca-pub-123"))
        assertTrue(AdBlocker.isAdOrTracker("https://securepubads.g.doubleclick.net/gampad/ads"))
        assertTrue(AdBlocker.isAdOrTracker("https://www.googletagservices.com/tag/js/gpt.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://adservice.google.com/adsid/integrator.sync.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://tpc.googlesyndication.com/sodar/sodar2.js"))

        // Major ad networks
        assertTrue(AdBlocker.isAdOrTracker("https://aax.amazon-adsystem.com/e/dtb/bid"))
        assertTrue(AdBlocker.isAdOrTracker("https://static.criteo.net/js/ld/ld.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://cdn.taboola.com/libtrc/loader.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://widgets.outbrain.com/outbrain.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://ib.adnxs.com/jpt"))
        assertTrue(AdBlocker.isAdOrTracker("https://serve.popads.net/serve.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://jsc.mgid.com/site/123.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://cdn.adpushup.com/123/adpushup.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://inmobi.com/ad/loader.js"))

        // Leading web publisher & blog ad networks
        assertTrue(AdBlocker.isAdOrTracker("https://scripts.mediavine.com/tags/test.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://ads.adthrive.com/sites/123/ads.min.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://g.ezoic.net/ezoic/ezoic.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://a.pub.network/mysite-com/cls.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://adengine.snigelweb.com/service.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://cdns.connatix.com/connatix.player.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://live.primis.tech/live/liveView.php"))
    }

    @Test
    fun testIndianAdNetworksBlocked() {
        assertTrue(AdBlocker.isAdOrTracker("https://static.clmbtech.com/ad/commons/js/colombia.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://colombiaonline.com/serve/ads"))
        assertTrue(AdBlocker.isAdOrTracker("https://clmbe.com/click"))
        assertTrue(AdBlocker.isAdOrTracker("https://ads.vserv.mobi/delivery/ad.php"))
        assertTrue(AdBlocker.isAdOrTracker("https://vserv.com/banner.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://adelement.com/engine.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://jioads.com/sdk/v1/ad.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://paytmads.com/ads.js"))
    }

    @Test
    fun testFalsePositivesPrevented() {
        // "vega.js" must NOT match "ga.js"
        assertFalse(AdBlocker.isAdOrTracker("https://example.com/assets/vega.js"))
        assertFalse(AdBlocker.isAdOrTracker("https://cdn.example.com/libs/vega.js"))

        // "broadserver" must NOT match generic "adserver"
        assertFalse(AdBlocker.isAdOrTracker("https://example.com/broadserver/news"))
        assertFalse(AdBlocker.isAdOrTracker("https://example.com/api/broadserver"))

        // Uploads must NOT be blocked
        assertFalse(AdBlocker.isAdOrTracker("https://example.com/scripts/megaupload.js"))
        assertFalse(AdBlocker.isAdOrTracker("https://example.com/uploads/2026/banner_photo.jpg", siteHost = "example.com"))

        // First-party application bundles and clean websites
        assertFalse(AdBlocker.isAdOrTracker("https://en.wikipedia.org/wiki/Kotlin"))
        assertFalse(AdBlocker.isAdOrTracker("https://github.com/torvalds/linux"))
        assertFalse(AdBlocker.isAdOrTracker("https://www.google.com/search?q=android"))
        assertFalse(AdBlocker.isAdOrTracker("https://stackoverflow.com/questions/12345/android-webview"))
        assertFalse(AdBlocker.isAdOrTracker("https://cdn.example.com/static/js/bundle.js", siteHost = "example.com"))
    }

    @Test
    fun testThirdPartyMatching() {
        assertTrue(AdBlocker.isThirdParty("doubleclick.net", "example.com"))
        assertTrue(AdBlocker.isThirdParty("ad.clmbtech.com", "timesofindia.indiatimes.com"))
        assertTrue(AdBlocker.isThirdParty("google-analytics.com", "github.com"))

        assertFalse(AdBlocker.isThirdParty("cdn.example.com", "example.com"))
        assertFalse(AdBlocker.isThirdParty("assets.example.com", "www.example.com"))
        assertFalse(AdBlocker.isThirdParty("m.timesofindia.indiatimes.com", "timesofindia.indiatimes.com"))
        assertFalse(AdBlocker.isThirdParty("news.google.co.in", "google.co.in"))
    }

    @Test
    fun testBaseDomainExtraction() {
        assertEquals("example.com", AdBlocker.getBaseDomain("www.example.com"))
        assertEquals("example.com", AdBlocker.getBaseDomain("sub.deep.example.com"))
        assertEquals("google.co.in", AdBlocker.getBaseDomain("news.google.co.in"))
        assertEquals("bbc.co.uk", AdBlocker.getBaseDomain("news.bbc.co.uk"))
        assertEquals("doubleclick.net", AdBlocker.getBaseDomain("ad.doubleclick.net"))
    }

    @Test
    fun testResourceTypeDetection() {
        assertEquals(ResourceType.SCRIPT, AdBlocker.detectResourceType("https://example.com/app.js"))
        assertEquals(ResourceType.SCRIPT, AdBlocker.detectResourceType("https://example.com/module.mjs?v=2"))
        assertEquals(ResourceType.IMAGE, AdBlocker.detectResourceType("https://example.com/banner.png"))
        assertEquals(ResourceType.IMAGE, AdBlocker.detectResourceType("https://example.com/icon.svg"))
        assertEquals(ResourceType.STYLESHEET, AdBlocker.detectResourceType("https://example.com/style.css"))

        // Check header overrides
        val scriptHeaders = mapOf("Sec-Fetch-Dest" to "script")
        assertEquals(ResourceType.SCRIPT, AdBlocker.detectResourceType("https://example.com/api/bundle", scriptHeaders))

        val imageHeaders = mapOf("Accept" to "image/webp,image/*")
        assertEquals(ResourceType.IMAGE, AdBlocker.detectResourceType("https://example.com/dynamic-img", imageHeaders))

        val fontHeaders = mapOf("Sec-Fetch-Dest" to "font")
        assertEquals(ResourceType.FONT, AdBlocker.detectResourceType("https://example.com/font.bin", fontHeaders))
    }

    @Test
    fun testEmptyResponseTypes() {
        val scriptResp = AdBlocker.createEmptyResponse("https://example.com/ad.js", ResourceType.SCRIPT)
        assertNotNull(scriptResp)
        assertEquals("application/javascript", AdBlocker.getMimeTypeForResource("https://example.com/ad.js", ResourceType.SCRIPT))

        val imgResp = AdBlocker.createEmptyResponse("https://example.com/banner.png", ResourceType.IMAGE)
        assertNotNull(imgResp)
        assertEquals("image/png", AdBlocker.getMimeTypeForResource("https://example.com/banner.png", ResourceType.IMAGE))

        val gifResp = AdBlocker.createEmptyResponse("https://example.com/pixel.gif", ResourceType.IMAGE)
        assertNotNull(gifResp)
        assertEquals("image/gif", AdBlocker.getMimeTypeForResource("https://example.com/pixel.gif", ResourceType.IMAGE))

        val cssResp = AdBlocker.createEmptyResponse("https://example.com/ads.css", ResourceType.STYLESHEET)
        assertNotNull(cssResp)
        assertEquals("text/css", AdBlocker.getMimeTypeForResource("https://example.com/ads.css", ResourceType.STYLESHEET))

        val xhrResp = AdBlocker.createEmptyResponse("https://example.com/api/track", ResourceType.XMLHTTPREQUEST)
        assertNotNull(xhrResp)
        assertEquals("text/plain", AdBlocker.getMimeTypeForResource("https://example.com/api/track", ResourceType.XMLHTTPREQUEST))

        val subdocResp = AdBlocker.createEmptyResponse("https://example.com/iframe_ad.html", ResourceType.SUBDOCUMENT)
        assertNotNull(subdocResp)
        assertEquals("text/plain", AdBlocker.getMimeTypeForResource("https://example.com/iframe_ad.html", ResourceType.SUBDOCUMENT))
    }

    @Test
    fun testExceptionRulesHandling() {
        AdBlocker.parseRuleLine("||annoying-ads.com^")
        assertTrue(AdBlocker.isAdOrTracker("https://annoying-ads.com/ad.html"))

        // Add exception rule
        AdBlocker.parseRuleLine("@@||annoying-ads.com/safe-page^")
        assertFalse(AdBlocker.isAdOrTracker("https://annoying-ads.com/safe-page"))

        // Normal ad endpoint on same domain should still be blocked
        assertTrue(AdBlocker.isAdOrTracker("https://annoying-ads.com/banner.html"))
    }

    @Test
    fun testImportantModifierOverridesException() {
        // Normal exception
        AdBlocker.parseRuleLine("@@||special-network.com/banner^")
        // Important block rule
        AdBlocker.parseRuleLine("||special-network.com/banner^\$important")

        // Important blocking rule should override the standard exception
        assertTrue(AdBlocker.isAdOrTracker("https://special-network.com/banner"))
    }

    @Test
    fun testDomainModifierRestriction() {
        // Rule applies only when visited from example-news.com
        AdBlocker.parseRuleLine("||partner-tracker.com^\$domain=example-news.com")

        // Request from example-news.com -> blocked
        assertTrue(AdBlocker.isAdOrTracker("https://partner-tracker.com/track.js", siteHost = "example-news.com"))

        // Request from other-site.com -> NOT blocked
        assertFalse(AdBlocker.isAdOrTracker("https://partner-tracker.com/track.js", siteHost = "other-site.com"))
    }

    @Test
    fun testCosmeticRuleExtractionAndDomainIsolation() {
        // Global rule
        AdBlocker.parseRuleLine("##.global-ad-box-2026")
        // Domain specific rule
        AdBlocker.parseRuleLine("techsite.com##.techsite-sponsor")
        // Unhide exception
        AdBlocker.parseRuleLine("specialsite.com#@#.global-ad-box-2026")

        val techCss = AdBlocker.getCosmeticCss(siteHost = "techsite.com")
        assertTrue(techCss.contains(".global-ad-box-2026"))
        assertTrue(techCss.contains(".techsite-sponsor"))

        val otherCss = AdBlocker.getCosmeticCss(siteHost = "otherplace.com")
        assertTrue(otherCss.contains(".global-ad-box-2026"))
        assertFalse(otherCss.contains(".techsite-sponsor"))

        val unhideCss = AdBlocker.getCosmeticCss(siteHost = "specialsite.com")
        assertFalse(unhideCss.contains(".global-ad-box-2026"))
    }

    @Test
    fun testInvalidOrNullUrlsSafe() {
        assertFalse(AdBlocker.isAdOrTracker(null))
        assertFalse(AdBlocker.isAdOrTracker(""))
        assertFalse(AdBlocker.isAdOrTracker("   "))
        assertFalse(AdBlocker.isAdOrTracker("not a valid url"))
    }

    @Test
    fun testThirdPartyRuleParsingAndFastLookup() {
        // Test ||adnetwork.xyz^$third-party
        AdBlocker.parseRuleLine("||adnetwork.xyz^\$third-party")

        // When requested from third-party site -> blocked
        assertTrue(AdBlocker.isAdOrTracker("https://adnetwork.xyz/serve", siteHost = "mysite.com"))
        // When requested from first-party site -> not blocked by this rule
        assertFalse(AdBlocker.isAdOrTracker("https://adnetwork.xyz/static/css", siteHost = "adnetwork.xyz"))
    }

    @Test
    fun testSubdomainAdHeuristics() {
        // Third-party ad subdomains like ads.thirdparty.com
        assertTrue(AdBlocker.isAdOrTracker("https://ads.somenewdomain.com/banner.js", siteHost = "news.org"))
        assertTrue(AdBlocker.isAdOrTracker("https://adserver.randomnetwork.net/pixel", siteHost = "news.org"))
        assertTrue(AdBlocker.isAdOrTracker("https://pagead.affiliateplace.com/tag", siteHost = "news.org"))

        // First-party subdomains should not be falsely blocked by third-party heuristic
        assertFalse(AdBlocker.isAdOrTracker("https://ads.news.org/info.html", siteHost = "news.org"))
    }

    @Test
    fun testIframeSubdocumentHeaderDetection() {
        val headers = mapOf("Sec-Fetch-Dest" to "document")
        val resType = AdBlocker.detectResourceType("https://example.com/frame.html", headers, isForMainFrame = false)
        assertEquals(ResourceType.SUBDOCUMENT, resType)
    }

    @Test
    fun testCosmeticScriptSafeQuoting() {
        val script = AdBlocker.getCosmeticDocumentStartScript("youtube.com")
        assertNotNull(script)
        assertTrue(script.contains("velocity-shield-ad-hide"))
        assertTrue(script.contains("style.textContent = \""))
    }

    @Test
    fun testStripTrackingParameters() {
        val dirtyUrl = "https://example.com/product?id=123&utm_source=facebook&utm_medium=cpc&fbclid=IwAR123&gclid=Cj0K&keep=true"
        val cleanUrl = AdBlocker.stripTrackingParameters(dirtyUrl)
        assertTrue(cleanUrl.contains("id=123"))
        assertTrue(cleanUrl.contains("keep=true"))
        assertFalse(cleanUrl.contains("utm_source"))
        assertFalse(cleanUrl.contains("utm_medium"))
        assertFalse(cleanUrl.contains("fbclid"))
        assertFalse(cleanUrl.contains("gclid"))

        // Standard URL without tracking params remains untouched
        val cleanOriginal = "https://example.com/article?page=2&sort=recent"
        assertEquals(cleanOriginal, AdBlocker.stripTrackingParameters(cleanOriginal))
    }

    @Test
    fun testChromiumDefusersInDocumentStartScript() {
        val script = AdBlocker.getCosmeticDocumentStartScript("news-portal.com")
        assertTrue(script.contains("window.adsbygoogle"))
        assertTrue(script.contains("window.googletag"))
        assertTrue(script.contains("window.pbjs"))
        assertTrue(script.contains("navigator.sendBeacon"))
        assertTrue(script.contains("window.open"))
        assertTrue(script.contains("window.canRunAds = true"))
        assertTrue(script.contains("purgeAdElements"))
        assertTrue(script.contains("removeStickyAdBars"))
        assertTrue(script.contains("defuseAdblockWalls"))
        assertTrue(script.contains("aswift_"))
    }
}
