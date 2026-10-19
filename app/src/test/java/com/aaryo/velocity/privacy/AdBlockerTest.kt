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
    fun testInvalidOrNullUrlsSafe() {
        assertFalse(AdBlocker.isAdOrTracker(null))
        assertFalse(AdBlocker.isAdOrTracker(""))
        assertFalse(AdBlocker.isAdOrTracker("not a valid url"))
    }
}
