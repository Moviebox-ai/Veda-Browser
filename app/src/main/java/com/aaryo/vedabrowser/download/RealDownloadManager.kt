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
        // Automatically migrate any files trapped in private/legacy directories to public Download/Veda Browser
        scope.launch {
            migrateDownloadsToPublicStorageIfAny()
        }
    }

    /**
     * Resolves the dedicated "Veda Browser" folder directly in Internal Storage root.
     * Path: /storage/emulated/0/Veda Browser/
     */
    fun getInternalStorageVedaFolder(): File {
        // 1. Primary: Direct Internal Storage Root (/storage/emulated/0/Veda Browser)
        val internalStorageRoot = Environment.getExternalStorageDirectory()
        val directVedaFolder = File(internalStorageRoot, VEDA_DOWNLOADS_FOLDER)
        if (!directVedaFolder.exists()) {
            try {
                directVedaFolder.mkdirs()
            } catch (_: Exception) {}
        }
        if (directVedaFolder.exists()) {
            return directVedaFolder
        }

        // 2. Secondary fallback if permission pending: Public Download/Veda Browser
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val publicVedaFolder = File(publicDownloads, VEDA_DOWNLOADS_FOLDER)
        if (!publicVedaFolder.exists()) {
            try {
                publicVedaFolder.mkdirs()
            } catch (_: Exception) {}
        }
        if (publicVedaFolder.exists()) {
            return publicVedaFolder
        }

        return directVedaFolder
    }

    fun triggerStorageMigration() {
        scope.launch {
            migrateDownloadsToPublicStorageIfAny()
        }
    }

    private fun migrateDownloadsToPublicStorageIfAny() {
        try {
            val targetDir = getInternalStorageVedaFolder()
            val pathsToScan = mutableListOf<String>()

            // 1. Migrate files trapped in context.getExternalFilesDir (Android/data/.../files/Download)
            val externalFiles = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (externalFiles != null && externalFiles.exists() && externalFiles.absolutePath != targetDir.absolutePath) {
                externalFiles.listFiles()?.forEach { file ->
                    if (file.isFile && file.length() > 0) {
                        try {
                            val targetFile = File(targetDir, file.name)
                            if (!targetFile.exists() || targetFile.length() < file.length()) {
                                file.copyTo(targetFile, overwrite = true)
                            }
                            file.delete()
                            pathsToScan.add(targetFile.absolutePath)
                        } catch (_: Exception) {}
                    }
                }
                val extSubVeda = File(externalFiles, VEDA_DOWNLOADS_FOLDER)
                if (extSubVeda.exists() && extSubVeda.isDirectory && extSubVeda.absolutePath != targetDir.absolutePath) {
                    extSubVeda.listFiles()?.forEach { file ->
                        if (file.isFile && file.length() > 0) {
                            try {
                                val targetFile = File(targetDir, file.name)
                                if (!targetFile.exists() || targetFile.length() < file.length()) {
                                    file.copyTo(targetFile, overwrite = true)
                                }
                                file.delete()
                                pathsToScan.add(targetFile.absolutePath)
                            } catch (_: Exception) {}
                        }
                    }
                    if (extSubVeda.list().isNullOrEmpty()) {
                        extSubVeda.delete()
                    }
                }
            }

            // 2. Migrate files trapped in context.filesDir/Veda Browser
            val appInternalFolder = File(context.filesDir, VEDA_DOWNLOADS_FOLDER)
            if (appInternalFolder.exists() && appInternalFolder.isDirectory && appInternalFolder.absolutePath != targetDir.absolutePath) {
                appInternalFolder.listFiles()?.forEach { file ->
                    if (file.isFile && file.length() > 0) {
                        try {
                            val targetFile = File(targetDir, file.name)
                            if (!targetFile.exists() || targetFile.length() < file.length()) {
                                file.copyTo(targetFile, overwrite = true)
                            }
                            file.delete()
                            pathsToScan.add(targetFile.absolutePath)
                        } catch (_: Exception) {}
                    }
                }
            }

            // 3. Migrate files from legacy /storage/emulated/0/Veda Browser/ if any
            val legacyRoot = File(Environment.getExternalStorageDirectory(), VEDA_DOWNLOADS_FOLDER)
            if (legacyRoot.exists() && legacyRoot.isDirectory && legacyRoot.absolutePath != targetDir.absolutePath) {
                legacyRoot.listFiles()?.forEach { file ->
                    if (file.isFile && file.length() > 0) {
                        try {
                            val targetFile = File(targetDir, file.name)
                            if (!targetFile.exists() || targetFile.length() < file.length()) {
                                file.copyTo(targetFile, overwrite = true)
                            }
                            pathsToScan.add(targetFile.absolutePath)
                        } catch (_: Exception) {}
                    }
                }
            }

            // 4. Scan all migrated files into Android MediaStore so they show immediately in File Manager, Gallery, etc.
            if (pathsToScan.isNotEmpty()) {
                android.media.MediaScannerConnection.scanFile(
                    context,
                    pathsToScan.toTypedArray(),
                    null,
                    null
                )
            }
        } catch (_: Exception) {}
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

    fun getDownloadedFile(fileName: String, knownPath: String? = null): File? {
        val targetFolder = getInternalStorageVedaFolder()

        // 1. Direct check of knownPath if present
        if (!knownPath.isNullOrBlank()) {
            val file = File(knownPath)
            if (file.exists() && file.length() > 0) {
                // If it's located in hidden app-specific storage, migrate it to the public folder
                if (file.parentFile != null && file.parentFile?.absolutePath != targetFolder.absolutePath) {
                    try {
                        val target = File(targetFolder, file.name)
                        if (!target.exists() || target.length() < file.length()) {
                            file.copyTo(target, overwrite = true)
                        }
                        file.delete()
                        android.media.MediaScannerConnection.scanFile(
                            context,
                            arrayOf(target.absolutePath),
                            null,
                            null
                        )
                        return target
                    } catch (_: Exception) {
                        return file
                    }
                }
                return file
            }
        }

        // 2. Public Download/Veda Browser/
        val primaryFile = File(targetFolder, fileName)
        if (primaryFile.exists() && primaryFile.length() > 0) return primaryFile

        // 3. Public Download/ root
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val downloadFile = File(publicDownloads, fileName)
        if (downloadFile.exists() && downloadFile.length() > 0) return downloadFile

        // 4. App external files (Android/data/com.aaryo.vedabrowser/files/Download)
        val externalFiles = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (externalFiles != null) {
            val extFile = File(externalFiles, fileName)
            if (extFile.exists() && extFile.length() > 0) return extFile
            val extSubFile = File(File(externalFiles, VEDA_DOWNLOADS_FOLDER), fileName)
            if (extSubFile.exists() && extSubFile.length() > 0) return extSubFile
        }

        // 5. Legacy /storage/emulated/0/Veda Browser/
        val internalStorage = Environment.getExternalStorageDirectory()
        val internalFile = File(File(internalStorage, VEDA_DOWNLOADS_FOLDER), fileName)
        if (internalFile.exists() && internalFile.length() > 0) return internalFile

        // 6. App internal files context.filesDir
        val appInternalFile = File(File(context.filesDir, VEDA_DOWNLOADS_FOLDER), fileName)
        if (appInternalFile.exists() && appInternalFile.length() > 0) return appInternalFile
        val rawAppFile = File(context.filesDir, fileName)
        if (rawAppFile.exists() && rawAppFile.length() > 0) return rawAppFile

        return null
    }

    private fun getUriForDownloadedItem(item: DownloadItem): Uri? {
        val file = getDownloadedFile(item.fileName, item.localFilePath)
        if (file != null && file.exists()) {
            return try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (_: Exception) {
                try {
                    Uri.fromFile(file)
                } catch (_: Exception) {
                    null
                }
            }
        }

        val downloadId = item.id.toLongOrNull()
        if (downloadId != null) {
            val dmUri = try {
                downloadManager.getUriForDownloadedFile(downloadId)
            } catch (_: Exception) {
                null
            }
            if (dmUri != null) return dmUri
        }

        return null
    }

    fun openDownloadedFile(item: DownloadItem) {
        val file = getDownloadedFile(item.fileName, item.localFilePath)
        val uri = getUriForDownloadedItem(item)

        if (uri == null || file == null || !file.exists()) {
            Toast.makeText(context, "Downloaded file not found on disk", Toast.LENGTH_SHORT).show()
            return
        }

        val mime = resolveEffectiveMime(file, item.mimeType)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (_: Exception) {
            try {
                // Fallback to wildcard */* if specific mime didn't match a default handler
                val genericIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "*/*")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(Intent.createChooser(genericIntent, "Open with"))
            } catch (_: Exception) {
                Toast.makeText(context, "No app available to open this file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareDownloadedFile(item: DownloadItem) {
        val file = getDownloadedFile(item.fileName, item.localFilePath)
        val uri = getUriForDownloadedItem(item)

        if (uri == null || file == null || !file.exists()) {
            Toast.makeText(context, "File not available for sharing", Toast.LENGTH_SHORT).show()
            return
        }

        val mime = resolveEffectiveMime(file, item.mimeType)
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

    fun openDownloadsFolderInFileManager() {
        try {
            val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val folder = getInternalStorageVedaFolder()
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", folder)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "resource/folder")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(context, "Saved in: Download/Veda Browser", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun resolveEffectiveMime(file: File, fallbackMime: String): String {
        if (fallbackMime.isNotBlank() && fallbackMime != "*/*" && fallbackMime != "application/octet-stream") {
            return fallbackMime
        }

        val extension = MimeTypeMap.getFileExtensionFromUrl(file.name).lowercase()
        val extensionMime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        if (!extensionMime.isNullOrBlank()) {
            return extensionMime
        }

        // Magic bytes inspection for common media and documents
        try {
            if (file.exists() && file.length() >= 8) {
                file.inputStream().use { input ->
                    val header = ByteArray(12)
                    val read = input.read(header)
                    if (read >= 4) {
                        // MP4: 'ftyp' at bytes 4..7
                        if (read >= 8 && header[4] == 'f'.code.toByte() && header[5] == 't'.code.toByte() &&
                            header[6] == 'y'.code.toByte() && header[7] == 'p'.code.toByte()
                        ) {
                            return "video/mp4"
                        }
                        // MKV / WebM: 1A 45 DF A3
                        if (header[0] == 0x1A.toByte() && header[1] == 0x45.toByte() &&
                            header[2] == 0xDF.toByte() && header[3] == 0xA3.toByte()
                        ) {
                            return "video/webm"
                        }
                        // PDF: %PDF
                        if (header[0] == '%'.code.toByte() && header[1] == 'P'.code.toByte() &&
                            header[2] == 'D'.code.toByte() && header[3] == 'F'.code.toByte()
                        ) {
                            return "application/pdf"
                        }
                        // JPEG: FF D8 FF
                        if (header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() && header[2] == 0xFF.toByte()) {
                            return "image/jpeg"
                        }
                        // PNG: 89 50 4E 47
                        if (header[0] == 0x89.toByte() && header[1] == 0x50.toByte() &&
                            header[2] == 0x4E.toByte() && header[3] == 0x47.toByte()
                        ) {
                            return "image/png"
                        }
                        // ZIP / APK: 50 4B 03 04
                        if (header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() &&
                            header[2] == 0x03.toByte() && header[3] == 0x04.toByte()
                        ) {
                            return if (file.name.endsWith(".apk", ignoreCase = true)) {
                                "application/vnd.android.package-archive"
                            } else {
                                "application/zip"
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return if (fallbackMime.isNotBlank()) fallbackMime else "*/*"
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
