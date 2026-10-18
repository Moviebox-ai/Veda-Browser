package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.StorageUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StorageUtilsTest {

    @Test
    fun testGetAppDownloadDirectoryUsesAppName() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        val downloadDir = StorageUtils.getAppDownloadDirectory(context)

        assertNotNull(downloadDir)
        assertTrue("Download directory name should match app name", downloadDir.name == appName)
        assertTrue("Download directory must exist or be created", downloadDir.exists())
    }

    @Test
    fun testGetDisplayFolderPath() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val folderPath = StorageUtils.getDisplayFolderPath(context)
        assertEquals("Download/Velocity", folderPath)
    }
}
