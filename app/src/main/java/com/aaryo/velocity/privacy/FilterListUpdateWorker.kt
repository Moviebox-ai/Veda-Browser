package com.aaryo.velocity.privacy

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aaryo.velocity.data.BrowserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class FilterListUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "VelocityFilterListUpdateWork"

        // Production mirror URLs for live updates
        val FILTER_URLS = mapOf(
            "easylist.txt" to "https://raw.githubusercontent.com/easylist/easylist/master/easylist/easylist_general_block.txt",
            "easyprivacy.txt" to "https://raw.githubusercontent.com/easylist/easylist/master/easyprivacy/easyprivacy_general.txt",
            "ublock_filters.txt" to "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt",
            "indian_filters.txt" to "https://raw.githubusercontent.com/AdguardTeam/FiltersRegistry/master/filters/filter_20_Indian/filter.txt"
        )

        fun scheduleWeeklyUpdate(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<FilterListUpdateWorker>(7, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val targetDir = File(applicationContext.filesDir, "filter_lists")
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        var anyUpdated = false

        for ((filename, url) in FILTER_URLS) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "VelocityBrowser/1.0 AdBlocker")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrBlank() && bodyString.length > 500) {
                        val tempFile = File(targetDir, "$filename.tmp")
                        tempFile.writeText(bodyString, Charsets.UTF_8)
                        val finalFile = File(targetDir, filename)
                        if (tempFile.renameTo(finalFile) || (finalFile.delete() && tempFile.renameTo(finalFile))) {
                            anyUpdated = true
                        }
                    }
                }
                response.close()
            } catch (_: Exception) {
                // Graceful fallback: maintain existing local files or bundled assets
            }
        }

        if (anyUpdated) {
            val now = System.currentTimeMillis()
            val repository = BrowserRepository(applicationContext)
            val currentSettings = repository.settingsFlow.first()
            repository.saveSettings(currentSettings.copy(filterListsLastUpdated = now))
            AdBlocker.loadRules(applicationContext, currentSettings, forceReload = true)
        }

        Result.success()
    }
}
