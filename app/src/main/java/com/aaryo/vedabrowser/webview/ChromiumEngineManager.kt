package com.aaryo.vedabrowser.webview

import android.content.Context
import android.os.Build
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/**
 * Diagnostics and configuration manager for the underlying Google Chromium (Blink + V8)
 * native engine on Android.
 */
data class ChromiumEngineDetails(
    val packageName: String,
    val versionName: String,
    val isMultiProcessSupported: Boolean,
    val isSafeBrowsingSupported: Boolean,
    val isAlgorithmicDarkeningSupported: Boolean,
    val isMultiProfileSupported: Boolean,
    val isDocumentStartScriptSupported: Boolean,
    val isServiceWorkerSupported: Boolean
)

object ChromiumEngineManager {

    private var isSafeBrowsingInitialized = false

    /**
     * Bootstraps the Chromium engine at Application startup.
     */
    fun initializeEngine(context: Context) {
        // Enable Chromium Remote WebContents DevTools debugging (chrome://inspect)
        try {
            WebView.setWebContentsDebuggingEnabled(true)
        } catch (_: Throwable) {
        }

        // Initialize Chromium Safe Browsing API
        if (WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING)) {
            try {
                WebViewCompat.startSafeBrowsing(context.applicationContext) { success ->
                    isSafeBrowsingInitialized = success
                }
            } catch (_: Throwable) {
            }
        }
    }

    /**
     * Inspects the active Chromium WebView provider installed on the system.
     */
    fun getEngineDetails(context: Context): ChromiumEngineDetails {
        val packageInfo = try {
            WebViewCompat.getCurrentWebViewPackage(context)
        } catch (_: Throwable) {
            null
        }

        val pkgName = packageInfo?.packageName ?: "com.google.android.webview"
        val verName = packageInfo?.versionName ?: "Chromium Engine (Blink/V8)"

        val multiProcess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROCESS)
        } else false

        return ChromiumEngineDetails(
            packageName = pkgName,
            versionName = verName,
            isMultiProcessSupported = multiProcess,
            isSafeBrowsingSupported = WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING),
            isAlgorithmicDarkeningSupported = WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING),
            isMultiProfileSupported = WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE),
            isDocumentStartScriptSupported = WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT),
            isServiceWorkerSupported = WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE)
        )
    }

    /**
     * Optimizes a WebView instance with full Chromium hardware acceleration,
     * HTML5 storage, V8 execution, and modern Chromium WebSettings.
     */
    fun optimizeChromiumSettings(
        webView: WebView,
        enableHardwareAcceleration: Boolean = true,
        enableSafeBrowsing: Boolean = true
    ) {
        // Enable GPU Hardware Acceleration for Chromium compositor
        if (enableHardwareAcceleration) {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }

        // Safe Browsing per-view control if supported
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            try {
                androidx.webkit.WebSettingsCompat.setSafeBrowsingEnabled(
                    webView.settings,
                    enableSafeBrowsing
                )
            } catch (_: Throwable) {
            }
        }

        webView.settings.apply {
            // Full HTML5 Storage & V8 Web SQL/IndexedDB support
            domStorageEnabled = true
            databaseEnabled = true

            // HTML5 Geolocation API support
            setGeolocationEnabled(true)

            // Allow HTML5 video/audio playback without requiring user gestures
            mediaPlaybackRequiresUserGesture = false

            // Standard high-performance Chromium viewport & zoom settings
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false

            // Security defaults: disallow raw file:// schema access
            allowFileAccess = false
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

            // Enable multi-window / popup handling
            setSupportMultipleWindows(true)
        }
    }
}
