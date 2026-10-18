package com.example.webview

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.webkit.HttpAuthHandler
import android.webkit.RenderProcessGoneDetail
import android.webkit.SafeBrowsingResponse
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import com.example.security.ContentBlocker
import com.example.security.IntentValidator
import com.example.security.SchemePolicy
import com.example.util.AppLogger
import com.example.util.UrlResolver

class VelocityWebViewClient(
    private val context: Context,
    private val contentBlocker: ContentBlocker,
    private val onPageStartedCallback: (url: String) -> Unit,
    private val onPageFinishedCallback: (url: String, canGoBack: Boolean, canGoForward: Boolean, isSecure: Boolean) -> Unit,
    private val onSslErrorCallback: (error: String, url: String, proceed: () -> Unit, cancel: () -> Unit) -> Unit,
    private val onRenderProcessCrashedCallback: () -> Unit,
    private val onInternalNavigation: (String) -> Unit,
    private val onHttpAuthRequestCallback: ((host: String, realm: String, proceed: (String, String) -> Unit, cancel: () -> Unit) -> Unit)? = null
) : WebViewClient() {

    private var currentSslError: String? = null

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val url = request?.url ?: return null
        if (request.isForMainFrame) return null
        if (contentBlocker.shouldBlock(url)) {
            AppLogger.d("VelocityWebViewClient", "Blocked tracker/ad: ${url.host}")
            return contentBlocker.createBlockedResponse()
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val uri = request?.url ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        val urlString = uri.toString()

        if (SchemePolicy.isInternalScheme(scheme)) {
            onInternalNavigation(urlString)
            return true
        }

        if (SchemePolicy.isSafeExternalScheme(scheme) || scheme == "intent") {
            return IntentValidator.handleExternalUri(context, urlString)
        }

        if (SchemePolicy.isForbidden(scheme)) {
            AppLogger.w("VelocityWebViewClient", "Blocked forbidden navigation scheme: $scheme")
            return true
        }

        return false // Allow WebView to load http/https normally
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        currentSslError = null
        if (url != null) {
            onPageStartedCallback(url)
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (url != null) {
            val isHttps = UrlResolver.isSecure(url)
            val isSecure = isHttps && (currentSslError == null)
            val canBack = view?.canGoBack() ?: false
            val canForward = view?.canGoForward() ?: false
            onPageFinishedCallback(url, canBack, canForward, isSecure)
        }
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        val failingUrl = error?.url ?: view?.url ?: ""
        val currentViewUrl = view?.url ?: ""

        // Check if error is for the main frame
        val isMainFrame = failingUrl.isEmpty() || failingUrl == currentViewUrl ||
                try {
                    val failHost = Uri.parse(failingUrl).host
                    val viewHost = Uri.parse(currentViewUrl).host
                    failHost != null && failHost == viewHost
                } catch (_: Exception) {
                    false
                }

        // Subresource SSL errors (iframes, ads, scripts) must be canceled immediately to prevent request hanging
        if (!isMainFrame) {
            AppLogger.w("VelocityWebViewClient", "Subresource SSL error canceled: $failingUrl")
            handler?.cancel()
            return
        }

        val errorMessage = when (error?.primaryError) {
            SslError.SSL_EXPIRED -> "The security certificate has expired."
            SslError.SSL_IDMISMATCH -> "Hostname does not match the security certificate."
            SslError.SSL_NOTYETVALID -> "The security certificate is not yet valid."
            SslError.SSL_UNTRUSTED -> "The certificate authority is untrusted or self-signed."
            SslError.SSL_DATE_INVALID -> "The date of the certificate is invalid."
            else -> "An SSL certificate verification error occurred."
        }

        currentSslError = errorMessage
        AppLogger.w("VelocityWebViewClient", "Main-frame SSL Error ($errorMessage) for $failingUrl")
        onSslErrorCallback(
            errorMessage,
            failingUrl,
            { handler?.proceed() },
            { handler?.cancel() }
        )
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            AppLogger.w("VelocityWebViewClient", "Main frame load error: ${error?.description}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    override fun onSafeBrowsingHit(
        view: WebView?,
        request: WebResourceRequest?,
        threatType: Int,
        callback: SafeBrowsingResponse?
    ) {
        AppLogger.w("VelocityWebViewClient", "Safe Browsing Hit: Threat type $threatType")
        // backToSafety(false) prevents sending telemetry without user consent
        callback?.backToSafety(false)
    }

    override fun onReceivedHttpAuthRequest(
        view: WebView?,
        handler: HttpAuthHandler?,
        host: String?,
        realm: String?
    ) {
        val targetHost = host ?: ""
        val targetRealm = realm ?: ""
        if (onHttpAuthRequestCallback != null) {
            onHttpAuthRequestCallback.invoke(
                targetHost,
                targetRealm,
                { username, password -> handler?.proceed(username, password) },
                { handler?.cancel() }
            )
        } else {
            handler?.cancel()
        }
    }

    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        AppLogger.e("VelocityWebViewClient", "WebView render process gone. Crashed: ${detail?.didCrash()}")
        onRenderProcessCrashedCallback()
        return true // Prevent host app crash
    }
}
