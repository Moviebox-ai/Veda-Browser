package com.example.webview

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
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
    private val onSslErrorCallback: (error: String, url: String) -> Unit,
    private val onRenderProcessCrashedCallback: () -> Unit,
    private val onInternalNavigation: (String) -> Unit
) : WebViewClient() {

    private var currentSslError: String? = null

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val url = request?.url ?: return null
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
        // Strict security: Cancel SSL immediately to prevent MITM attacks
        handler?.cancel()

        val errorMessage = when (error?.primaryError) {
            SslError.SSL_EXPIRED -> "The security certificate has expired."
            SslError.SSL_IDMISMATCH -> "Hostname does not match the security certificate."
            SslError.SSL_NOTYETVALID -> "The security certificate is not yet valid."
            SslError.SSL_UNTRUSTED -> "The certificate authority is untrusted or self-signed."
            SslError.SSL_DATE_INVALID -> "The date of the certificate is invalid."
            else -> "An SSL certificate verification error occurred."
        }

        currentSslError = errorMessage
        val failingUrl = error?.url ?: view?.url ?: ""
        AppLogger.w("VelocityWebViewClient", "SSL Error ($errorMessage) for $failingUrl")
        onSslErrorCallback(errorMessage, failingUrl)
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
        // Return back to safety to protect user
        callback?.backToSafety(true)
    }

    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        AppLogger.e("VelocityWebViewClient", "WebView render process gone. Crashed: ${detail?.didCrash()}")
        onRenderProcessCrashedCallback()
        return true // Prevent host app crash
    }
}
