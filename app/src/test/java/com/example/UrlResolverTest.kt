package com.example

import com.example.domain.model.SearchEngine
import com.example.util.UrlResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlResolverTest {

    @Test
    fun testEmptyInput_resolvesToInternalHome() {
        val result = UrlResolver.resolve("")
        assertEquals(UrlResolver.VELOCITY_HOME, result.url)
        assertEquals(UrlResolver.ResolvedType.INTERNAL, result.type)
    }

    @Test
    fun testDirectUrl_preserved() {
        val httpsResult = UrlResolver.resolve("https://example.com/test")
        assertEquals("https://example.com/test", httpsResult.url)
        assertEquals(UrlResolver.ResolvedType.WEB_URL, httpsResult.type)

        val httpResult = UrlResolver.resolve("http://insecure.site")
        assertEquals("http://insecure.site", httpResult.url)
        assertEquals(UrlResolver.ResolvedType.WEB_URL, httpResult.type)
    }

    @Test
    fun testDomain_escalatesToHttps() {
        val result = UrlResolver.resolve("github.com/kotlin")
        assertEquals("https://github.com/kotlin", result.url)
        assertEquals(UrlResolver.ResolvedType.WEB_URL, result.type)
    }

    @Test
    fun testIpAndLocalhost_prependsHttp() {
        val ipResult = UrlResolver.resolve("192.168.1.1:8080")
        assertEquals("http://192.168.1.1:8080", ipResult.url)
        assertEquals(UrlResolver.ResolvedType.WEB_URL, ipResult.type)

        val localResult = UrlResolver.resolve("localhost:3000/api")
        assertEquals("http://localhost:3000/api", localResult.url)
        assertEquals(UrlResolver.ResolvedType.WEB_URL, localResult.type)
    }

    @Test
    fun testDangerousSchemes_blocked() {
        val jsResult = UrlResolver.resolve("javascript:alert(1)")
        assertEquals(UrlResolver.ResolvedType.BLOCKED_SCHEME, jsResult.type)

        val fileResult = UrlResolver.resolve("file:///sdcard/passwords.txt")
        assertEquals(UrlResolver.ResolvedType.BLOCKED_SCHEME, fileResult.type)

        val contentResult = UrlResolver.resolve("content://media/external/images")
        assertEquals(UrlResolver.ResolvedType.BLOCKED_SCHEME, contentResult.type)
    }

    @Test
    fun testSearchQuery_encodedCorrectly() {
        val queryResult = UrlResolver.resolve("how to build an android browser", SearchEngine.DUCKDUCKGO)
        assertEquals(UrlResolver.ResolvedType.SEARCH_QUERY, queryResult.type)
        assertTrue(queryResult.url.contains("duckduckgo.com/?q=how+to+build+an+android+browser"))
    }

    @Test
    fun testExtractDisplayHost() {
        assertEquals("example.com", UrlResolver.extractDisplayHost("https://www.example.com/path?arg=1"))
        assertEquals("github.com", UrlResolver.extractDisplayHost("https://github.com/torvalds/linux"))
        assertEquals("", UrlResolver.extractDisplayHost("velocity:home"))
    }

    @Test
    fun testIsSecure() {
        assertTrue(UrlResolver.isSecure("https://secure.site"))
        assertFalse(UrlResolver.isSecure("http://insecure.site"))
        assertFalse(UrlResolver.isSecure("velocity:home"))
    }
}
