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
import kotlinx.coroutines.flow.MutableSharedFlow
import java.io.File

class RealDownloadManager(private val context: Context) {

    companion object {
        const val VEDA_DOWNLOADS_FOLDER = "Veda Browser"

        val liveProgressFlow = MutableSharedFlow<DownloadItem>(extraBufferCapacity = 64)
        val liveCompleteFlow = MutableSharedFlow<DownloadItem>(extraBufferCapacity = 16)

        fun dispatchDownloadProgress(item: DownloadItem) {
            liveProgressFlow.tryEmit(item)
        }

        fun dispatchDownloadComplete(item: DownloadItem) {
            liveCompleteFlow.tryEmit(item)
        }
    }

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        DownloadNotificationHelper.initChannels(context)
        // Automatically migrate any legacy files from Download/Veda Browser to Internal Storage/Veda Browser
        scope.launch {
            migrateLegacyDownloadsIfAny()
        }
    }

    /**
     * Resolves the dedicated "Veda Browser" folder directly in Internal Storage root.
     * Path: /storage/emulated/0/Veda Browser/
     */
    fun getInternalStorageVedaFolder(): File {
        val internalStorageRoot = Environment.getExternalStorageDirectory()
        val vedaFolder = File(internalStorageRoot, VEDA_DOWNLOADS_FOLDER)
        if (!vedaFolder.exists()) {
            try {
                vedaFolder.mkdirs()
            } catch (_: Exception) {
            }
        }
        if (vedaFolder.exists() && vedaFolder.canWrite()) {
            return vedaFolder
        }

        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val publicVedaFolder = File(publicDownloads, VEDA_DOWNLOADS_FOLDER)
        if (!publicVedaFolder.exists()) {
            try {
                publicVedaFolder.mkdirs()
            } catch (_: Exception) {
            }
        }
        if (publicVedaFolder.exists() && publicVedaFolder.canWrite()) {
            return publicVedaFolder
        }

        val externalFiles = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (externalFiles != null && externalFiles.canWrite()) {
            return externalFiles
        }

        val appInternalFolder = File(context.filesDir, VEDA_DOWNLOADS_FOLDER)
        if (!appInternalFolder.exists()) {
            try {
                appInternalFolder.mkdirs()
            } catch (_: Exception) {
            }
        }
        return appInternalFolder
    }

    private fun migrateLegacyDownloadsIfAny() {
        try {
            val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val legacyFolder = File(publicDownloads, VEDA_DOWNLOADS_FOLDER)
            if (legacyFolder.exists() && legacyFolder.isDirectory) {
                val targetDir = getInternalStorageVedaFolder()
                legacyFolder.listFiles()?.forEach { file ->
                    try {
                        val targetFile = File(targetDir, file.name)
                        if (!targetFile.exists()) {
                            file.copyTo(targetFile, overwrite = true)
                        }
                        file.delete()
                    } catch (_: Exception) {
                    }
                }
                if (legacyFolder.list().isNullOrEmpty()) {
                    legacyFolder.delete()
                }
            }
        } catch (_: Exception) {
        }
    }

    fun enqueueDownload(
        url: String,
        rawFileName: String?,
        mimeType: String?,
        userAgent: String?,
        onDownloadStarted: (DownloadItem) -> Unit,
        onProgressUpdate: (DownloadItem) -> Unit
    ): Long {
        val sanitizedName = FilenameSanitizer.sanitize(rawFileName)
        val resolvedMime = mimeType?.takeIf { it.isNotEmpty() && it != "application/octet-stream" }
            ?: guessMimeType(sanitizedName)

        val vedaFolder = getInternalStorageVedaFolder()
        val destinationFile = File(vedaFolder, sanitizedName)

        val downloadIdLong = System.currentTimeMillis()
        val downloadId = downloadIdLong.toString()

        val item = DownloadItem(
            id = downloadId,
            fileName = sanitizedName,
            url = url,
            mimeType = resolvedMime,
            status = DownloadStatus.DOWNLOADING,
            localFilePath = destinationFile.absolutePath,
            userAgent = userAgent
        )

        onDownloadStarted(item)

        // Delegate to Foreground Service for reliable background download and status bar notifications
        DownloadForegroundService.startDownload(
            context = context,
            downloadId = downloadId,
            url = url,
            fileName = sanitizedName,
            mimeType = resolvedMime,
            userAgent = userAgent,
            destinationPath = destinationFile.absolutePath
        )

        // Stream live progress updates to caller
        scope.launch {
            liveProgressFlow.collect { updatedItem ->
                if (updatedItem.id == downloadId) {
                    onProgressUpdate(updatedItem)
                }
            }
        }

        return downloadIdLong
    }

    fun pauseDownload(downloadId: String) {
        DownloadForegroundService.pauseDownload(context, downloadId)
    }

    fun resumeDownload(downloadId: String, forceRestart: Boolean = false) {
        DownloadForegroundService.resumeDownload(context, downloadId, forceRestart)
    }

    fun cancelDownload(downloadId: Long) {
        cancelDownload(downloadId.toString())
        try {
            downloadManager.remove(downloadId)
        } catch (_: Exception) {}
    }

    fun cancelDownload(downloadId: String) {
        DownloadForegroundService.cancelDownload(context, downloadId)
        try {
            downloadId.toLongOrNull()?.let { downloadManager.remove(it) }
        } catch (_: Exception) {}
    }

    fun getDownloadedFile(fileName: String): File? {
        // 1. Primary: Direct folder in Internal Storage root (/storage/emulated/0/Veda Browser/)
        val primaryFolder = getInternalStorageVedaFolder()
        val primaryFile = File(primaryFolder, fileName)
        if (primaryFile.exists()) return primaryFile

        val internalStorage = Environment.getExternalStorageDirectory()
        val internalFile = File(File(internalStorage, VEDA_DOWNLOADS_FOLDER), fileName)
        if (internalFile.exists()) return internalFile

        // 2. Internal app files: context.filesDir/Veda Browser/
        val appInternalFile = File(File(context.filesDir, VEDA_DOWNLOADS_FOLDER), fileName)
        if (appInternalFile.exists()) return appInternalFile

        // 3. Fallback: If still in Download/Veda Browser/, migrate it to Internal Storage
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val legacyVedaFile = File(File(publicDownloads, VEDA_DOWNLOADS_FOLDER), fileName)
        if (legacyVedaFile.exists()) {
            try {
                legacyVedaFile.copyTo(primaryFile, overwrite = true)
                legacyVedaFile.delete()
                return primaryFile
            } catch (_: Exception) {
                return legacyVedaFile
            }
        }

        // 4. Fallback: Download/
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
