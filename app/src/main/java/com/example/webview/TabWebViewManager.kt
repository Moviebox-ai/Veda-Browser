package com.example.webview

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.example.security.ContentBlocker
import com.example.util.AppLogger
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages WebViews per tab with hardened security configurations and lifecycle safety.
 */
class TabWebViewManager(
    val contentBlocker: ContentBlocker = ContentBlocker()
) {

    private val webViews = ConcurrentHashMap<String, WebView>()
    private val defaultUserAgents = ConcurrentHashMap<String, String>()

    companion object {
        const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
    }

    @Suppress("DEPRECATION")
    fun getOrCreateWebView(
        tabId: String,
        context: Context,
        isPrivate: Boolean,
        client: VelocityWebViewClient,
        chromeClient: VelocityChromeClient
    ): WebView {
        val existing = webViews[tabId]
        if (existing != null) {
            existing.webViewClient = client
            existing.webChromeClient = chromeClient
            return existing
        }

        val webView = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            webViewClient = client
            webChromeClient = chromeClient

            // Save default mobile user agent
            val defaultUa = settings.userAgentString
            defaultUserAgents[tabId] = defaultUa

            // Enable GPU Hardware Acceleration for ultra-fast rendering & video
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

            // Consume insets so Chromium's internal compositor does not trigger conflicting layout passes during IME animation
            ViewCompat.setOnApplyWindowInsetsListener(this) { _, _ ->
                WindowInsetsCompat.CONSUMED
            }

            // High-throughput cookie handling for reliable uploads and authenticated downloads
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, !isPrivate)

            // Hardened Security & High-Performance Configurations
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                loadsImagesAutomatically = true
                mediaPlaybackRequiresUserGesture = false

                // Keep file:// access blocked for security, but allow content:// for high-speed file uploads
                allowFileAccess = false
                allowContentAccess = true
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false

                // Enforce strict HTTPS / Block mixed content
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

                // Performance & Cache
                cacheMode = if (isPrivate) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT

                // Zoom & Viewport
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                useWideViewPort = true
                loadWithOverviewMode = true

                // Multiple windows
                setSupportMultipleWindows(false)

                // Safe Browsing
                if (WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING)) {
                    safeBrowsingEnabled = true
                }

                // Dark mode support
                if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(this, true)
                }
            }

            if (isPrivate) {
                // Disable form data for private tabs (cache isolation handled via LOAD_NO_CACHE)
                settings.saveFormData = false
            }
        }

        webViews[tabId] = webView
        return webView
    }

    fun getWebView(tabId: String): WebView? = webViews[tabId]

    fun saveTabState(tabId: String): Bundle? {
        val webView = webViews[tabId] ?: return null
        val bundle = Bundle()
        webView.saveState(bundle)
        return bundle
    }

    fun restoreTabState(tabId: String, bundle: Bundle?): Boolean {
        if (bundle == null) return false
        val webView = webViews[tabId] ?: return false
        return webView.restoreState(bundle) != null
    }

    fun setDesktopMode(tabId: String, enabled: Boolean) {
        val webView = webViews[tabId] ?: return
        val defaultUa = defaultUserAgents[tabId] ?: webView.settings.userAgentString
        webView.settings.userAgentString = if (enabled) DESKTOP_USER_AGENT else defaultUa
        webView.settings.useWideViewPort = enabled
        webView.settings.loadWithOverviewMode = enabled
        webView.reload()
    }

    fun setTextZoom(tabId: String, zoomPercent: Int) {
        val webView = webViews[tabId] ?: return
        webView.settings.textZoom = zoomPercent.coerceIn(50, 200)
    }

    fun setJavaScriptEnabled(tabId: String, enabled: Boolean) {
        val webView = webViews[tabId] ?: return
        webView.settings.javaScriptEnabled = enabled
    }

    fun destroyTab(tabId: String) {
        val webView = webViews.remove(tabId) ?: return
        defaultUserAgents.remove(tabId)
        try {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.stopLoading()
            webView.webViewClient = WebViewClient()
            webView.webChromeClient = null
            webView.removeAllViews()
            webView.destroy()
        } catch (e: Exception) {
            AppLogger.w("TabWebViewManager", "Error destroying webview for tab $tabId: ${e.message}")
        }
    }

    fun destroyAll() {
        for (tabId in webViews.keys) {
            destroyTab(tabId)
        }
    }

    @Suppress("DEPRECATION")
    fun onTrimMemory() {
        // Clear caches to free memory when Android system requests
        for (webView in webViews.values) {
            webView.freeMemory()
        }
    }

    fun clearBrowsingData(clearCookies: Boolean, clearCache: Boolean, clearStorage: Boolean) {
        if (clearCookies) {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
        if (clearCache) {
            for (webView in webViews.values) {
                webView.clearCache(true)
            }
        }
        if (clearStorage) {
            WebStorage.getInstance().deleteAllData()
        }
    }
}
