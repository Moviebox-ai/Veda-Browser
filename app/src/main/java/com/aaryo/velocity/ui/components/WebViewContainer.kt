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
    onTrackerBlocked: () -> Unit,
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
    val currentAdBlockEnabled by rememberUpdatedState(tab.adBlockEnabled)
    val currentOnPageStarted by rememberUpdatedState(onPageStarted)
    val currentOnPageFinished by rememberUpdatedState(onPageFinished)
    val currentOnProgressChanged by rememberUpdatedState(onProgressChanged)
    val currentOnTitleReceived by rememberUpdatedState(onTitleReceived)
    val currentOnFaviconReceived by rememberUpdatedState(onFaviconReceived)
    val currentOnNavChanged by rememberUpdatedState(onNavigationStateChanged)
    val currentOnTrackerBlocked by rememberUpdatedState(onTrackerBlocked)
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

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val uri = request?.url ?: return false
                        val urlString = uri.toString()

                        // Intercept *.user.js scripts to prompt install dialog
                        if (urlString.endsWith(".user.js", ignoreCase = true) || uri.path?.endsWith(".user.js", ignoreCase = true) == true) {
                            currentOnInterceptUserscript(urlString)
                            return true
                        }

                        if (SchemePolicy.isWebScheme(urlString)) {
                            return false
                        }

                        if (SchemePolicy.isBlockedScheme(urlString)) {
                            return true
                        }

                        if (SchemePolicy.isExternalScheme(urlString)) {
                            val safeIntent = IntentValidator.parseSafeIntent(context, urlString)
                            if (safeIntent != null) {
                                try {
                                    context.startActivity(safeIntent)
                                } catch (_: Exception) {
                                    // Activity not found
                                }
                            }
                            return true
                        }

                        return false
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val reqUrl = request?.url?.toString()
                        val currentSiteHost = view?.url?.let { Uri.parse(it).host }

                        if (currentAdBlockEnabled && AdBlocker.isAdOrTracker(reqUrl, currentSiteHost)) {
                            view?.post { currentOnTrackerBlocked() }
                            return AdBlocker.createEmptyResponse()
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let { currentOnPageStarted(it) }
                        favicon?.let { currentOnFaviconReceived(it) }
                        currentOnNavChanged(view?.canGoBack() == true, view?.canGoForward() == true)

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
                            val hitTest = view?.hitTestResult
                            val targetUrl = hitTest?.extra ?: ""
                            currentOnOpenNewTab(targetUrl)
                            return true
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
