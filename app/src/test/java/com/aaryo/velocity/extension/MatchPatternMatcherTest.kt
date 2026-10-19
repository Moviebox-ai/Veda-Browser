package com.aaryo.velocity.extension

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchPatternMatcherTest {

    @Test
    fun testAllUrlsMatching() {
        val includes = listOf("<all_urls>")
        val excludes = emptyList<String>()

        assertTrue(MatchPatternMatcher.isUrlMatched(includes, excludes, "https://example.com"))
        assertTrue(MatchPatternMatcher.isUrlMatched(includes, excludes, "http://sub.domain.org/path/file.html"))
        assertTrue(MatchPatternMatcher.isUrlMatched(includes, excludes, "https://news.ycombinator.com/item?id=123"))

        // Internal and non-web schemes must be rejected
        assertFalse(MatchPatternMatcher.isUrlMatched(includes, excludes, "about:blank"))
        assertFalse(MatchPatternMatcher.isUrlMatched(includes, excludes, "velocity://newtab"))
        assertFalse(MatchPatternMatcher.isUrlMatched(includes, excludes, "chrome://settings"))
        assertFalse(MatchPatternMatcher.isUrlMatched(includes, excludes, null))
        assertFalse(MatchPatternMatcher.isUrlMatched(includes, excludes, ""))
    }

    @Test
    fun testDomainWildcardMatching() {
        val pattern = "*://*.google.com/*"

        assertTrue(MatchPatternMatcher.matches(pattern, "https://www.google.com/"))
        assertTrue(MatchPatternMatcher.matches(pattern, "https://google.com/search?q=test"))
        assertTrue(MatchPatternMatcher.matches(pattern, "http://mail.google.com/mail/u/0/"))
        assertFalse(MatchPatternMatcher.matches(pattern, "https://notgoogle.com/"))
        assertFalse(MatchPatternMatcher.matches(pattern, "https://google.com.evil.com/"))
    }

    @Test
    fun testExactSchemeAndPath() {
        val pattern = "https://github.com/aaryo/*"

        assertTrue(MatchPatternMatcher.matches(pattern, "https://github.com/aaryo/velocity"))
        assertTrue(MatchPatternMatcher.matches(pattern, "https://github.com/aaryo/velocity/releases"))
        assertFalse(MatchPatternMatcher.matches(pattern, "http://github.com/aaryo/velocity")) // http rejected
        assertFalse(MatchPatternMatcher.matches(pattern, "https://github.com/otheruser/project"))
    }

    @Test
    fun testExcludePatternOverridesInclude() {
        val includes = listOf("https://example.com/*")
        val excludes = listOf("https://example.com/admin/*", "https://example.com/private/*")

        assertTrue(MatchPatternMatcher.isUrlMatched(includes, excludes, "https://example.com/public"))
        assertTrue(MatchPatternMatcher.isUrlMatched(includes, excludes, "https://example.com/blog/article-1"))
        assertFalse(MatchPatternMatcher.isUrlMatched(includes, excludes, "https://example.com/admin/dashboard"))
        assertFalse(MatchPatternMatcher.isUrlMatched(includes, excludes, "https://example.com/private/user-data"))
    }

    @Test
    fun testEmptyPatterns() {
        assertFalse(MatchPatternMatcher.isUrlMatched(emptyList(), emptyList(), "https://example.com"))
    }
}
