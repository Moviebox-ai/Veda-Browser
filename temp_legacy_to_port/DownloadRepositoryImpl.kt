package temp_legacy_to_port

import android.content.Context
import com.aaryo.velocity.model.DownloadItem
import com.aaryo.velocity.model.DownloadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface DownloadRepository {
    fun getAllDownloads(): Flow<List<DownloadItem>>
    suspend fun enqueue(fileName: String, url: String, mimeType: String, totalBytes: Long): DownloadItem
    suspend fun cancel(id: String)
    suspend fun delete(id: String)
}

class DownloadRepositoryImpl(
    private val context: Context
) : DownloadRepository {

    private val _downloadsFlow = MutableStateFlow<List<DownloadItem>>(emptyList())

    override fun getAllDownloads(): Flow<List<DownloadItem>> = _downloadsFlow.asStateFlow()

    override suspend fun enqueue(
        fileName: String,
        url: String,
        mimeType: String,
        totalBytes: Long
    ): DownloadItem {
        val sanitized = FilenameSanitizer.sanitize(fileName)
        val item = DownloadItem(
            fileName = sanitized,
            url = url,
            mimeType = mimeType,
            totalBytes = totalBytes,
            status = DownloadStatus.DOWNLOADING
        )
        _downloadsFlow.value = listOf(item) + _downloadsFlow.value
        return item
    }

    override suspend fun cancel(id: String) {
        _downloadsFlow.value = _downloadsFlow.value.map {
            if (it.id == id) it.copy(status = DownloadStatus.CANCELLED) else it
        }
    }

    override suspend fun delete(id: String) {
        _downloadsFlow.value = _downloadsFlow.value.filterNot { it.id == id }
    }
}
