package com.aaryo.velocity.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.aaryo.velocity.extension.ExtensionInjector
import com.aaryo.velocity.extension.GmBridge
import com.aaryo.velocity.model.BrowserSettings
import com.aaryo.velocity.model.Extension
import com.aaryo.velocity.model.RunAt
import com.aaryo.velocity.model.Tab
import com.aaryo.velocity.privacy.AdBlocker
import com.aaryo.velocity.security.CertificateHelper
import com.aaryo.velocity.security.CertificateInfo
import com.aaryo.velocity.security.IntentValidator
import com.aaryo.velocity.security.SchemePolicy
import com.aaryo.velocity.ui.dialogs.SslWarningState
import com.aaryo.velocity.ui.dialogs.WebPermissionState
import com.aaryo.velocity.webview.TabWebViewManager

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(
    tab: Tab,
    settings: BrowserSettings,
    tabWebViewManager: TabWebViewManager,
    extensions: List<Extension> = emptyList(),
    isExtensionsGlobalEnabled: Boolean = true,
    gmBridge: GmBridge? = null,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String, String?, CertificateInfo?) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onTitleReceived: (String) -> Unit,
    onFaviconReceived: (Bitmap) -> Unit,
    onNavigationStateChanged: (Boolean, Boolean) -> Unit,
    onTrackerBlocked: () -> Unit = {},
    onItemBlocked: (isTracker: Boolean) -> Unit = {},
    onExtensionsCountChanged: (Int) -> Unit = {},
    onInterceptUserscript: (String) -> Unit = {},
    onDownloadStart: (String, String, String, String) -> Unit,
    onOpenNewTabRequested: (String) -> Unit,
    onShowCustomView: (View, WebChromeClient.CustomViewCallback) -> Unit,
    onHideCustomView: () -> Unit,
    onSslErrorOccurred: (SslWarningState) -> Unit,
    onPermissionRequested: (WebPermissionState) -> Unit,
    findInPageQuery: String,
    modifier: Modifier = Modifier,
    onWebViewReady: (WebView) -> Unit = {}
) {
    val context = LocalContext.current

    // Use rememberUpdatedState so async callbacks never capture stale props
    val currentTab by rememberUpdatedState(tab)
    val currentSettings by rememberUpdatedState(settings)
    val currentAdBlockEnabled by rememberUpdatedState(tab.adBlockEnabled && settings.adBlockGlobalEnabled)
    val currentOnPageStarted by rememberUpdatedState(onPageStarted)
    val currentOnPageFinished by rememberUpdatedState(onPageFinished)
    val currentOnProgressChanged by rememberUpdatedState(onProgressChanged)
    val currentOnTitleReceived by rememberUpdatedState(onTitleReceived)
    val currentOnFaviconReceived by rememberUpdatedState(onFaviconReceived)
    val currentOnNavChanged by rememberUpdatedState(onNavigationStateChanged)
    val currentOnTrackerBlocked by rememberUpdatedState(onTrackerBlocked)
    val currentOnItemBlocked by rememberUpdatedState(onItemBlocked)
    val currentOnDownloadStart by rememberUpdatedState(onDownloadStart)
    val currentOnOpenNewTab by rememberUpdatedState(onOpenNewTabRequested)
    val currentOnShowCustomView by rememberUpdatedState(onShowCustomView)
    val currentOnHideCustomView by rememberUpdatedState(onHideCustomView)
    val currentOnSslError by rememberUpdatedState(onSslErrorOccurred)
    val currentOnPermission by rememberUpdatedState(onPermissionRequested)
    val currentExtensions by rememberUpdatedState(extensions)
    val currentIsExtensionsGlobalEnabled by rememberUpdatedState(isExtensionsGlobalEnabled)
    val currentGmBridge by rememberUpdatedState(gmBridge)
    val currentOnExtensionsCountChanged by rememberUpdatedState(onExtensionsCountChanged)
    val currentOnInterceptUserscript by rememberUpdatedState(onInterceptUserscript)

    // File chooser callback holder
    var fileUploadCallback: ValueCallback<Array<Uri>>? = null
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri>? ->
        fileUploadCallback?.onReceiveValue(uris?.toTypedArray() ?: emptyArray())
        fileUploadCallback = null
    }

    // Retrieve or create the persistent WebView from TabWebViewManager
    val webView = tabWebViewManager.getOrCreateWebView(
        context = context,
        tabId = tab.id,
        isIncognito = tab.isIncognito,
        settings = settings,
        isDesktopMode = tab.isDesktopMode,
        gmBridge = gmBridge
    )

    // Handle Find in Page
    LaunchedEffect(findInPageQuery) {
        if (findInPageQuery.isNotEmpty()) {
            webView.findAllAsync(findInPageQuery)
        } else {
            webView.clearMatches()
        }
    }

    // Load initial/updated URL
    LaunchedEffect(tab.url) {
        if (!tab.isNewTab && tab.url.isNotEmpty() && webView.url != tab.url) {
            val headers = mutableMapOf<String, String>()
            if (settings.doNotTrackEnabled) {
                headers["DNT"] = "1"
            }
            webView.loadUrl(tab.url, headers)
        }
    }

    // Reflect ad block enabled/disabled cosmetic state dynamically
    LaunchedEffect(currentAdBlockEnabled) {
        AdBlocker.setCosmeticAdBlockState(webView, currentAdBlockEnabled)
    }

    // Attach/Detach lifecycle management
    DisposableEffect(tab.id) {
        onWebViewReady(webView)
        onDispose {
            // Save state when switching away from this tab
            tabWebViewManager.saveTabState(tab.id)
            // Detach from parent view to allow clean re-attachment
            (webView.parent as? ViewGroup)?.removeView(webView)
        }
    }

    AndroidView(
        factory = {
            // Guarantee removal from any previous layout parent first
            (webView.parent as? ViewGroup)?.removeView(webView)

            webView.apply {
                webViewClient = object : WebViewClient() {

                    private fun handleUrlLoading(
                        view: WebView?,
                        urlString: String,
                        refererHost: String?
                    ): Boolean {
                        // 1. Intercept ad or tracker redirects when Shield is enabled
                        if (currentAdBlockEnabled && AdBlocker.isAdOrTracker(urlString, refererHost)) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                currentOnTrackerBlocked()
                            }
                            return true
                        }

                        // 2. Intercept *.user.js scripts to prompt install dialog
                        if (urlString.endsWith(".user.js", ignoreCase = true) ||
                            urlString.substringBefore('?').endsWith(".user.js", ignoreCase = true)
                        ) {
                            currentOnInterceptUserscript(urlString)
                            return true
                        }

                        // 3. Special handling for intent:, chrome custom schemes, and search redirects
                        // Extract embedded web URLs so searches and queries stay inside Velocity WebView!
                        val extractedWebUrl = IntentValidator.extractWebUrlFromIntent(urlString)
                        if (!extractedWebUrl.isNullOrBlank()) {
                            view?.loadUrl(extractedWebUrl)
                            return true
                        }

                        // 4. Block any intent or scheme attempting to escape to Chrome or external browsers
                        if (IntentValidator.isBrowserIntent(urlString) || SchemePolicy.isBrowserCustomScheme(urlString)) {
                            return true
                        }

                        // 5. Regular HTTP/HTTPS web schemes -> always stay inside Velocity WebView!
                        if (SchemePolicy.isWebScheme(urlString)) {
                            return false
                        }

                        // 6. Block unsafe internal schemes (file://, content://)
                        if (SchemePolicy.isBlockedScheme(urlString)) {
                            return true
                        }

                        // 7. Non-browser native app schemes (e.g. tel:, mailto:, sms:, geo:, whatsapp:, upi:)
                        if (SchemePolicy.isExternalScheme(urlString) || urlString.startsWith("intent:", ignoreCase = true)) {
                            val safeIntent = IntentValidator.parseSafeIntent(context, urlString)
                            if (safeIntent != null) {
                                try {
                                    context.startActivity(safeIntent)
                                } catch (_: Exception) {
                                    // Activity not found on device
                                }
                            }
                            return true
                        }

                        return true
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val uri = request?.url ?: return false
                        val urlString = uri.toString()
                        val refererHost = request.requestHeaders?.let { headers ->
                            (headers["Referer"] ?: headers["referer"])?.let { ref ->
                                try { Uri.parse(ref).host } catch (_: Exception) { null }
                            }
                        } ?: try { Uri.parse(currentTab.url).host } catch (_: Exception) { null }

                        return handleUrlLoading(view, urlString, refererHost)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        if (url == null) return false
                        val currentSiteHost = try { Uri.parse(currentTab.url).host } catch (_: Exception) { null }
                        return handleUrlLoading(view, url, currentSiteHost)
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val reqUrl = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)
                        val headers = request.requestHeaders

                        // Safely determine parent site host without calling WebView methods on the background thread
                        val refererHost = headers?.let { h ->
                            (h["Referer"] ?: h["referer"])?.let { ref ->
                                try { Uri.parse(ref).host } catch (_: Exception) { null }
                            }
                        }
                        val currentSiteHost = refererHost
                            ?: (if (request.isForMainFrame) request.url?.host else null)
                            ?: try { Uri.parse(currentTab.url).host } catch (_: Exception) { null }

                        val resType = AdBlocker.detectResourceType(reqUrl, headers, request.isForMainFrame)

                        if (currentAdBlockEnabled && AdBlocker.isAdOrTracker(reqUrl, currentSiteHost, resType, headers)) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                currentOnTrackerBlocked()
                            }
                            return AdBlocker.createEmptyResponse(reqUrl)
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
                        if (url != null && currentAdBlockEnabled) {
                            val currentSiteHost = try { Uri.parse(currentTab.url).host } catch (_: Exception) { null }
                            val resType = AdBlocker.detectResourceType(url)
                            if (AdBlocker.isAdOrTracker(url, currentSiteHost, resType)) {
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    currentOnTrackerBlocked()
                                }
                                return AdBlocker.createEmptyResponse(url)
                            }
                        }
                        return super.shouldInterceptRequest(view, url)
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let { currentOnPageStarted(it) }
                        favicon?.let { currentOnFaviconReceived(it) }
                        currentOnNavChanged(view?.canGoBack() == true, view?.canGoForward() == true)

                        // Manage cosmetic ad block on page start
                        if (view != null) {
                            if (currentAdBlockEnabled) {
                                AdBlocker.injectCosmeticAdBlock(view)
                            } else {
                                AdBlocker.removeCosmeticAdBlock(view)
                            }
                        }

                        if (url != null && view != null && currentGmBridge != null) {
                            currentGmBridge?.let { bridge ->
                                ExtensionInjector.injectForStage(
                                    webView = view,
                                    targetUrl = url,
                                    stage = RunAt.DOCUMENT_START,
                                    extensions = currentExtensions,
                                    isIncognito = currentTab.isIncognito,
                                    isGlobalEnabled = currentIsExtensionsGlobalEnabled,
                                    gmBridge = bridge
                                )
                                val matchingCount = ExtensionInjector.countMatchingExtensions(
                                    targetUrl = url,
                                    extensions = currentExtensions,
                                    isIncognito = currentTab.isIncognito,
                                    isGlobalEnabled = currentIsExtensionsGlobalEnabled
                                )
                                currentOnExtensionsCountChanged(matchingCount)
                            }
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Ensure ad elements and placeholders are collapsed once page has loaded
                        if (view != null) {
                            if (currentAdBlockEnabled) {
                                AdBlocker.injectCosmeticAdBlock(view)
                            } else {
                                AdBlocker.removeCosmeticAdBlock(view)
                            }
                        }
                        val cert = view?.certificate
                        val isSecure = url?.startsWith("https://") == true && cert != null
                        val certInfo = if (url != null) {
                            CertificateHelper.extractInfo(cert, url, isSecure)
                        } else null

                        url?.let { currentOnPageFinished(it, view?.title, certInfo) }
                        currentOnNavChanged(view?.canGoBack() == true, view?.canGoForward() == true)

                        // Inject DOCUMENT_END and schedule DOCUMENT_IDLE userscripts
                        if (url != null && view != null && currentGmBridge != null) {
                            currentGmBridge?.let { bridge ->
                                ExtensionInjector.injectForStage(
                                    webView = view,
                                    targetUrl = url,
                                    stage = RunAt.DOCUMENT_END,
                                    extensions = currentExtensions,
                                    isIncognito = currentTab.isIncognito,
                                    isGlobalEnabled = currentIsExtensionsGlobalEnabled,
                                    gmBridge = bridge
                                )
                                view.postDelayed({
                                    ExtensionInjector.injectForStage(
                                        webView = view,
                                        targetUrl = url,
                                        stage = RunAt.DOCUMENT_IDLE,
                                        extensions = currentExtensions,
                                        isIncognito = currentTab.isIncognito,
                                        isGlobalEnabled = currentIsExtensionsGlobalEnabled,
                                        gmBridge = bridge
                                    )
                                }, 150)
                                val matchingCount = ExtensionInjector.countMatchingExtensions(
                                    targetUrl = url,
                                    extensions = currentExtensions,
                                    isIncognito = currentTab.isIncognito,
                                    isGlobalEnabled = currentIsExtensionsGlobalEnabled
                                )
                                currentOnExtensionsCountChanged(matchingCount)
                            }
                        }

                        // Inject DNT property into JavaScript runtime if DNT is active
                        if (currentSettings.doNotTrackEnabled) {
                            view?.evaluateJavascript(
                                "try { Object.defineProperty(navigator, 'doNotTrack', { get: () => '1' }); } catch(e){}",
                                null
                            )
                        }
                    }

                    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                        super.doUpdateVisitedHistory(view, url, isReload)
                        currentOnNavChanged(view?.canGoBack() == true, view?.canGoForward() == true)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            val failedUrl = request.url?.toString() ?: ""
                            val errorDesc = error?.description?.toString() ?: "Connection failed"
                            val offlineHtml = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                <meta name="viewport" content="width=device-width, initial-scale=1">
                                <style>
                                  body { font-family: -apple-system, Roboto, sans-serif; background: #0B1120; color: #F8FAFC; padding: 40px 24px; text-align: center; }
                                  h2 { color: #00E5FF; margin-top: 32px; font-size: 22px; }
                                  p { color: #94A3B8; font-size: 14px; line-height: 1.6; }
                                  .btn { display: inline-block; margin-top: 24px; background: #0284C7; color: #fff; padding: 12px 28px; border-radius: 24px; text-decoration: none; font-weight: bold; }
                                </style>
                                </head>
                                <body>
                                  <h2>Unable to reach site</h2>
                                  <p>Velocity could not load <strong>$failedUrl</strong>.<br>$errorDesc</p>
                                  <a class="btn" href="javascript:location.reload()">Reload Page</a>
                                </body>
                                </html>
                            """.trimIndent()
                            view?.loadDataWithBaseURL(null, offlineHtml, "text/html", "UTF-8", null)
                        }
                    }

                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?
                    ) {
                        if (handler != null) {
                            val failedUrl = error?.url ?: view?.url ?: ""
                            val description = when (error?.primaryError) {
                                SslError.SSL_EXPIRED -> "The certificate has expired"
                                SslError.SSL_IDMISMATCH -> "Hostname mismatch"
                                SslError.SSL_UNTRUSTED -> "Certificate authority is not trusted"
                                SslError.SSL_NOTYETVALID -> "The certificate is not yet valid"
                                SslError.SSL_DATE_INVALID -> "Certificate date is invalid"
                                else -> "Invalid SSL certificate"
                            }
                            currentOnSslError(
                                SslWarningState(
                                    url = failedUrl,
                                    primaryErrorDescription = description,
                                    handler = handler
                                )
                            )
                        }
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        currentOnProgressChanged(newProgress)
                        // Trigger cosmetic ad hiding early during DOM building (e.g. at 30% and 70%)
                        if (currentAdBlockEnabled && (newProgress == 30 || newProgress == 70) && view != null) {
                            AdBlocker.injectCosmeticAdBlock(view)
                        }
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        title?.let { currentOnTitleReceived(it) }
                    }

                    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                        super.onReceivedIcon(view, icon)
                        icon?.let { currentOnFaviconReceived(it) }
                    }

                    override fun onCreateWindow(
                        view: WebView?,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: Message?
                    ): Boolean {
                        if (resultMsg != null) {
                            // Block unprompted popups when Shield is active
                            if (currentAdBlockEnabled && !isUserGesture) {
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    currentOnTrackerBlocked()
                                }
                                return true
                            }
                            val hitTest = view?.hitTestResult
                            val targetUrl = hitTest?.extra ?: ""
                            if (currentAdBlockEnabled && targetUrl.isNotEmpty() && AdBlocker.isAdOrTracker(targetUrl)) {
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    currentOnTrackerBlocked()
                                }
                                return true
                            }
                            if (targetUrl.isNotEmpty()) {
                                currentOnOpenNewTab(targetUrl)
                                return true
                            }

                            // Intercept window.open popups via transport WebView
                            val tempWebView = WebView(view?.context ?: return false).apply {
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(v: WebView?, req: WebResourceRequest?): Boolean {
                                        val url = req?.url?.toString() ?: return false
                                        val siteHost = try { Uri.parse(currentTab.url).host } catch (_: Exception) { null }
                                        if (currentAdBlockEnabled && AdBlocker.isAdOrTracker(url, siteHost)) {
                                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                currentOnTrackerBlocked()
                                            }
                                            v?.destroy()
                                            return true
                                        }
                                        currentOnOpenNewTab(url)
                                        v?.destroy()
                                        return true
                                    }

                                    @Deprecated("Deprecated in Java")
                                    override fun shouldOverrideUrlLoading(v: WebView?, url: String?): Boolean {
                                        if (url == null) return false
                                        val siteHost = try { Uri.parse(currentTab.url).host } catch (_: Exception) { null }
                                        if (currentAdBlockEnabled && AdBlocker.isAdOrTracker(url, siteHost)) {
                                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                currentOnTrackerBlocked()
                                            }
                                            v?.destroy()
                                            return true
                                        }
                                        currentOnOpenNewTab(url)
                                        v?.destroy()
                                        return true
                                    }
                                }
                            }
                            val transport = resultMsg.obj as? WebView.WebViewTransport
                            if (transport != null) {
                                transport.webView = tempWebView
                                resultMsg.sendToTarget()
                                return true
                            }
                            return false
                        }
                        return false
                    }

                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        fileUploadCallback?.onReceiveValue(null)
                        fileUploadCallback = filePathCallback
                        val mimeTypes = fileChooserParams?.acceptTypes ?: arrayOf("*/*")
                        fileChooserLauncher.launch(mimeTypes)
                        return true
                    }

                    override fun onPermissionRequest(request: PermissionRequest?) {
                        if (request != null) {
                            currentOnPermission(
                                WebPermissionState.ResourcePermission(
                                    origin = request.origin.toString(),
                                    resources = request.resources,
                                    request = request
                                )
                            )
                        }
                    }

                    override fun onGeolocationPermissionsShowPrompt(
                        origin: String?,
                        callback: GeolocationPermissions.Callback?
                    ) {
                        if (origin != null && callback != null) {
                            currentOnPermission(
                                WebPermissionState.GeolocationPermission(
                                    origin = origin,
                                    callback = callback
                                )
                            )
                        }
                    }

                    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                        if (view != null && callback != null) {
                            currentOnShowCustomView(view, callback)
                        }
                    }

                    override fun onHideCustomView() {
                        currentOnHideCustomView()
                    }
                }

                setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                    currentOnDownloadStart(url, contentDisposition, mimetype ?: "application/octet-stream", userAgent ?: "")
                }
            }
        },
        update = { currentView ->
            // Update webview settings dynamically when settings change
            tabWebViewManager.applyWebSettings(
                webView = currentView,
                settings = settings,
                isDesktopMode = tab.isDesktopMode,
                isIncognito = tab.isIncognito
            )
        },
        modifier = modifier.fillMaxSize()
    )
}
