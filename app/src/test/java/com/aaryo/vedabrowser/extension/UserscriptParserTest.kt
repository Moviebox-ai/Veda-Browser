package com.aaryo.vedabrowser.extension

import com.aaryo.vedabrowser.model.RunAt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserscriptParserTest {

    @Test
    fun testParseFullMetadataBlock() {
        val script = """
            // ==UserScript==
            // @name         Dark Reader Custom
            // @version      2.4.1
            // @description  A stylish dark reader theme
            // @author       Alice Developer
            // @match        https://example.com/*
            // @include      https://*.wikipedia.org/*
            // @exclude      https://example.com/checkout/*
            // @run-at       document-start
            // @grant        GM_getValue
            // @grant        GM_setValue
            // @grant        GM_xmlhttpRequest
            // @connect      api.example.com
            // @updateURL    https://example.com/script.user.js
            // @icon         https://example.com/icon.png
            // ==/UserScript==
            console.log('Script loaded');
        """.trimIndent()

        val parsed = UserscriptParser.parse(script)

        assertEquals("Dark Reader Custom", parsed.name)
        assertEquals("2.4.1", parsed.version)
        assertEquals("A stylish dark reader theme", parsed.description)
        assertEquals("Alice Developer", parsed.author)
        assertEquals(RunAt.DOCUMENT_START, parsed.runAt)

        assertEquals(2, parsed.matchPatterns.size)
        assertTrue(parsed.matchPatterns.contains("https://example.com/*"))
        assertTrue(parsed.matchPatterns.contains("https://*.wikipedia.org/*"))

        assertEquals(1, parsed.excludePatterns.size)
        assertEquals("https://example.com/checkout/*", parsed.excludePatterns[0])

        assertEquals(3, parsed.grants.size)
        assertTrue(parsed.grants.contains("GM_getValue"))
        assertTrue(parsed.grants.contains("GM_setValue"))
        assertTrue(parsed.grants.contains("GM_xmlhttpRequest"))

        assertEquals(1, parsed.connectRules.size)
        assertEquals("api.example.com", parsed.connectRules[0])

        assertEquals("https://example.com/script.user.js", parsed.updateUrl)
        assertEquals("https://example.com/icon.png", parsed.iconUrl)
        assertFalse(parsed.isBuiltIn)
        assertTrue(parsed.enabled)
    }

    @Test
    fun testDefaultFallbackWhenNoMetadata() {
        val plainCode = "console.log('Hello world without header');"
        val parsed = UserscriptParser.parse(plainCode)

        assertEquals("Untitled Script", parsed.name)
        assertEquals("1.0", parsed.version)
        assertEquals("Anonymous", parsed.author)
        assertEquals("", parsed.description)
        assertEquals(listOf("<all_urls>"), parsed.matchPatterns)
        assertEquals(RunAt.DOCUMENT_END, parsed.runAt)
        assertTrue(parsed.grants.isEmpty())
    }

    @Test
    fun testParseRunAtVariants() {
        val scriptStart = """
            // ==UserScript==
            // @name Run Start
            // @run-at document-start
            // ==/UserScript==
        """.trimIndent()
        assertEquals(RunAt.DOCUMENT_START, UserscriptParser.parse(scriptStart).runAt)

        val scriptIdle = """
            // ==UserScript==
            // @name Run Idle
            // @run-at document-idle
            // ==/UserScript==
        """.trimIndent()
        assertEquals(RunAt.DOCUMENT_IDLE, UserscriptParser.parse(scriptIdle).runAt)

        val scriptEnd = """
            // ==UserScript==
            // @name Run End
            // @run-at document-end
            // ==/UserScript==
        """.trimIndent()
        assertEquals(RunAt.DOCUMENT_END, UserscriptParser.parse(scriptEnd).runAt)
    }

    @Test(expected = UserscriptParser.UserscriptParseException::class)
    fun testScriptSizeLimitExceeded() {
        val hugeCode = "A".repeat(1024 * 1024 + 10)
        UserscriptParser.parse(hugeCode)
    }

    @Test
    fun testGrantNoneIgnored() {
        val script = """
            // ==UserScript==
            // @name No Grants
            // @grant none
            // ==/UserScript==
        """.trimIndent()
        val parsed = UserscriptParser.parse(script)
        assertTrue(parsed.grants.isEmpty())
    }
}
