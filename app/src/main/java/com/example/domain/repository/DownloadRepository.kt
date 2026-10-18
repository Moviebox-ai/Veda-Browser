package com.example.domain.repository

import android.content.Context
import com.example.domain.model.DownloadItem
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun getAllDownloads(): Flow<List<DownloadItem>>
    suspend fun enqueueDownload(
        context: Context,
        url: String,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long
    ): Long
    suspend fun pauseDownload(id: Long)
    suspend fun resumeDownload(context: Context, id: Long)
    suspend fun cancelDownload(id: Long)
    suspend fun deleteDownload(context: Context, item: DownloadItem)
    fun openFile(context: Context, item: DownloadItem): Boolean
}
