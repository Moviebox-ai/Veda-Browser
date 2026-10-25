package com.aaryo.vedabrowser.download

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.webkit.CookieManager
import androidx.core.content.ContextCompat
import com.aaryo.vedabrowser.data.BrowserRepository
import com.aaryo.vedabrowser.model.DownloadItem
import com.aaryo.vedabrowser.model.DownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

class DownloadForegroundService : Service() {

    companion object {
        const val ACTION_START_DOWNLOAD = "com.aaryo.vedabrowser.action.START_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.aaryo.vedabrowser.action.CANCEL_DOWNLOAD"

        const val EXTRA_DOWNLOAD_ID = "extra_download_id"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_FILE_NAME = "extra_file_name"
        const val EXTRA_MIME_TYPE = "extra_mime_type"
        const val EXTRA_USER_AGENT = "extra_user_agent"
        const val EXTRA_DEST_PATH = "extra_dest_path"

        fun startDownload(
            context: Context,
            downloadId: String,
            url: String,
            fileName: String,
            mimeType: String,
            userAgent: String?,
            destinationPath: String
        ) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_FILE_NAME, fileName)
                putExtra(EXTRA_MIME_TYPE, mimeType)
                putExtra(EXTRA_USER_AGENT, userAgent)
                putExtra(EXTRA_DEST_PATH, destinationPath)
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {
                // If foreground service restriction applies, start as regular service
                try {
                    context.startService(intent)
                } catch (_: Exception) {}
            }
        }

        fun cancelDownload(context: Context, downloadId: String) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_CANCEL_DOWNLOAD
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val activeItems = ConcurrentHashMap<String, DownloadItem>()
    private lateinit var repository: BrowserRepository

    override fun onCreate() {
        super.onCreate()
        repository = BrowserRepository(applicationContext)
        DownloadNotificationHelper.initChannels(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START_DOWNLOAD -> {
                val downloadId = intent.getStringExtra(EXTRA_DOWNLOAD_ID) ?: return START_NOT_STICKY
                val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
                val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "download"
                val mimeType = intent.getStringExtra(EXTRA_MIME_TYPE) ?: "*/*"
                val userAgent = intent.getStringExtra(EXTRA_USER_AGENT)
                val destPath = intent.getStringExtra(EXTRA_DEST_PATH) ?: return START_NOT_STICKY

                handleDownload(downloadId, url, fileName, mimeType, userAgent, destPath)
            }
            ACTION_CANCEL_DOWNLOAD -> {
                val downloadId = intent.getStringExtra(EXTRA_DOWNLOAD_ID)
                if (downloadId != null) {
                    activeJobs[downloadId]?.cancel()
                    activeJobs.remove(downloadId)
                    activeItems.remove(downloadId)
                    checkRemainingAndStop()
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun handleDownload(
        downloadId: String,
        url: String,
        fileName: String,
        mimeType: String,
        userAgent: String?,
        destinationPath: String
    ) {
        val initialItem = DownloadItem(
            id = downloadId,
            fileName = fileName,
            url = url,
            mimeType = mimeType,
            status = DownloadStatus.DOWNLOADING
        )
        activeItems[downloadId] = initialItem
        promoteToForegroundIfNeeded(initialItem)

        serviceScope.launch {
            repository.addOrUpdateDownload(initialItem)
            RealDownloadManager.dispatchDownloadProgress(initialItem)
        }

        val destinationFile = File(destinationPath)
        val job = serviceScope.launch {
            var connection: HttpURLConnection? = null
            var downloadedBytes = 0L
            var totalBytes = 0L

            try {
                val targetUrl = URL(url)
                connection = (targetUrl.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = true
                    userAgent?.let { setRequestProperty("User-Agent", it) }
                    val cookie = CookieManager.getInstance().getCookie(url)
                    if (!cookie.isNullOrEmpty()) {
                        setRequestProperty("Cookie", cookie)
                    }
                    setRequestProperty("DNT", "1")
                }
                connection.connect()

                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    totalBytes = connection.contentLengthLong

                    destinationFile.parentFile?.let {
                        if (!it.exists()) it.mkdirs()
                    }

                    connection.inputStream.use { input ->
                        FileOutputStream(destinationFile).use { output ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            var lastProgressTime = System.currentTimeMillis()

                            while (input.read(buffer).also { read = it } != -1 && isActive) {
                                output.write(buffer, 0, read)
                                downloadedBytes += read

                                val now = System.currentTimeMillis()
                                if (now - lastProgressTime >= 500) {
                                    lastProgressTime = now
                                    val currentItem = initialItem.copy(
                                        downloadedBytes = downloadedBytes,
                                        totalBytes = if (totalBytes > 0) totalBytes else downloadedBytes,
                                        status = DownloadStatus.DOWNLOADING
                                    )
                                    activeItems[downloadId] = currentItem
                                    updateForegroundNotification(currentItem)
                                    repository.addOrUpdateDownload(currentItem)
                                    RealDownloadManager.dispatchDownloadProgress(currentItem)
                                }
                            }
                        }
                    }

                    if (isActive) {
                        val completedItem = initialItem.copy(
                            downloadedBytes = downloadedBytes,
                            totalBytes = if (totalBytes > 0) totalBytes else downloadedBytes,
                            status = DownloadStatus.COMPLETED
                        )
                        activeItems.remove(downloadId)
                        repository.addOrUpdateDownload(completedItem)
                        RealDownloadManager.dispatchDownloadProgress(completedItem)
                        RealDownloadManager.dispatchDownloadComplete(completedItem)

                        // Trigger media scanner so file appears in Android file manager
                        try {
                            android.media.MediaScannerConnection.scanFile(
                                applicationContext,
                                arrayOf(destinationFile.absolutePath),
                                arrayOf(mimeType),
                                null
                            )
                        } catch (_: Exception) {}

                        // Post Heads-up system notification on Android
                        val notifId = (downloadId.hashCode() and 0x7FFFFFFF)
                        DownloadNotificationHelper.showCompletionNotification(
                            context = applicationContext,
                            notificationId = notifId,
                            fileName = fileName,
                            file = destinationFile,
                            mimeType = mimeType,
                            totalBytes = if (totalBytes > 0) totalBytes else downloadedBytes
                        )
                    }
                } else {
                    handleDownloadError(downloadId, initialItem, "HTTP $responseCode")
                }
            } catch (e: Exception) {
                if (isActive) {
                    handleDownloadError(downloadId, initialItem, e.localizedMessage)
                }
            } finally {
                connection?.disconnect()
                activeJobs.remove(downloadId)
                checkRemainingAndStop()
            }
        }

        activeJobs[downloadId] = job
    }

    private suspend fun handleDownloadError(downloadId: String, item: DownloadItem, reason: String?) {
        activeItems.remove(downloadId)
        val failedItem = item.copy(status = DownloadStatus.FAILED)
        repository.addOrUpdateDownload(failedItem)
        RealDownloadManager.dispatchDownloadProgress(failedItem)

        val notifId = (downloadId.hashCode() and 0x7FFFFFFF)
        DownloadNotificationHelper.showFailureNotification(
            context = applicationContext,
            notificationId = notifId,
            fileName = item.fileName,
            reason = reason
        )
    }

    private fun promoteToForegroundIfNeeded(item: DownloadItem) {
        val notification = DownloadNotificationHelper.buildForegroundNotification(
            context = this,
            activeCount = activeItems.size.coerceAtLeast(1),
            latestFileName = item.fileName,
            downloadedBytes = item.downloadedBytes,
            totalBytes = item.totalBytes
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    DownloadNotificationHelper.FOREGROUND_SERVICE_NOTIFICATION_ID,
                    notification,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    } else {
                        0
                    }
                )
            } else {
                startForeground(DownloadNotificationHelper.FOREGROUND_SERVICE_NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
        }
    }

    private fun updateForegroundNotification(item: DownloadItem) {
        val notification = DownloadNotificationHelper.buildForegroundNotification(
            context = this,
            activeCount = activeItems.size.coerceAtLeast(1),
            latestFileName = item.fileName,
            downloadedBytes = item.downloadedBytes,
            totalBytes = item.totalBytes
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(DownloadNotificationHelper.FOREGROUND_SERVICE_NOTIFICATION_ID, notification)
    }

    private fun checkRemainingAndStop() {
        if (activeJobs.isEmpty() && activeItems.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_DETACH)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        activeItems.clear()
    }
}
