package com.aaryo.velocity.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.ProfileStore
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.aaryo.velocity.model.BrowserSettings

const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36 Velocity/1.0"

class TabWebViewManager {

    private val webViewMap = mutableMapOf<String, WebView>()
    private val tabStateBundles = mutableMapOf<String, Bundle>()

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateWebView(
        context: Context,
        tabId: String,
        isIncognito: Boolean,
        settings: BrowserSettings,
        isDesktopMode: Boolean,
        gmBridge: com.aaryo.velocity.extension.GmBridge? = null
    ): WebView {
        val existing = webViewMap[tabId]
        if (existing != null) {
            applyWebSettings(existing, settings, isDesktopMode, isIncognito)
            gmBridge?.attachToWebView(existing)
            return existing
        }

        val webView = WebView(context.applicationContext).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        gmBridge?.attachToWebView(webView)

        // Configure multi-profile if supported by Android WebView engine
        if (isIncognito && WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
            try {
                ProfileStore.getInstance().getOrCreateProfile("incognito")
                androidx.webkit.WebViewCompat.setProfile(webView, "incognito")
            } catch (_: Exception) {
                // Fallback to standard cookie/cache isolation handled on cleanup
            }
        }

        applyWebSettings(webView, settings, isDesktopMode, isIncognito)

        // Restore state if previously saved
        tabStateBundles[tabId]?.let { bundle ->
            webView.restoreState(bundle)
        }

        webViewMap[tabId] = webView
        return webView
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun applyWebSettings(
        webView: WebView,
        settings: BrowserSettings,
        isDesktopMode: Boolean,
        isIncognito: Boolean
    ) {
        webView.settings.apply {
            javaScriptEnabled = settings.javaScriptEnabled
            textZoom = settings.textZoomPercent
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
            allowFileAccess = false
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

            userAgentString = if (isDesktopMode || settings.desktopSiteDefault) {
                DESKTOP_USER_AGENT
            } else {
                null
            }

            cacheMode = if (isIncognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
        }

        // Apply Algorithmic Darkening / Force Dark if available
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(
                webView.settings,
                settings.forceDarkWebContent
            )
        } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            @Suppress("DEPRECATION")
            WebSettingsCompat.setForceDark(
                webView.settings,
                if (settings.forceDarkWebContent) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
            )
        }

        // Cookie management
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(settings.cookiesEnabled)
        cookieManager.setAcceptThirdPartyCookies(webView, !isIncognito && settings.cookiesEnabled)
    }

    fun applySettingsToAll(settings: BrowserSettings) {
        for ((_, webView) in webViewMap) {
            webView.settings.javaScriptEnabled = settings.javaScriptEnabled
            webView.settings.textZoom = settings.textZoomPercent
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(
                    webView.settings,
                    settings.forceDarkWebContent
                )
            }
        }
        CookieManager.getInstance().setAcceptCookie(settings.cookiesEnabled)
    }

    fun captureThumbnail(tabId: String): Bitmap? {
        val webView = webViewMap[tabId] ?: return null
        val width = webView.width
        val height = webView.height
        if (width <= 0 || height <= 0) return null

        return try {
            val targetWidth = 360
            val targetHeight = (height.toFloat() / width.toFloat() * targetWidth).toInt().coerceAtLeast(200)
            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val scale = targetWidth.toFloat() / width.toFloat()
            val matrix = Matrix().apply { setScale(scale, scale) }
            canvas.setMatrix(matrix)

            webView.draw(canvas)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    fun saveTabState(tabId: String) {
        val webView = webViewMap[tabId] ?: return
        val bundle = Bundle()
        webView.saveState(bundle)
        tabStateBundles[tabId] = bundle
    }

    fun saveAllStates() {
        for (tabId in webViewMap.keys) {
            saveTabState(tabId)
        }
    }

    fun destroyWebView(tabId: String) {
        val webView = webViewMap.remove(tabId) ?: return
        tabStateBundles.remove(tabId)

        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.stopLoading()
        webView.loadUrl("about:blank")
        webView.clearHistory()
        webView.removeAllViews()
        webView.destroy()
    }

    fun destroyAll(onlyIncognitoTabIds: Set<String>? = null) {
        val idsToDestroy = onlyIncognitoTabIds ?: webViewMap.keys.toSet()
        for (id in idsToDestroy) {
            destroyWebView(id)
        }

        // If closing incognito tabs and MULTI_PROFILE is unsupported, clear residual storage
        if (onlyIncognitoTabIds != null && !WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
            clearIncognitoFallbackData()
        }
    }

    private fun clearIncognitoFallbackData() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
    }

    fun clearDataOnExit(clearCache: Boolean, clearCookies: Boolean) {
        for ((_, webView) in webViewMap) {
            if (clearCache) {
                webView.clearCache(true)
                webView.clearFormData()
            }
        }
        if (clearCookies) {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
    }
}
