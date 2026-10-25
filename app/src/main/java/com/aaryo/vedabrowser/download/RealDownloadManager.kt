package com.aaryo.vedabrowser.download

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
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

    companion object {
        const val VEDA_DOWNLOADS_FOLDER = "Veda Browser"
    }

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

        // Ensure the dedicated "Veda Browser" folder exists directly in internal storage root
        val internalStorageDir = Environment.getExternalStorageDirectory()
        val vedaFolder = File(internalStorageDir, VEDA_DOWNLOADS_FOLDER)
        if (!vedaFolder.exists()) {
            try {
                vedaFolder.mkdirs()
            } catch (_: Exception) {
            }
        }
        val destinationFile = File(vedaFolder, sanitizedName)

        val request = try {
            DownloadManager.Request(Uri.parse(url)).apply {
                setTitle(sanitizedName)
                setDescription("Downloading file to Veda Browser")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationUri(Uri.fromFile(destinationFile))

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
            // Fallback to direct streaming downloader into internal storage
            return downloadDirectly(
                url = url,
                destinationFile = destinationFile,
                sanitizedName = sanitizedName,
                mimeType = mimeType,
                userAgent = userAgent,
                onDownloadStarted = onDownloadStarted,
                onProgressUpdate = onProgressUpdate
            )
        }

        val downloadId = try {
            downloadManager.enqueue(request)
        } catch (_: Exception) {
            // If DownloadManager system service disallows custom URI on this Android API,
            // stream directly into internal storage / Veda Browser/
            return downloadDirectly(
                url = url,
                destinationFile = destinationFile,
                sanitizedName = sanitizedName,
                mimeType = mimeType,
                userAgent = userAgent,
                onDownloadStarted = onDownloadStarted,
                onProgressUpdate = onProgressUpdate
            )
        }

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

    private fun downloadDirectly(
        url: String,
        destinationFile: File,
        sanitizedName: String,
        mimeType: String?,
        userAgent: String?,
        onDownloadStarted: (DownloadItem) -> Unit,
        onProgressUpdate: (DownloadItem) -> Unit
    ): Long {
        val downloadId = System.currentTimeMillis()
        val item = DownloadItem(
            id = downloadId.toString(),
            fileName = sanitizedName,
            url = url,
            mimeType = mimeType ?: guessMimeType(sanitizedName),
            status = DownloadStatus.DOWNLOADING
        )
        onDownloadStarted(item)

        scope.launch {
            try {
                val connection = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
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
                    val totalBytes = connection.contentLengthLong
                    var bytesReadSoFar = 0L

                    connection.inputStream.use { input ->
                        java.io.FileOutputStream(destinationFile).use { output ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            var lastUpdate = System.currentTimeMillis()

                            while (input.read(buffer).also { read = it } != -1 && isActive) {
                                output.write(buffer, 0, read)
                                bytesReadSoFar += read
                                val now = System.currentTimeMillis()
                                if (now - lastUpdate > 500) {
                                    lastUpdate = now
                                    onProgressUpdate(
                                        item.copy(
                                            downloadedBytes = bytesReadSoFar,
                                            totalBytes = if (totalBytes > 0) totalBytes else bytesReadSoFar,
                                            status = DownloadStatus.DOWNLOADING
                                        )
                                    )
                                }
                            }
                        }
                    }

                    try {
                        android.media.MediaScannerConnection.scanFile(
                            context,
                            arrayOf(destinationFile.absolutePath),
                            arrayOf(mimeType ?: guessMimeType(sanitizedName)),
                            null
                        )
                    } catch (_: Exception) {}

                    onProgressUpdate(
                        item.copy(
                            downloadedBytes = bytesReadSoFar,
                            totalBytes = if (totalBytes > 0) totalBytes else bytesReadSoFar,
                            status = DownloadStatus.COMPLETED
                        )
                    )
                } else {
                    onProgressUpdate(item.copy(status = DownloadStatus.FAILED))
                }
            } catch (_: Exception) {
                onProgressUpdate(item.copy(status = DownloadStatus.FAILED))
            }
        }
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
                            try {
                                val destFile = File(File(Environment.getExternalStorageDirectory(), VEDA_DOWNLOADS_FOLDER), initialItem.fileName)
                                if (destFile.exists()) {
                                    android.media.MediaScannerConnection.scanFile(
                                        context,
                                        arrayOf(destFile.absolutePath),
                                        arrayOf(initialItem.mimeType),
                                        null
                                    )
                                }
                            } catch (_: Exception) {}
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

    fun getDownloadedFile(fileName: String): File? {
        // 1. Primary: /storage/emulated/0/Veda Browser/ (Internal Storage root)
        val internalStorage = Environment.getExternalStorageDirectory()
        val internalFile = File(File(internalStorage, VEDA_DOWNLOADS_FOLDER), fileName)
        if (internalFile.exists()) return internalFile

        // 2. Fallback: Download/Veda Browser/
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val vedaFile = File(File(publicDownloads, VEDA_DOWNLOADS_FOLDER), fileName)
        if (vedaFile.exists()) return vedaFile

        // 3. Fallback: Download/
        val legacyFile = File(publicDownloads, fileName)
        if (legacyFile.exists()) return legacyFile

        return null
    }

    private fun getUriForDownloadedItem(item: DownloadItem): Uri? {
        val downloadId = item.id.toLongOrNull()
        if (downloadId != null) {
            val dmUri = try {
                downloadManager.getUriForDownloadedFile(downloadId)
            } catch (_: Exception) {
                null
            }
            if (dmUri != null) return dmUri
        }

        val file = getDownloadedFile(item.fileName)
        if (file != null && file.exists()) {
            return try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (_: Exception) {
                Uri.fromFile(file)
            }
        }
        return null
    }

    fun openDownloadedFile(item: DownloadItem) {
        val uri = getUriForDownloadedItem(item)

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
        val uri = getUriForDownloadedItem(item)

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
