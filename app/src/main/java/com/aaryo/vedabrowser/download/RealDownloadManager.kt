package com.aaryo.vedabrowser.download

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.aaryo.vedabrowser.model.DownloadItem
import com.aaryo.vedabrowser.model.DownloadStatus
import com.aaryo.vedabrowser.util.FilenameSanitizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class RealDownloadManager(private val context: Context) {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val scope = CoroutineScope(Dispatchers.IO)

    fun enqueueDownload(
        url: String,
        rawFileName: String?,
        mimeType: String?,
        userAgent: String?,
        onDownloadStarted: (DownloadItem) -> Unit,
        onProgressUpdate: (DownloadItem) -> Unit
    ): Long {
        val sanitizedName = FilenameSanitizer.sanitize(rawFileName)

        val request = try {
            DownloadManager.Request(Uri.parse(url)).apply {
                setTitle(sanitizedName)
                setDescription("Downloading file via Velocity")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, sanitizedName)

                val cookie = CookieManager.getInstance().getCookie(url)
                if (!cookie.isNullOrEmpty()) {
                    addRequestHeader("Cookie", cookie)
                }
                if (!userAgent.isNullOrEmpty()) {
                    addRequestHeader("User-Agent", userAgent)
                }
                addRequestHeader("DNT", "1")

                if (!mimeType.isNullOrEmpty() && mimeType != "application/octet-stream") {
                    setMimeType(mimeType)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot download: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            return -1L
        }

        val downloadId = downloadManager.enqueue(request)

        val item = DownloadItem(
            id = downloadId.toString(),
            fileName = sanitizedName,
            url = url,
            mimeType = mimeType ?: guessMimeType(sanitizedName),
            status = DownloadStatus.DOWNLOADING
        )

        onDownloadStarted(item)
        trackProgress(downloadId, item, onProgressUpdate)
        return downloadId
    }

    private fun trackProgress(
        downloadId: Long,
        initialItem: DownloadItem,
        onProgressUpdate: (DownloadItem) -> Unit
    ) {
        scope.launch {
            var isFinished = false
            while (isActive && !isFinished) {
                delay(800)
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

                    val downloadedBytes = if (bytesDownloadedIndex >= 0) cursor.getLong(bytesDownloadedIndex) else 0L
                    val totalBytes = if (bytesTotalIndex >= 0) cursor.getLong(bytesTotalIndex) else 0L
                    val status = if (statusIndex >= 0) cursor.getInt(statusIndex) else -1

                    val itemStatus = when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            isFinished = true
                            DownloadStatus.COMPLETED
                        }
                        DownloadManager.STATUS_FAILED -> {
                            isFinished = true
                            DownloadStatus.FAILED
                        }
                        DownloadManager.STATUS_PAUSED -> DownloadStatus.DOWNLOADING
                        else -> DownloadStatus.DOWNLOADING
                    }

                    val updated = initialItem.copy(
                        downloadedBytes = downloadedBytes,
                        totalBytes = if (totalBytes > 0) totalBytes else initialItem.totalBytes,
                        status = itemStatus
                    )
                    onProgressUpdate(updated)
                    cursor.close()
                } else {
                    cursor?.close()
                    break
                }
            }
        }
    }

    fun cancelDownload(downloadId: Long) {
        downloadManager.remove(downloadId)
    }

    fun openDownloadedFile(item: DownloadItem) {
        val downloadId = item.id.toLongOrNull()
        val uri = if (downloadId != null) {
            downloadManager.getUriForDownloadedFile(downloadId)
        } else {
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), item.fileName)
            if (file.exists()) Uri.fromFile(file) else null
        }

        if (uri == null) {
            Toast.makeText(context, "Downloaded file not found on disk", Toast.LENGTH_SHORT).show()
            return
        }

        val mime = if (item.mimeType.isNotEmpty()) item.mimeType else guessMimeType(item.fileName)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (_: Exception) {
            Toast.makeText(context, "No app available to open this file", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareDownloadedFile(item: DownloadItem) {
        val downloadId = item.id.toLongOrNull()
        val uri = if (downloadId != null) {
            downloadManager.getUriForDownloadedFile(downloadId)
        } else {
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), item.fileName)
            if (file.exists()) Uri.fromFile(file) else null
        }

        if (uri == null) {
            Toast.makeText(context, "File not available for sharing", Toast.LENGTH_SHORT).show()
            return
        }

        val mime = if (item.mimeType.isNotEmpty()) item.mimeType else guessMimeType(item.fileName)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            type = mime
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(Intent.createChooser(shareIntent, "Share ${item.fileName}"))
        } catch (_: Exception) {
            Toast.makeText(context, "Cannot share file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guessMimeType(fileName: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(fileName)
        return if (!extension.isNullOrEmpty()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: "*/*"
        } else {
            "*/*"
        }
    }
}
