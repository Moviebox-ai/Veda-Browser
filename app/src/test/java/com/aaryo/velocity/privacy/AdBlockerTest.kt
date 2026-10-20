package com.aaryo.velocity.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdBlockerTest {

    @Before
    fun setUp() {
        AdBlocker.isStrictMode = false
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
    fun testLoginCriticalDomainsPreservedInStandardMode() {
        AdBlocker.isStrictMode = false

        // Essential identity and OAuth login endpoints must not be blocked
        assertFalse(AdBlocker.isAdOrTracker("https://connect.facebook.net/en_US/sdk.js"))
        assertFalse(AdBlocker.isAdOrTracker("https://cdn.branch.io/branch-latest.min.js"))
        assertFalse(AdBlocker.isAdOrTracker("https://cdn.segment.com/analytics.js/v1/key/analytics.min.js"))
        assertFalse(AdBlocker.isAdOrTracker("https://accounts.google.com/o/oauth2/auth"))
        assertFalse(AdBlocker.isAdOrTracker("https://appleid.apple.com/auth/authorize"))
        assertFalse(AdBlocker.isAdOrTracker("https://github.com/login/oauth/authorize"))
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

        // Display & Video ad networks
        assertTrue(AdBlocker.isAdOrTracker("https://aax.amazon-adsystem.com/e/dtb/bid"))
        assertTrue(AdBlocker.isAdOrTracker("https://static.criteo.net/js/ld/ld.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://cdn.taboola.com/libtrc/loader.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://widgets.outbrain.com/outbrain.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://ib.adnxs.com/jpt"))
        assertTrue(AdBlocker.isAdOrTracker("https://serve.popads.net/serve.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://jsc.mgid.com/site/123.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://cdn.adpushup.com/123/adpushup.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://inmobi.com/ad/loader.js"))
    }

    @Test
    fun testIndianAdNetworksBlocked() {
        // Colombia Online / Times Internet Ad Network
        assertTrue(AdBlocker.isAdOrTracker("https://static.clmbtech.com/ad/commons/js/colombia.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://colombiaonline.com/serve/ads"))
        assertTrue(AdBlocker.isAdOrTracker("https://clmbe.com/click"))

        // Other Indian networks
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

        // Megaupload / general uploads must NOT be blocked
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
        // Different base domain -> third party
        assertTrue(AdBlocker.isThirdParty("doubleclick.net", "example.com"))
        assertTrue(AdBlocker.isThirdParty("ad.clmbtech.com", "timesofindia.indiatimes.com"))
        assertTrue(AdBlocker.isThirdParty("google-analytics.com", "github.com"))

        // Same base domain -> first party (not third party)
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
    fun testCosmeticRuleExtraction() {
        AdBlocker.parseRuleLine("##.custom-ad-unit-2026")
        val css = AdBlocker.getCosmeticCss()
        assertTrue(css.contains(".adsbygoogle"))
        assertTrue(css.contains(".custom-ad-unit-2026"))
        assertTrue(css.contains("display: none !important"))
    }

    @Test
    fun testInvalidOrNullUrlsSafe() {
        assertFalse(AdBlocker.isAdOrTracker(null))
        assertFalse(AdBlocker.isAdOrTracker(""))
        assertFalse(AdBlocker.isAdOrTracker("   "))
        assertFalse(AdBlocker.isAdOrTracker("not a valid url"))
    }
}
