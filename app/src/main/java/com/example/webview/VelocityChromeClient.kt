package com.example.webview

import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.example.util.AppLogger

class VelocityChromeClient(
    private val onProgressChangedCallback: (Int) -> Unit,
    private val onTitleReceivedCallback: (String) -> Unit,
    private val onIconReceivedCallback: (Bitmap?) -> Unit,
    private val onShowCustomViewCallback: (View, WebChromeClient.CustomViewCallback) -> Unit,
    private val onHideCustomViewCallback: () -> Unit,
    private val onShowFileChooserCallback: ((ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean)? = null,
    private val onJsAlertCallback: ((message: String, result: JsResult) -> Unit)? = null,
    private val onJsConfirmCallback: ((message: String, result: JsResult) -> Unit)? = null,
    private val onJsPromptCallback: ((message: String, defaultValue: String, result: JsPromptResult) -> Unit)? = null,
    private val onJsBeforeUnloadCallback: ((message: String, result: JsResult) -> Unit)? = null,
    private val onGeolocationPromptCallback: ((origin: String, callback: GeolocationPermissions.Callback) -> Unit)? = null
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChangedCallback(newProgress)
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        if (!title.isNullOrBlank()) {
            onTitleReceivedCallback(title)
        }
    }

    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
        super.onReceivedIcon(view, icon)
        onIconReceivedCallback(icon)
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (view != null && callback != null) {
            onShowCustomViewCallback(view, callback)
        }
    }

    override fun onHideCustomView() {
        onHideCustomViewCallback()
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        return onShowFileChooserCallback?.invoke(filePathCallback, fileChooserParams)
            ?: super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
    }

    override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        if (result == null) return false
        val msg = message ?: ""
        if (onJsAlertCallback != null) {
            onJsAlertCallback.invoke(msg, result)
            return true
        }
        return super.onJsAlert(view, url, message, result)
    }

    override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        if (result == null) return false
        val msg = message ?: ""
        if (onJsConfirmCallback != null) {
            onJsConfirmCallback.invoke(msg, result)
            return true
        }
        return super.onJsConfirm(view, url, message, result)
    }

    override fun onJsPrompt(
        view: WebView?,
        url: String?,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult?
    ): Boolean {
        if (result == null) return false
        val msg = message ?: ""
        val def = defaultValue ?: ""
        if (onJsPromptCallback != null) {
            onJsPromptCallback.invoke(msg, def, result)
            return true
        }
        return super.onJsPrompt(view, url, message, defaultValue, result)
    }

    override fun onJsBeforeUnload(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        if (result == null) return false
        val msg = message ?: ""
        if (onJsBeforeUnloadCallback != null) {
            onJsBeforeUnloadCallback.invoke(msg, result)
            return true
        }
        return super.onJsBeforeUnload(view, url, message, result)
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        if (origin == null || callback == null) return
        AppLogger.d("VelocityChromeClient", "Geolocation requested by $origin")
        if (onGeolocationPromptCallback != null) {
            onGeolocationPromptCallback.invoke(origin, callback)
        } else {
            // Default to deny if no UI prompt wired
            callback.invoke(origin, false, false)
        }
    }
}
