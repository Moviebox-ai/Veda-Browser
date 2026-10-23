package com.aaryo.vedabrowser.webview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

class DesktopModeTest {

    @Test
    fun testUserAgentStrings() {
        // Desktop UA must indicate Windows NT 10.0 and Chrome without Mobile
        assertTrue(DESKTOP_USER_AGENT.contains("Windows NT 10.0"))
        assertTrue(DESKTOP_USER_AGENT.contains("Chrome/"))
        assertFalse(DESKTOP_USER_AGENT.contains("Mobile"))

        // Mobile UA must indicate Android and Mobile
        assertTrue(MOBILE_USER_AGENT.contains("Android"))
        assertTrue(MOBILE_USER_AGENT.contains("Mobile"))
    }

    @Test
    fun testDesktopViewportScriptContains1280Width() {
        assertTrue(DESKTOP_VIEWPORT_JS.contains("width=1280"))
        assertTrue(DESKTOP_VIEWPORT_JS.contains("userAgentData"))
        assertTrue(DESKTOP_VIEWPORT_JS.contains("platform: 'Windows'"))
    }

    @Test
    fun testRealWorldAdUrls() {
        val urls = listOf(
            "https://securepubads.g.doubleclick.net/tag/js/gpt.js",
            "https://c.amazon-adsystem.com/aax2/apstag.js",
            "https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js",
            "https://adservice.google.com/adsid/google/ui",
            "https://taboola.com/libtrc/example/loader.js",
            "https://outbrain.com/widget/loader.js",
            "https://clmbtech.com/track/pixel.gif",
            "https://jioads.com/sdk/banner.js",
            "https://adnxs.com/seg?add=1",
            "https://googleads.g.doubleclick.net/pagead/ads?client=ca-pub-123",
            "https://static.criteo.net/js/ld/ld.js"
        )
        for (u in urls) {
            val blocked = com.aaryo.vedabrowser.privacy.AdBlocker.isAdOrTracker(u, siteHost = "theverge.com")
            assertTrue("Expected $u to be blocked!", blocked)
        }
    }

    @Test
    fun testConvertMobileUrlToDesktop() {
        val helper = UrlTestHelper()

        assertEquals(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            helper.convertMobileUrlToDesktop("https://m.youtube.com/watch?v=dQw4w9WgXcQ")
        )
        assertEquals(
            "https://www.facebook.com/profile",
            helper.convertMobileUrlToDesktop("https://m.facebook.com/profile")
        )
        assertEquals(
            "https://twitter.com/explore",
            helper.convertMobileUrlToDesktop("https://mobile.twitter.com/explore")
        )
        assertEquals(
            "https://en.wikipedia.org/wiki/Android",
            helper.convertMobileUrlToDesktop("https://en.m.wikipedia.org/wiki/Android")
        )
        assertEquals(
            "https://wikipedia.org/wiki/Main_Page",
            helper.convertMobileUrlToDesktop("https://m.wikipedia.org/wiki/Main_Page")
        )
        assertEquals(
            "https://www.imdb.com/title/tt0111161/",
            helper.convertMobileUrlToDesktop("https://m.imdb.com/title/tt0111161/")
        )
        // Standard URLs remain intact
        assertEquals(
            "https://www.google.com/search?q=kotlin",
            helper.convertMobileUrlToDesktop("https://www.google.com/search?q=kotlin")
        )
        assertEquals(
            "https://github.com/torvalds/linux",
            helper.convertMobileUrlToDesktop("https://github.com/torvalds/linux")
        )
    }

    @Test
    fun testConvertDesktopUrlToMobile() {
        val helper = UrlTestHelper()

        assertEquals(
            "https://m.youtube.com/watch?v=123",
            helper.convertDesktopUrlToMobile("https://www.youtube.com/watch?v=123")
        )
        assertEquals(
            "https://m.facebook.com/home",
            helper.convertDesktopUrlToMobile("https://www.facebook.com/home")
        )
        assertEquals(
            "https://m.imdb.com/title/tt123",
            helper.convertDesktopUrlToMobile("https://www.imdb.com/title/tt123")
        )
        assertEquals(
            "https://en.m.wikipedia.org/wiki/Kotlin",
            helper.convertDesktopUrlToMobile("https://en.wikipedia.org/wiki/Kotlin")
        )
    }

    private class UrlTestHelper {
        fun convertMobileUrlToDesktop(url: String): String {
            return try {
                val uri = URI(url)
                val host = uri.host ?: return url
                val newHost = when {
                    host.equals("m.youtube.com", ignoreCase = true) -> "www.youtube.com"
                    host.equals("m.facebook.com", ignoreCase = true) -> "www.facebook.com"
                    host.equals("mobile.twitter.com", ignoreCase = true) -> "twitter.com"
                    host.equals("m.imdb.com", ignoreCase = true) -> "www.imdb.com"
                    host.endsWith(".m.wikipedia.org", ignoreCase = true) -> host.replace(".m.wikipedia.org", ".wikipedia.org")
                    host.equals("m.wikipedia.org", ignoreCase = true) -> "wikipedia.org"
                    host.startsWith("m.", ignoreCase = true) -> host.removePrefix("m.")
                    host.startsWith("mobile.", ignoreCase = true) -> host.removePrefix("mobile.")
                    else -> host
                }
                if (newHost != host) {
                    URI(uri.scheme, uri.userInfo, newHost, uri.port, uri.rawPath, uri.rawQuery, uri.rawFragment).toString()
                } else {
                    url
                }
            } catch (_: Exception) {
                url
            }
        }

        fun convertDesktopUrlToMobile(url: String): String {
            return try {
                val uri = URI(url)
                val host = uri.host ?: return url
                val newHost = when {
                    host.equals("www.youtube.com", ignoreCase = true) -> "m.youtube.com"
                    host.equals("www.facebook.com", ignoreCase = true) -> "m.facebook.com"
                    host.equals("www.imdb.com", ignoreCase = true) -> "m.imdb.com"
                    host.endsWith(".wikipedia.org", ignoreCase = true) && !host.contains(".m.") -> {
                        host.replace(".wikipedia.org", ".m.wikipedia.org")
                    }
                    else -> host
                }
                if (newHost != host) {
                    URI(uri.scheme, uri.userInfo, newHost, uri.port, uri.rawPath, uri.rawQuery, uri.rawFragment).toString()
                } else {
                    url
                }
            } catch (_: Exception) {
                url
            }
        }
    }
}
