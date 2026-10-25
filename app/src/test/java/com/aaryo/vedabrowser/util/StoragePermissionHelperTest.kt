package com.aaryo.vedabrowser.util

import android.Manifest
import android.os.Build
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoragePermissionHelperTest {

    @Test
    fun testRequiredPermissionsArrayConsistency() {
        val permissions = StoragePermissionHelper.getRequiredStoragePermissions()
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            assertTrue(permissions.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            assertTrue(permissions.contains(Manifest.permission.READ_EXTERNAL_STORAGE))
        } else {
            assertEquals(0, permissions.size)
        }
    }

    @Test
    fun testPermissionRequiredFlagMatchesApiLevel() {
        val required = StoragePermissionHelper.isStoragePermissionRequired()
        val expected = Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
        assertEquals(expected, required)
    }
}
