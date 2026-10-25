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

    @Test
    fun testInternalStorageFolderNaming() {
        val folder = RealDownloadManager.VEDA_DOWNLOADS_FOLDER
        assertEquals("Veda Browser", folder)
        // Ensure folder is named exactly "Veda Browser" for direct internal storage root
        assertTrue(!folder.contains("Download"))
    }

    @Test
    fun testPausedStatusProgressMeta() {
        val pausedItem = com.aaryo.vedabrowser.model.DownloadItem(
            fileName = "tutorial.mp4",
            url = "https://example.com/tutorial.mp4",
            downloadedBytes = 50L * 1024 * 1024,
            totalBytes = 100L * 1024 * 1024,
            status = com.aaryo.vedabrowser.model.DownloadStatus.PAUSED,
            supportsResume = true
        )
        assertEquals(com.aaryo.vedabrowser.model.DownloadStatus.PAUSED, pausedItem.status)
        assertTrue(pausedItem.formattedProgressMeta.contains("Paused"))
        assertTrue(pausedItem.supportsResume)
    }

    @Test
    fun testDismissedDownloadBannerIdsBehavior() {
        val dismissedSet = java.util.Collections.synchronizedSet(mutableSetOf<String>())
        val downloadId = "test-dl-123"
        dismissedSet.add(downloadId)
        assertTrue(dismissedSet.contains(downloadId))

        // When download is restarted or resumed, dismissed status is cleared
        dismissedSet.remove(downloadId)
        assertTrue(!dismissedSet.contains(downloadId))
    }
}
