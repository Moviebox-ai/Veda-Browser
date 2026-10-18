package com.example

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.DownloadNotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DownloadNotificationTest {

    @Test
    fun testCreateChannelsAndNotifyUpload() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        DownloadNotificationHelper.createChannels(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val progressChannel = notificationManager.getNotificationChannel("velocity_downloads_progress")
        val completeChannel = notificationManager.getNotificationChannel("velocity_downloads_complete")
        val uploadChannel = notificationManager.getNotificationChannel("velocity_uploads")

        assertNotNull("Progress channel must exist", progressChannel)
        assertNotNull("Complete channel must exist", completeChannel)
        assertNotNull("Upload channel must exist", uploadChannel)

        // Test upload notification
        DownloadNotificationHelper.notifyUpload(context, 1, "profile_pic.png")

        // Test progress notification
        DownloadNotificationHelper.notifyProgress(context, 101L, "sample.zip", 5000L, 10000L, 250000L)

        // Test completed notification
        val testFile = File(context.cacheDir, "sample.zip").apply { writeText("dummy content") }
        DownloadNotificationHelper.notifyCompleted(context, 101L, "sample.zip", testFile, "application/zip")
    }

    @Test
    fun testFormatBytesAndSpeed() {
        assertEquals("0 B", DownloadNotificationHelper.formatBytes(0))
        assertEquals("512 B", DownloadNotificationHelper.formatBytes(512))
        assertTrue(DownloadNotificationHelper.formatBytes(1048576).contains("MB"))
        assertTrue(DownloadNotificationHelper.formatSpeed(1048576).contains("MB/s"))
    }
}
