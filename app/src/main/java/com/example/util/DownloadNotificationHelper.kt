package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.MainActivity
import com.example.R
import java.io.File
import java.util.Locale

/**
 * System Notification Manager for Downloads and File Uploads in Velocity Browser.
 * Handles channel creation, ongoing progress notifications, download completion with file opening intents,
 * and upload status notifications visible in the Android system notification shade.
 */
object DownloadNotificationHelper {

    private const val CHANNEL_PROGRESS_ID = "velocity_downloads_progress"
    private const val CHANNEL_COMPLETE_ID = "velocity_downloads_complete"
    private const val CHANNEL_UPLOADS_ID = "velocity_uploads"

    private const val UPLOAD_NOTIFICATION_BASE_ID = 90000

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            // Channel for active in-progress downloads
            val progressChannel = NotificationChannel(
                CHANNEL_PROGRESS_ID,
                "Active Downloads",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows real-time download progress and speed in status bar"
                setShowBadge(false)
            }

            // Channel for completed or failed downloads (High importance = heads-up alert / sound on finish)
            val completeChannel = NotificationChannel(
                CHANNEL_COMPLETE_ID,
                "Completed Downloads",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a file finishes downloading"
                setShowBadge(true)
                enableVibration(true)
            }

            // Channel for file uploads
            val uploadChannel = NotificationChannel(
                CHANNEL_UPLOADS_ID,
                "File Uploads",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows file upload activity and selections in status bar"
                setShowBadge(true)
            }

            manager.createNotificationChannel(progressChannel)
            manager.createNotificationChannel(completeChannel)
            manager.createNotificationChannel(uploadChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                AppLogger.d("DownloadNotificationHelper", "POST_NOTIFICATIONS permission not granted yet")
                return false
            }
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun notifyProgress(
        context: Context,
        downloadId: Long,
        filename: String,
        downloadedBytes: Long,
        totalBytes: Long,
        speedBytesPerSec: Long
    ) {
        if (!hasNotificationPermission(context)) return

        createChannels(context)

        val progressPercent = if (totalBytes > 0) {
            ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
        } else {
            -1
        }

        val speedText = if (speedBytesPerSec > 0) formatSpeed(speedBytesPerSec) else ""
        val downloadedText = formatBytes(downloadedBytes)
        val totalText = if (totalBytes > 0) formatBytes(totalBytes) else "Unknown"

        val contentText = buildString {
            if (progressPercent >= 0) {
                append("$progressPercent% • ")
            }
            append("$downloadedText / $totalText")
            if (speedText.isNotEmpty()) {
                append(" • $speedText")
            }
        }

        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "downloads")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            downloadId.toInt(),
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_PROGRESS_ID)
            .setSmallIcon(R.drawable.ic_notif_download)
            .setContentTitle(filename)
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)

        if (progressPercent >= 0) {
            builder.setProgress(100, progressPercent, false)
        } else {
            builder.setProgress(0, 0, true)
        }

        try {
            NotificationManagerCompat.from(context).notify(downloadId.toInt(), builder.build())
        } catch (e: Exception) {
            AppLogger.w("DownloadNotificationHelper", "Could not show progress notification: ${e.message}")
        }
    }

    fun notifyCompleted(
        context: Context,
        downloadId: Long,
        filename: String,
        targetFile: File,
        mimeType: String
    ) {
        if (!hasNotificationPermission(context)) return

        createChannels(context)

        // Cancel the ongoing progress notification
        cancel(context, downloadId)

        val openIntent = try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, targetFile)
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } catch (_: Exception) {
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "downloads")
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            downloadId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fileSizeText = if (targetFile.exists()) formatBytes(targetFile.length()) else ""
        val subtitle = if (fileSizeText.isNotEmpty()) "$filename • $fileSizeText" else filename

        val builder = NotificationCompat.Builder(context, CHANNEL_COMPLETE_ID)
            .setSmallIcon(R.drawable.ic_notif_download_done)
            .setContentTitle("Download complete")
            .setContentText(subtitle)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_STATUS)

        try {
            NotificationManagerCompat.from(context).notify(downloadId.toInt(), builder.build())
        } catch (e: Exception) {
            AppLogger.w("DownloadNotificationHelper", "Could not show completed notification: ${e.message}")
        }
    }

    fun notifyFailed(
        context: Context,
        downloadId: Long,
        filename: String,
        errorMessage: String
    ) {
        if (!hasNotificationPermission(context)) return

        createChannels(context)
        cancel(context, downloadId)

        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "downloads")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            downloadId.toInt(),
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_COMPLETE_ID)
            .setSmallIcon(R.drawable.ic_notif_error)
            .setContentTitle("Download failed")
            .setContentText("$filename • $errorMessage")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_ERROR)

        try {
            NotificationManagerCompat.from(context).notify(downloadId.toInt(), builder.build())
        } catch (e: Exception) {
            AppLogger.w("DownloadNotificationHelper", "Could not show failure notification: ${e.message}")
        }
    }

    fun notifyUpload(
        context: Context,
        fileCount: Int,
        firstFileName: String?
    ) {
        if (!hasNotificationPermission(context)) return

        createChannels(context)

        val title = if (fileCount > 1) "Uploading $fileCount files" else "File uploaded"
        val subtitle = if (!firstFileName.isNullOrBlank()) {
            if (fileCount > 1) "$firstFileName and ${fileCount - 1} more" else "$firstFileName selected for upload"
        } else {
            "File selected for webpage upload"
        }

        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            UPLOAD_NOTIFICATION_BASE_ID,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_UPLOADS_ID)
            .setSmallIcon(R.drawable.ic_notif_upload)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_STATUS)

        try {
            NotificationManagerCompat.from(context).notify(UPLOAD_NOTIFICATION_BASE_ID, builder.build())
        } catch (e: Exception) {
            AppLogger.w("DownloadNotificationHelper", "Could not show upload notification: ${e.message}")
        }
    }

    fun cancel(context: Context, downloadId: Long) {
        try {
            NotificationManagerCompat.from(context).cancel(downloadId.toInt())
        } catch (_: Exception) {}
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var value = bytes.toDouble()
        var unitIndex = 0
        while (value >= 1024 && unitIndex < units.size - 1) {
            value /= 1024
            unitIndex++
        }
        return if (unitIndex == 0) "${bytes.toInt()} B" else String.format(Locale.US, "%.1f %s", value, units[unitIndex])
    }

    fun formatSpeed(bytesPerSec: Long): String {
        return "${formatBytes(bytesPerSec)}/s"
    }
}
