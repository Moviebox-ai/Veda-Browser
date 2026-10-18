package com.example.util

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import com.example.R
import java.io.File

/**
 * Utility for resolving and managing the dedicated app folder in Android storage.
 * Ensures all files, photos, videos, etc. downloaded from the browser are saved
 * in a dedicated folder named after the application (e.g., "Velocity").
 */
object StorageUtils {

    /**
     * Resolves the application folder in Android storage.
     * Uses the app name (e.g. "Velocity") to create a public storage directory:
     * 1. Public Downloads: /storage/emulated/0/Download/Velocity
     * 2. Public Storage Root: /storage/emulated/0/Velocity (if writable on the device)
     * 3. External App Files: Android/data/<pkg>/files/Download/Velocity
     * 4. Internal fallback if storage is unmounted.
     */
    fun getAppDownloadDirectory(context: Context): File {
        val appName = runCatching {
            context.getString(R.string.app_name)
        }.getOrNull()?.trim()?.ifBlank { "Velocity" } ?: "Velocity"

        // 1. Primary: Standard public Downloads directory -> Download/<AppName>
        try {
            val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val appDownloadsDir = File(publicDownloads, appName)
            if (appDownloadsDir.exists() || appDownloadsDir.mkdirs()) {
                if (appDownloadsDir.canWrite()) {
                    return appDownloadsDir
                }
            }
        } catch (e: Exception) {
            AppLogger.w("StorageUtils", "Failed to access public downloads dir: ${e.message}")
        }

        // 2. Secondary: Primary external storage root -> /storage/emulated/0/<AppName>
        try {
            val externalRoot = Environment.getExternalStorageDirectory()
            val directAppDir = File(externalRoot, appName)
            if (directAppDir.exists() || directAppDir.mkdirs()) {
                if (directAppDir.canWrite()) {
                    return directAppDir
                }
            }
        } catch (e: Exception) {
            AppLogger.w("StorageUtils", "Failed to access external storage root dir: ${e.message}")
        }

        // 3. Fallback: External app-specific storage -> Android/data/<pkg>/files/Download/<AppName>
        try {
            val extFiles = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.getExternalFilesDir(null)
            if (extFiles != null) {
                val dir = File(extFiles, appName)
                if (dir.exists() || dir.mkdirs()) {
                    if (dir.canWrite()) return dir
                }
            }
        } catch (e: Exception) {
            AppLogger.w("StorageUtils", "Failed to access external files dir: ${e.message}")
        }

        // 4. Internal fallback
        val internalDir = File(context.filesDir, appName)
        if (!internalDir.exists()) {
            internalDir.mkdirs()
        }
        return internalDir
    }

    /**
     * User-facing display folder string for UI dialogs and download indicators.
     */
    fun getDisplayFolderPath(context: Context): String {
        val appName = runCatching {
            context.getString(R.string.app_name)
        }.getOrNull()?.trim()?.ifBlank { "Velocity" } ?: "Velocity"
        return "Download/$appName"
    }

    /**
     * Scans the saved file with MediaScanner so Android Gallery, Photos,
     * Video players, and Files apps immediately index the new media file.
     */
    fun scanMediaFile(context: Context, file: File, mimeType: String?) {
        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                if (!mimeType.isNullOrBlank()) arrayOf(mimeType) else null,
                null
            )
        } catch (e: Exception) {
            AppLogger.w("StorageUtils", "MediaScanner failed for ${file.name}: ${e.message}")
        }
    }
}
