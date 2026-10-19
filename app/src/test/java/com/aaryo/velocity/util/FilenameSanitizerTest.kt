package com.aaryo.velocity.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilenameSanitizerTest {

    @Test
    fun testDirectoryTraversalStripped() {
        val unsafe = "../../etc/passwd"
        val safe = FilenameSanitizer.sanitize(unsafe)
        assertFalse(safe.contains(".."))
        assertFalse(safe.contains("/"))
        assertFalse(safe.contains("\\"))
    }

    @Test
    fun testIllegalCharactersReplaced() {
        val raw = "report:2024*q1?final<draft>|\"v2\".pdf"
        val safe = FilenameSanitizer.sanitize(raw)
        assertFalse(safe.contains(":"))
        assertFalse(safe.contains("*"))
        assertFalse(safe.contains("?"))
        assertFalse(safe.contains("<"))
        assertFalse(safe.contains(">"))
        assertFalse(safe.contains("|"))
        assertFalse(safe.contains("\""))
        assertTrue(safe.endsWith(".pdf"))
    }

    @Test
    fun testNullOrEmptyFallsBackToDefault() {
        assertEquals("download", FilenameSanitizer.sanitize(null))
        assertEquals("download", FilenameSanitizer.sanitize(""))
        assertEquals("custom_fallback", FilenameSanitizer.sanitize("   ", defaultName = "custom_fallback"))
    }

    @Test
    fun testPreservesValidExtensionsOnLengthCap() {
        val longBase = "A".repeat(150) + ".tar.gz"
        val safe = FilenameSanitizer.sanitize(longBase)
        assertTrue(safe.length <= 128)
        assertTrue(safe.endsWith(".gz"))
    }
}
