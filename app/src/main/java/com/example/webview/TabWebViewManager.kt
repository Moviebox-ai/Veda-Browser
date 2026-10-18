package com.example.webview

import android.content.Context
import android.content.MutableContextWrapper
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.ProfileStore
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.security.ContentBlocker
import com.example.util.AppLogger
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages WebViews per tab with hardened security configurations, profile isolation,
 * and memory-leak prevention using MutableContextWrapper.
 */
class TabWebViewManager(
    val contentBlocker: ContentBlocker = ContentBlocker()
) {

    private val webViews = ConcurrentHashMap<String, WebView>()
    private val defaultUserAgents = ConcurrentHashMap<String, String>()
    private val contextWrappers = ConcurrentHashMap<String, MutableContextWrapper>()

    companion object {
        const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
        const val PRIVATE_PROFILE_NAME = "velocity_private_profile"
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
            contextWrappers[tabId]?.baseContext = context
            existing.webViewClient = client
            existing.webChromeClient = chromeClient
            return existing
        }

        // Use MutableContextWrapper so Activity references can be detached to prevent memory leaks
        val wrapper = MutableContextWrapper(context)
        contextWrappers[tabId] = wrapper

        val webView = WebView(wrapper).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            webViewClient = client
            webChromeClient = chromeClient

            // Save default mobile user agent
            val defaultUa = settings.userAgentString
            defaultUserAgents[tabId] = defaultUa

            // Enable GPU Hardware Acceleration
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

            // Consume window insets
            ViewCompat.setOnApplyWindowInsetsListener(this) { _, _ ->
                WindowInsetsCompat.CONSUMED
            }

            // Multi-profile isolation for private tabs via AndroidX WebKit Profile API
            if (isPrivate && WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
                try {
                    ProfileStore.getInstance().getOrCreateProfile(PRIVATE_PROFILE_NAME)
                    WebViewCompat.setProfile(this, PRIVATE_PROFILE_NAME)
                } catch (e: Exception) {
                    AppLogger.w("TabWebViewManager", "Failed to assign multi-profile to private tab: ${e.message}")
                }
            } else {
                // High-throughput cookie handling for standard tabs
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, !isPrivate)
            }

            // Hardened Security & Performance Configurations
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadsImagesAutomatically = true

                // Autoplay blocked without user gesture
                mediaPlaybackRequiresUserGesture = true

                // Completely disable file:// and content:// access to prevent local exfiltration
                allowFileAccess = false
                allowContentAccess = false
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false

                // Block mixed content strictly
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                javaScriptCanOpenWindowsAutomatically = false

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
                // Hardened private mode settings
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
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
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

    fun detachAllContexts(appContext: Context) {
        for (wrapper in contextWrappers.values) {
            wrapper.baseContext = appContext
        }
    }

    fun destroyTab(tabId: String) {
        contextWrappers.remove(tabId)
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

    /**
     * Completely wipes private profile data when all private tabs are closed.
     */
    fun cleanupPrivateProfile() {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
            try {
                ProfileStore.getInstance().deleteProfile(PRIVATE_PROFILE_NAME)
                AppLogger.d("TabWebViewManager", "Private profile deleted successfully")
            } catch (e: Exception) {
                AppLogger.w("TabWebViewManager", "Could not delete private profile: ${e.message}")
            }
        } else {
            try {
                CookieManager.getInstance().removeSessionCookies(null)
            } catch (_: Exception) {}
        }
    }

    @Suppress("DEPRECATION")
    fun onTrimMemory() {
        for (webView in webViews.values) {
            webView.freeMemory()
        }
    }

    /**
     * Clears origin-scoped site data (localStorage, IndexedDB) without wiping other websites.
     */
    fun clearSiteData(origin: String) {
        try {
            WebStorage.getInstance().deleteOrigin(origin)
        } catch (e: Exception) {
            AppLogger.w("TabWebViewManager", "Could not delete origin storage for $origin: ${e.message}")
        }
    }

    /**
     * Global clear browsing data (Must be executed on the UI thread).
     */
    fun clearBrowsingData(clearCookies: Boolean, clearCache: Boolean, clearStorage: Boolean) {
        if (clearCookies) {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
        if (clearStorage) {
            WebStorage.getInstance().deleteAllData()
        }
        for (webView in webViews.values) {
            if (clearCache) {
                webView.clearCache(true)
            }
        }
    }
}
