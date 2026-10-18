package com.example

import com.example.util.FilenameSanitizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FilenameSanitizerTest {

    @Test
    fun testPathTraversalStripped() {
        val sanitized = FilenameSanitizer.sanitize("../../etc/passwd.txt")
        assertFalse(sanitized.contains(".."))
        assertFalse(sanitized.contains("/"))
        assertEquals("passwd.txt", sanitized)
    }

    @Test
    fun testIllegalCharactersReplaced() {
        val sanitized = FilenameSanitizer.sanitize("test:file*name?2.pdf")
        assertFalse(sanitized.contains(":"))
        assertFalse(sanitized.contains("*"))
        assertFalse(sanitized.contains("?"))
        assertEquals("test_file_name_2.pdf", sanitized)
    }

    @Test
    fun testEmptyFallback() {
        val sanitized = FilenameSanitizer.sanitize("", mimeType = "image/png", fallbackDefault = "download")
        assertEquals("download.png", sanitized)
    }
}
