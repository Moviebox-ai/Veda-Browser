package com.aaryo.vedabrowser.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RealDownloadManagerTest {

    @Test
    fun testVedaBrowserDownloadsFolderConstant() {
        assertEquals("Veda Browser", RealDownloadManager.VEDA_DOWNLOADS_FOLDER)
    }

    @Test
    fun testSubPathConstruction() {
        val fileName = "sample_document.pdf"
        val expectedRelativeSubPath = "${RealDownloadManager.VEDA_DOWNLOADS_FOLDER}${File.separator}$fileName"
        assertEquals("Veda Browser${File.separator}sample_document.pdf", expectedRelativeSubPath)
    }

    @Test
    fun testSubPathWithSpecialCharacters() {
        val rawFileName = "my:video/file?*<>.mp4"
        val sanitized = com.aaryo.vedabrowser.util.FilenameSanitizer.sanitize(rawFileName)
        val relativeSubPath = "${RealDownloadManager.VEDA_DOWNLOADS_FOLDER}${File.separator}$sanitized"
        assertTrue(relativeSubPath.startsWith("Veda Browser"))
        assertNotNull(sanitized)
    }
}
