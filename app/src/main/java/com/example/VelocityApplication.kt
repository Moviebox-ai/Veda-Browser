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

        // Pre-warm Safe Browsing if supported
        try {
            WebView.startSafeBrowsing(this) { success ->
                AppLogger.d("VelocityApplication", "Safe Browsing initialized: $success")
            }
        } catch (e: Exception) {
            AppLogger.w("VelocityApplication", "Safe Browsing init skipped: ${e.message}")
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
