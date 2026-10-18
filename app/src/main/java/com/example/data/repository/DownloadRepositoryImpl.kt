package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.URLUtil
import androidx.core.content.FileProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.DownloadEntity
import com.example.domain.model.DownloadItem
import com.example.domain.model.DownloadStatus
import com.example.domain.repository.DownloadRepository
import com.example.VelocityApplication
import com.example.util.AppLogger
import com.example.util.DownloadNotificationHelper
import com.example.util.FilenameSanitizer
import com.example.util.StorageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * High-speed turbo download engine with multi-connection pooling, 128KB streaming buffers,
 * HTTP Range-based segmented pause/resume, real-time MB/s telemetry, and system notifications.
 */
class DownloadRepositoryImpl(
    private val database: AppDatabase,
    private val context: Context? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(32, 5, TimeUnit.MINUTES))
        .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
) : DownloadRepository {

    private val appContext: Context?
        get() = context ?: runCatching { VelocityApplication.instance }.getOrNull()

    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val liveSpeeds = ConcurrentHashMap<Long, Long>()

    // High throughput 128KB buffer for max bandwidth saturation
    private val BUFFER_SIZE = 131072 // 128 KB

    override fun getAllDownloads(): Flow<List<DownloadItem>> =
        database.downloadDao().getAllDownloads().map { list ->
            list.map {
                val currentSpeed = liveSpeeds[it.id] ?: 0L
                DownloadItem(
                    id = it.id,
                    url = it.url,
                    filename = it.filename,
                    filePath = it.filePath,
                    mimeType = it.mimeType,
                    totalBytes = it.totalBytes,
                    downloadedBytes = it.downloadedBytes,
                    status = DownloadStatus.fromString(it.status),
                    createdAt = it.createdAt,
                    completedAt = it.completedAt,
                    errorMessage = it.errorMessage,
                    speedBytesPerSec = currentSpeed
                )
            }
        }

    override suspend fun enqueueDownload(
        context: Context,
        url: String,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long
    ): Long {
        val suggestedName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        val sanitized = FilenameSanitizer.sanitize(suggestedName, mimeType)

        val downloadsDir = StorageUtils.getAppDownloadDirectory(context)
        val targetFile = FilenameSanitizer.getUniqueFile(downloadsDir, sanitized)

        val entity = DownloadEntity(
            url = url,
            filename = targetFile.name,
            filePath = targetFile.absolutePath,
            mimeType = mimeType ?: "application/octet-stream",
            totalBytes = contentLength,
            downloadedBytes = 0,
            status = DownloadStatus.QUEUED.name
        )

        val id = database.downloadDao().insert(entity)

        val job = scope.launch {
            executeDownload(id, url, targetFile, contentLength, resume = false)
        }
        activeJobs[id] = job

        return id
    }

    override suspend fun pauseDownload(id: Long) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        liveSpeeds.remove(id)
        appContext?.let { DownloadNotificationHelper.cancel(it, id) }
        val current = database.downloadDao().getDownloadById(id)
        if (current != null && current.status == DownloadStatus.RUNNING.name) {
            database.downloadDao().updateProgress(
                id = id,
                status = DownloadStatus.PAUSED.name,
                downloadedBytes = current.downloadedBytes,
                totalBytes = current.totalBytes
            )
        }
    }

    override suspend fun resumeDownload(context: Context, id: Long) {
        val entity = database.downloadDao().getDownloadById(id) ?: return
        val targetFile = File(entity.filePath)

        val job = scope.launch {
            executeDownload(id, entity.url, targetFile, entity.totalBytes, resume = true)
        }
        activeJobs[id] = job
    }

    private suspend fun executeDownload(
        id: Long,
        url: String,
        targetFile: File,
        expectedLength: Long,
        resume: Boolean
    ) {
        try {
            val existingBytes = if (resume && targetFile.exists()) targetFile.length() else 0L

            database.downloadDao().updateProgress(id, DownloadStatus.RUNNING.name, existingBytes, expectedLength)
            appContext?.let { DownloadNotificationHelper.notifyProgress(it, id, targetFile.name, existingBytes, expectedLength, 0L) }

            val requestBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/128.0.0.0 Mobile Safari/537.36")
                .header("Accept-Encoding", "identity") // Avoid gzip for accurate byte streaming

            if (existingBytes > 0) {
                requestBuilder.header("Range", "bytes=$existingBytes-")
            }

            val request = requestBuilder.build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 206) {
                    val errMsg = "HTTP error: ${response.code}"
                    database.downloadDao().markFailed(id, errMsg)
                    appContext?.let { DownloadNotificationHelper.notifyFailed(it, id, targetFile.name, errMsg) }
                    liveSpeeds.remove(id)
                    return
                }

                val body = response.body ?: run {
                    database.downloadDao().markFailed(id, "Empty response body")
                    appContext?.let { DownloadNotificationHelper.notifyFailed(it, id, targetFile.name, "Empty response body") }
                    liveSpeeds.remove(id)
                    return
                }

                val append = response.code == 206 && existingBytes > 0
                val streamContentLength = body.contentLength()
                val total = if (append) {
                    existingBytes + (if (streamContentLength > 0) streamContentLength else 0L)
                } else {
                    if (streamContentLength > 0) streamContentLength else expectedLength
                }

                var downloaded = if (append) existingBytes else 0L

                BufferedInputStream(body.byteStream(), BUFFER_SIZE).use { input ->
                    BufferedOutputStream(FileOutputStream(targetFile, append), BUFFER_SIZE).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int
                        var lastUpdateTime = System.currentTimeMillis()
                        var bytesSinceLastUpdate = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            bytesSinceLastUpdate += bytesRead

                            val now = System.currentTimeMillis()
                            val elapsed = now - lastUpdateTime
                            if (elapsed >= 350) {
                                val currentSpeedBytesPerSec = if (elapsed > 0) (bytesSinceLastUpdate * 1000) / elapsed else 0L
                                liveSpeeds[id] = currentSpeedBytesPerSec
                                database.downloadDao().updateProgress(id, DownloadStatus.RUNNING.name, downloaded, total)
                                appContext?.let { DownloadNotificationHelper.notifyProgress(it, id, targetFile.name, downloaded, total, currentSpeedBytesPerSec) }
                                lastUpdateTime = now
                                bytesSinceLastUpdate = 0L
                            }
                        }
                        output.flush()
                    }
                }

                liveSpeeds.remove(id)
                database.downloadDao().updateProgress(id, DownloadStatus.COMPLETED.name, downloaded, total)
                database.downloadDao().markCompleted(id)
                val entity = database.downloadDao().getDownloadById(id)
                val mimeType = entity?.mimeType ?: "application/octet-stream"
                appContext?.let { ctx ->
                    StorageUtils.scanMediaFile(ctx, targetFile, mimeType)
                    DownloadNotificationHelper.notifyCompleted(ctx, id, targetFile.name, targetFile, mimeType)
                }
            }
        } catch (e: Exception) {
            liveSpeeds.remove(id)
            AppLogger.e("DownloadRepository", "Download failed for ID $id", e)
            val entity = database.downloadDao().getDownloadById(id)
            if (entity?.status != DownloadStatus.PAUSED.name && entity?.status != DownloadStatus.CANCELED.name) {
                val errorMsg = e.message ?: "Download failed"
                database.downloadDao().markFailed(id, errorMsg)
                appContext?.let { DownloadNotificationHelper.notifyFailed(it, id, targetFile.name, errorMsg) }
            } else {
                appContext?.let { DownloadNotificationHelper.cancel(it, id) }
            }
        } finally {
            activeJobs.remove(id)
            liveSpeeds.remove(id)
        }
    }

    override suspend fun cancelDownload(id: Long) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        liveSpeeds.remove(id)
        appContext?.let { DownloadNotificationHelper.cancel(it, id) }
        database.downloadDao().updateProgress(id, DownloadStatus.CANCELED.name, 0, 0)
    }

    override suspend fun deleteDownload(context: Context, item: DownloadItem) {
        cancelDownload(item.id)
        val file = File(item.filePath)
        if (file.exists()) {
            file.delete()
        }
        database.downloadDao().deleteById(item.id)
    }

    override fun openFile(context: Context, item: DownloadItem): Boolean {
        val file = File(item.filePath)
        if (!file.exists()) return false

        return try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, item.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            AppLogger.e("DownloadRepository", "Failed to open file: ${item.filePath}", e)
            false
        }
    }
}
