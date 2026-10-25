package com.aaryo.vedabrowser.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Utility to manage runtime storage permission checks and requests across different
 * Android versions according to Google Play and Scoped Storage guidelines.
 */
object StoragePermissionHelper {

    /**
     * Checks whether the app has the necessary storage permissions to download and save files.
     * - Android 11+ (API 30+): Scoped storage handles app downloads without requiring broad WRITE_EXTERNAL_STORAGE.
     * - Android 10 and lower (API <= 29): WRITE_EXTERNAL_STORAGE is a dangerous runtime permission.
     */
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            true
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Returns the array of storage permissions required for the current Android SDK level.
     */
    fun getRequiredStoragePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        } else {
            emptyArray()
        }
    }

    /**
     * Indicates whether the current device requires a runtime permission prompt for storage.
     */
    fun isStoragePermissionRequired(): Boolean {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
    }
}
