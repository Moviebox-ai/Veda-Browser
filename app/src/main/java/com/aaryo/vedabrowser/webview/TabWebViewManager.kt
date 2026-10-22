package com.aaryo.vedabrowser.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.ProfileStore
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.aaryo.vedabrowser.model.BrowserSettings
import com.aaryo.vedabrowser.privacy.AdBlocker

const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"

const val MOBILE_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"

class TabWebViewManager {

    private val webViewMap = mutableMapOf<String, WebView>()
    private val tabStateBundles = mutableMapOf<String, Bundle>()

    init {
        setupServiceWorkerAdBlocker()
    }

    private fun setupServiceWorkerAdBlocker() {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE)) {
            try {
                val controller = androidx.webkit.ServiceWorkerControllerCompat.getInstance()
                controller.setServiceWorkerClient(object : androidx.webkit.ServiceWorkerClientCompat() {
                    override fun shouldInterceptRequest(request: android.webkit.WebResourceRequest): android.webkit.WebResourceResponse? {
                        val url = request.url?.toString() ?: return null
                        val headers = request.requestHeaders
                        val refererHost = headers?.let { h ->
                            (h["Referer"] ?: h["referer"])?.let { ref ->
                                try { Uri.parse(ref).host } catch (_: Exception) { null }
                            }
                        } ?: if (request.isForMainFrame) request.url?.host else null

                        val adBlocker = AdBlocker
                        if (!adBlocker.globalEnabled) return null
                        val resType = adBlocker.detectResourceType(url, headers, request.isForMainFrame)
                        if (adBlocker.isAdOrTracker(url, refererHost, resType, headers)) {
                            return adBlocker.createEmptyResponse(url, resType)
                        }
                        return null
                    }
                })
            } catch (_: Throwable) {
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateWebView(
        context: Context,
        tabId: String,
        isIncognito: Boolean,
        settings: BrowserSettings,
        isDesktopMode: Boolean,
        gmBridge: com.aaryo.vedabrowser.extension.GmBridge? = null
    ): WebView {
        AdBlocker.setGlobalEnabled(settings.adBlockGlobalEnabled)
        val existing = webViewMap[tabId]
        if (existing != null) {
            applyWebSettings(existing, settings, isDesktopMode, isIncognito)
            gmBridge?.attachToWebView(existing)
            return existing
        }

        com.aaryo.vedabrowser.VedaApplication.cleanCorruptedVariationsSeeds(context)

        val webView = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            webViewClient = object : android.webkit.WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: android.webkit.WebResourceRequest?
                ): android.webkit.WebResourceResponse? {
                    val reqUrl = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)
                    val headers = request.requestHeaders
                    val siteHost = view?.url?.let {
                        try { Uri.parse(it).host } catch (_: Exception) { null }
                    } ?: headers?.let { h ->
                        (h["Referer"] ?: h["referer"])?.let { ref ->
                            try { Uri.parse(ref).host } catch (_: Exception) { null }
                        }
                    } ?: if (request.isForMainFrame) request.url?.host else null

                    val resType = AdBlocker.detectResourceType(reqUrl, headers, request.isForMainFrame)
                    if (settings.adBlockGlobalEnabled && AdBlocker.isAdOrTracker(reqUrl, siteHost, resType, headers)) {
                        return AdBlocker.createEmptyResponse(reqUrl, resType)
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                @Deprecated("Deprecated in Java")
                override fun shouldInterceptRequest(view: WebView?, url: String?): android.webkit.WebResourceResponse? {
                    if (url != null && settings.adBlockGlobalEnabled) {
                        val siteHost = view?.url?.let { try { Uri.parse(it).host } catch (_: Exception) { null } }
                        val resType = AdBlocker.detectResourceType(url)
                        if (AdBlocker.isAdOrTracker(url, siteHost, resType)) {
                            return AdBlocker.createEmptyResponse(url, resType)
                        }
                    }
                    return super.shouldInterceptRequest(view, url)
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                    val url = request?.url?.toString() ?: return false
                    return handleBaselineUrlLoading(view, url)
                }

                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    if (url == null) return false
                    return handleBaselineUrlLoading(view, url)
                }

                private fun handleBaselineUrlLoading(view: WebView?, url: String): Boolean {
                    val siteHost = view?.url?.let { try { Uri.parse(it).host } catch (_: Exception) { null } }
                    // Block ad or tracker redirect
                    if (settings.adBlockGlobalEnabled && AdBlocker.isAdOrTracker(url, siteHost)) {
                        return true
                    }
                    // Extract embedded web URLs so searches and queries stay inside Velocity
                    val webUrl = com.aaryo.vedabrowser.security.IntentValidator.extractWebUrlFromIntent(url)
                    if (!webUrl.isNullOrBlank()) {
                        view?.loadUrl(webUrl)
                        return true
                    }
                    // Block escaping to external browser
                    if (com.aaryo.vedabrowser.security.IntentValidator.isBrowserIntent(url) ||
                        com.aaryo.vedabrowser.security.SchemePolicy.isBrowserCustomScheme(url)
                    ) {
                        return true
                    }
                    // Keep web navigation in-app
                    if (com.aaryo.vedabrowser.security.SchemePolicy.isWebScheme(url)) {
                        return false
                    }
                    // Non-browser external schemes
                    if (com.aaryo.vedabrowser.security.SchemePolicy.isExternalScheme(url) || url.startsWith("intent:", ignoreCase = true)) {
                        val safeIntent = com.aaryo.vedabrowser.security.IntentValidator.parseSafeIntent(context, url)
                        if (safeIntent != null) {
                            try { context.startActivity(safeIntent) } catch (_: Exception) {}
                        }
                        return true
                    }
                    return true
                }
            }
        }
        gmBridge?.attachToWebView(webView)

        if (isIncognito && WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
            try {
                ProfileStore.getInstance().getOrCreateProfile("incognito")
                androidx.webkit.WebViewCompat.setProfile(webView, "incognito")
            } catch (_: Throwable) {
            }
        }

        // Inject cosmetic ad block at document start
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            try {
                androidx.webkit.WebViewCompat.addDocumentStartJavaScript(
                    webView,
                    AdBlocker.getCosmeticDocumentStartScript(),
                    setOf("*")
                )
            } catch (_: Throwable) {
            }
        }

        applyWebSettings(webView, settings, isDesktopMode, isIncognito)

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

            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(true)

            userAgentString = if (isDesktopMode || settings.desktopSiteDefault) {
                DESKTOP_USER_AGENT
            } else {
                MOBILE_USER_AGENT
            }

            cacheMode = if (isIncognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
        }

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

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(settings.cookiesEnabled)
        cookieManager.setAcceptThirdPartyCookies(webView, !isIncognito && settings.cookiesEnabled)
    }

    fun applySettingsToAll(settings: BrowserSettings) {
        AdBlocker.setGlobalEnabled(settings.adBlockGlobalEnabled)
        AdBlocker.isStrictMode = settings.adBlockStrictMode
        AdBlocker.syncAllowlist(settings.allowlistedDomains)

        if (webViewMap.isEmpty()) return
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
        try {
            CookieManager.getInstance().setAcceptCookie(settings.cookiesEnabled)
        } catch (_: Throwable) {
        }
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
