package com.aaryo.vedabrowser.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.aaryo.vedabrowser.MainActivity
import java.io.File
import java.util.Locale

object DownloadNotificationHelper {

    const val CHANNEL_ACTIVE_ID = "veda_downloads_active"
    const val CHANNEL_COMPLETE_ID = "veda_downloads_complete"

    const val FOREGROUND_SERVICE_NOTIFICATION_ID = 9001

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val activeChannel = NotificationChannel(
                CHANNEL_ACTIVE_ID,
                "Active Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time progress for ongoing downloads"
                setShowBadge(false)
            }

            val completeChannel = NotificationChannel(
                CHANNEL_COMPLETE_ID,
                "Completed Downloads",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when file downloads finish with instant open options"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(activeChannel)
            notificationManager.createNotificationChannel(completeChannel)
        }
    }

    fun buildForegroundNotification(
        context: Context,
        activeCount: Int,
        latestFileName: String,
        downloadedBytes: Long,
        totalBytes: Long
    ): Notification {
        initChannels(context)

        val percent = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
        val isIndeterminate = totalBytes <= 0L

        val title = if (activeCount > 1) {
            "Downloading $activeCount files"
        } else {
            "Downloading $latestFileName"
        }

        val contentText = if (totalBytes > 0) {
            "${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)} ($percent%)"
        } else {
            "${formatBytes(downloadedBytes)} downloaded"
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_SCREEN", "DOWNLOADS")
        }
        val pendingMainIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ACTIVE_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSubText("Veda Browser")
            .setProgress(100, percent, isIndeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingMainIntent)
            .setColor(0xFF00E5FF.toInt())
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun showCompletionNotification(
        context: Context,
        notificationId: Int,
        fileName: String,
        file: File?,
        mimeType: String,
        totalBytes: Long
    ) {
        initChannels(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val contentIntent = if (file != null && file.exists()) {
            val uri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (_: Exception) {
                Uri.fromFile(file)
            }

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, if (mimeType.isNotBlank()) mimeType else "*/*")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            PendingIntent.getActivity(
                context,
                notificationId,
                Intent.createChooser(viewIntent, "Open $fileName"),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("OPEN_SCREEN", "DOWNLOADS")
            }
            PendingIntent.getActivity(
                context,
                notificationId,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val sizeText = if (totalBytes > 0) " (${formatBytes(totalBytes)})" else ""

        val builder = NotificationCompat.Builder(context, CHANNEL_COMPLETE_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Download completed")
            .setContentText("$fileName$sizeText")
            .setSubText("Saved to Internal Storage")
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setColor(0xFF10B981.toInt())

        if (file != null && file.exists()) {
            val openAction = NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_view,
                "Open",
                contentIntent
            ).build()
            builder.addAction(openAction)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                val uri = try {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                } catch (_: Exception) {
                    Uri.fromFile(file)
                }
                putExtra(Intent.EXTRA_STREAM, uri)
                type = if (mimeType.isNotBlank()) mimeType else "*/*"
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val pendingShare = PendingIntent.getActivity(
                context,
                notificationId + 50000,
                Intent.createChooser(shareIntent, "Share $fileName"),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val shareAction = NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_share,
                "Share",
                pendingShare
            ).build()
            builder.addAction(shareAction)
        }

        notificationManager.notify(notificationId, builder.build())
    }

    fun showFailureNotification(
        context: Context,
        notificationId: Int,
        fileName: String,
        reason: String?
    ) {
        initChannels(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(context, CHANNEL_COMPLETE_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Download failed")
            .setContentText("$fileName • ${reason ?: "Connection error"}")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setColor(0xFFEF4444.toInt())

        notificationManager.notify(notificationId, builder.build())
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
            else -> "$bytes B"
        }
    }
}
