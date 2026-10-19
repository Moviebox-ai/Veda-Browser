package com.example.velocity.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.velocity.model.Tab
import com.example.velocity.privacy.AdBlocker

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36 Velocity/1.0"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(
    tab: Tab,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String, String?) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onTitleReceived: (String) -> Unit,
    onFaviconReceived: (Bitmap) -> Unit,
    onNavigationStateChanged: (Boolean, Boolean) -> Unit,
    onTrackerBlocked: () -> Unit,
    onDownloadStart: (String, String, String, Long) -> Unit,
    findInPageQuery: String,
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit = {}
) {
    val context = LocalContext.current
    val webView = remember(tab.id) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = true
                displayZoomControls = false
                allowFileAccess = false
                allowContentAccess = true
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                if (tab.isIncognito) {
                    cacheMode = WebSettings.LOAD_NO_CACHE
                } else {
                    cacheMode = WebSettings.LOAD_DEFAULT
                }
            }

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, !tab.isIncognito)

            setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
                onDownloadStart(url, fileName, mimetype ?: "application/octet-stream", contentLength)
            }
        }
    }

    // Configure user agent for Desktop mode
    LaunchedEffect(tab.isDesktopMode) {
        if (tab.isDesktopMode) {
            webView.settings.userAgentString = DESKTOP_USER_AGENT
        } else {
            webView.settings.userAgentString = null // Reset to default mobile user-agent
        }
        if (!tab.isNewTab) {
            webView.reload()
        }
    }

    // Handle Find in Page
    LaunchedEffect(findInPageQuery) {
        if (findInPageQuery.isNotEmpty()) {
            webView.findAllAsync(findInPageQuery)
        } else {
            webView.clearMatches()
        }
    }

    // Load URL when tab url changes
    LaunchedEffect(tab.url) {
        if (!tab.isNewTab && tab.url.isNotEmpty() && webView.url != tab.url) {
            val headers = if (tab.adBlockEnabled) mapOf("DNT" to "1") else emptyMap()
            webView.loadUrl(tab.url, headers)
        }
    }

    DisposableEffect(tab.id) {
        onWebViewCreated(webView)
        onDispose {
            if (tab.isIncognito) {
                webView.clearCache(true)
                webView.clearHistory()
                webView.clearFormData()
            }
        }
    }

    AndroidView(
        factory = {
            webView.apply {
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val reqUrl = request?.url?.toString()
                        if (tab.adBlockEnabled && AdBlocker.isAdOrTracker(reqUrl)) {
                            view?.post { onTrackerBlocked() }
                            return AdBlocker.createEmptyResponse()
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let { onPageStarted(it) }
                        favicon?.let { onFaviconReceived(it) }
                        onNavigationStateChanged(view?.canGoBack() == true, view?.canGoForward() == true)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        url?.let { onPageFinished(it, view?.title) }
                        onNavigationStateChanged(view?.canGoBack() == true, view?.canGoForward() == true)
                    }

                    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                        super.doUpdateVisitedHistory(view, url, isReload)
                        onNavigationStateChanged(view?.canGoBack() == true, view?.canGoForward() == true)
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onProgressChanged(newProgress)
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        title?.let { onTitleReceived(it) }
                    }

                    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                        super.onReceivedIcon(view, icon)
                        icon?.let { onFaviconReceived(it) }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
