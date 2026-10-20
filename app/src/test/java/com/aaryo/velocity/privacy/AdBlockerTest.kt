package com.aaryo.velocity.privacy

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

        AdBlocker.removeFromAllowlist(testDomain)
        assertFalse(AdBlocker.isAllowlisted(testDomain))
    }

    @Test
    fun testLoginCriticalDomainsPreservedInStandardMode() {
        AdBlocker.isStrictMode = false

        // Essential identity/login services must not be flagged in standard mode
        assertFalse(AdBlocker.isAdOrTracker("https://connect.facebook.net/en_US/sdk.js"))
        assertFalse(AdBlocker.isAdOrTracker("https://cdn.branch.io/branch-latest.min.js"))
        assertFalse(AdBlocker.isAdOrTracker("https://cdn.segment.com/analytics.js/v1/key/analytics.min.js"))
        assertFalse(AdBlocker.isAdOrTracker("https://accounts.google.com/o/oauth2/auth"))
    }

    @Test
    fun testAllowlistedSiteBypassesAdCheck() {
        val site = "trusted-partner.com"
        AdBlocker.addToAllowlist(site)

        val trackerUrl = "https://doubleclick.net/pagead/ads"
        // When request is for an allowlisted host, it should return false (bypassed)
        assertFalse(AdBlocker.isAdOrTracker(trackerUrl, siteHost = site))

        AdBlocker.removeFromAllowlist(site)
    }

    @Test
    fun testCommonAdNetworksBlocked() {
        // Google Ads & DoubleClick
        assertTrue(AdBlocker.isAdOrTracker("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://googleads.g.doubleclick.net/pagead/ads?client=ca-pub-123"))
        assertTrue(AdBlocker.isAdOrTracker("https://securepubads.g.doubleclick.net/gampad/ads"))
        assertTrue(AdBlocker.isAdOrTracker("https://www.googletagservices.com/tag/js/gpt.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://adservice.google.com/adsid/integrator.sync.js"))

        // Native / Display / Video ad networks
        assertTrue(AdBlocker.isAdOrTracker("https://aax.amazon-adsystem.com/e/dtb/bid"))
        assertTrue(AdBlocker.isAdOrTracker("https://static.criteo.net/js/ld/ld.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://cdn.taboola.com/libtrc/loader.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://widgets.outbrain.com/outbrain.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://ib.adnxs.com/jpt"))
        assertTrue(AdBlocker.isAdOrTracker("https://serve.popads.net/serve.js"))
        assertTrue(AdBlocker.isAdOrTracker("https://jsc.mgid.com/site/123.js"))
    }

    @Test
    fun testCleanWebsitesNotBlocked() {
        assertFalse(AdBlocker.isAdOrTracker("https://en.wikipedia.org/wiki/Kotlin"))
        assertFalse(AdBlocker.isAdOrTracker("https://github.com/torvalds/linux"))
        assertFalse(AdBlocker.isAdOrTracker("https://www.google.com/search?q=android"))
        assertFalse(AdBlocker.isAdOrTracker("https://stackoverflow.com/questions/12345/android-webview"))
    }

    @Test
    fun testInvalidOrNullUrlsSafe() {
        assertFalse(AdBlocker.isAdOrTracker(null))
        assertFalse(AdBlocker.isAdOrTracker(""))
        assertFalse(AdBlocker.isAdOrTracker("not a valid url"))
    }
}
