package com.example

import android.app.Application
import android.os.Build
import android.webkit.WebView
import com.example.data.local.AppDatabase
import com.example.data.local.preferences.UserPreferencesDataStore
import com.example.data.network.SearchSuggestionService
import com.example.data.repository.BrowserRepositoryImpl
import com.example.data.repository.DownloadRepositoryImpl
import com.example.domain.TabManager
import com.example.domain.repository.BrowserRepository
import com.example.domain.repository.DownloadRepository
import com.example.security.ContentBlocker
import com.example.util.AppLogger
import com.example.util.NetworkMonitor
import com.example.webview.TabWebViewManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class VelocityApplication : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var preferences: UserPreferencesDataStore
        private set
    lateinit var contentBlocker: ContentBlocker
        private set
    lateinit var webViewManager: TabWebViewManager
        private set
    lateinit var tabManager: TabManager
        private set
    lateinit var browserRepository: BrowserRepository
        private set
    lateinit var downloadRepository: DownloadRepository
        private set
    lateinit var suggestionService: SearchSuggestionService
        private set
    lateinit var networkMonitor: NetworkMonitor
        private set
    lateinit var speedTestManager: com.example.data.network.SpeedTestManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        AppLogger.i("VelocityApplication", "Initializing Velocity Browser core engine...")

        // Configure WebView data directory suffix for multi-process safety (Android 9+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val processName = getProcessName()
            if (packageName != processName) {
                WebView.setDataDirectorySuffix(processName)
            }
        }

        // Ensure Chromium Simple Cache and V8 GeneratedCodeCache directory structures exist.
        // Chromium's file enumerator requires 'Code Cache/js' and 'Code Cache/wasm' directories
        // under 'Default/HTTP Cache/Code Cache' and 'Default/Code Cache'. If 'wasm' does not exist,
        // opendir fails with ENOENT (2), causing 'Could not reconstruct index from disk' and
        // 'Failed to write a new fake index'.
        ensureChromiumCacheStructure()

        // Enable WebView debugging only for debug builds
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        // Initialize Local Storage & Preferences
        database = AppDatabase.getInstance(this)
        preferences = UserPreferencesDataStore(this)
        contentBlocker = ContentBlocker()
        webViewManager = TabWebViewManager(contentBlocker)
        tabManager = TabManager(webViewManager)

        browserRepository = BrowserRepositoryImpl(database, preferences)
        downloadRepository = DownloadRepositoryImpl(database)
        suggestionService = SearchSuggestionService()
        networkMonitor = NetworkMonitor(this)
        speedTestManager = com.example.data.network.SpeedTestManager()

        // Initialize Safe Browsing on the UI thread as strictly required by Android WebView API contract
        try {
            WebView.startSafeBrowsing(this) { success ->
                AppLogger.d("VelocityApplication", "Safe Browsing initialized: $success")
            }
        } catch (e: Exception) {
            AppLogger.w("VelocityApplication", "Safe Browsing init skipped: ${e.message}")
        }
    }

    private fun ensureChromiumCacheStructure() {
        try {
            val baseDirs = listOf(
                File(cacheDir, "WebView/Default"),
                File(cacheDir, "org.chromium.android_webview/Default")
            )

            for (base in baseDirs) {
                val httpCache = File(base, "HTTP Cache")
                val codeCache1 = File(httpCache, "Code Cache")
                val codeCache2 = File(base, "Code Cache")

                // Pre-create js and wasm code cache directories so opendir() in simple_file_enumerator.cc succeeds
                val requiredDirs = listOf(
                    File(codeCache1, "js"),
                    File(codeCache1, "wasm"),
                    File(codeCache2, "js"),
                    File(codeCache2, "wasm")
                )

                for (dir in requiredDirs) {
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                }

                // If zero-byte index files were left from interrupted writes, remove them
                // so Chromium's Simple Cache can re-write valid index files cleanly
                listOf(
                    File(httpCache, "index"),
                    File(codeCache1, "wasm/index"),
                    File(codeCache1, "js/index"),
                    File(codeCache2, "wasm/index"),
                    File(codeCache2, "js/index")
                ).forEach { sentinel ->
                    if (sentinel.exists() && sentinel.length() == 0L) {
                        sentinel.delete()
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.w("VelocityApplication", "Chromium cache structure prep note: ${e.message}")
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        webViewManager.onTrimMemory()
    }

    companion object {
        lateinit var instance: VelocityApplication
            private set
    }
}
