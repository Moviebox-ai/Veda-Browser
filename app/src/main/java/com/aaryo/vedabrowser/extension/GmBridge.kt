package com.aaryo.vedabrowser.extension

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.webkit.WebView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.aaryo.vedabrowser.model.Extension
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URI
import java.util.concurrent.TimeUnit

class GmBridge(
    private val context: Context,
    private val onOpenInTab: (String) -> Unit,
    private val onGetStorage: suspend (extId: String, key: String) -> String?,
    private val onSetStorage: suspend (extId: String, key: String, value: String) -> Unit,
    private val onDeleteStorage: suspend (extId: String, key: String) -> Unit,
    private val getExtensionById: (String) -> Extension?
) {
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    companion object {
        const val BRIDGE_NAME = "velocityGM"
    }

    fun attachToWebView(webView: WebView) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            try {
                WebViewCompat.addWebMessageListener(
                    webView,
                    BRIDGE_NAME,
                    setOf("*")
                ) { _, message, sourceOrigin, isMainFrame, replyProxy ->
                    handleWebMessage(message.data ?: "", replyProxy)
                }
            } catch (_: Exception) {
                // Ignore if already attached or unsupported
            }
        }
    }

    private fun handleWebMessage(messageJson: String, replyProxy: JavaScriptReplyProxy) {
        if (messageJson.isBlank()) return
        scope.launch {
            try {
                val json = gson.fromJson(messageJson, JsonObject::class.java)
                val action = json.get("action")?.asString ?: return@launch
                val extId = json.get("extId")?.asString ?: ""
                val requestId = json.get("requestId")?.asString ?: ""

                when (action) {
                    "GM_getValue" -> {
                        val key = json.get("key")?.asString ?: ""
                        val defaultVal = if (json.has("defaultValue") && !json.get("defaultValue").isJsonNull) {
                            json.get("defaultValue").asString
                        } else null
                        val stored = onGetStorage(extId, key) ?: defaultVal
                        sendResponse(replyProxy, requestId, success = true, result = stored)
                    }

                    "GM_setValue" -> {
                        val key = json.get("key")?.asString ?: ""
                        val value = json.get("value")?.asString ?: ""
                        onSetStorage(extId, key, value)
                        sendResponse(replyProxy, requestId, success = true, result = "OK")
                    }

                    "GM_deleteValue" -> {
                        val key = json.get("key")?.asString ?: ""
                        onDeleteStorage(extId, key)
                        sendResponse(replyProxy, requestId, success = true, result = "OK")
                    }

                    "GM_openInTab" -> {
                        val url = json.get("url")?.asString ?: ""
                        if (url.isNotBlank()) {
                            launch(Dispatchers.Main) { onOpenInTab(url) }
                        }
                        sendResponse(replyProxy, requestId, success = true, result = "OK")
                    }

                    "GM_setClipboard" -> {
                        val text = json.get("text")?.asString ?: ""
                        launch(Dispatchers.Main) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Userscript Clipboard", text))
                        }
                        sendResponse(replyProxy, requestId, success = true, result = "OK")
                    }

                    "GM_info" -> {
                        val ext = getExtensionById(extId)
                        val infoObj = JsonObject().apply {
                            addProperty("scriptHandler", "Velocity")
                            addProperty("version", "1.0")
                            val scriptObj = JsonObject().apply {
                                addProperty("name", ext?.name ?: "Userscript")
                                addProperty("version", ext?.version ?: "1.0")
                                addProperty("author", ext?.author ?: "Anonymous")
                            }
                            add("script", scriptObj)
                        }
                        sendResponse(replyProxy, requestId, success = true, result = infoObj.toString())
                    }

                    "GM_xmlhttpRequest" -> {
                        handleXmlHttpRequest(json, extId, requestId, replyProxy)
                    }
                }
            } catch (e: Exception) {
                // Log and swallow message parsing errors safely
            }
        }
    }

    private fun handleXmlHttpRequest(
        json: JsonObject,
        extId: String,
        requestId: String,
        replyProxy: JavaScriptReplyProxy
    ) {
        val details = json.getAsJsonObject("details") ?: return
        val url = details.get("url")?.asString ?: return

        // Validate @connect rules if defined by the extension
        val ext = getExtensionById(extId)
        if (ext != null && ext.connectRules.isNotEmpty()) {
            val reqHost = try { URI(url).host?.lowercase() ?: "" } catch (_: Exception) { "" }
            val isAllowed = ext.connectRules.any { rule ->
                rule == "*" || rule.equals(reqHost, ignoreCase = true) || reqHost.endsWith(".${rule.lowercase()}")
            }
            if (!isAllowed) {
                sendResponse(replyProxy, requestId, success = false, error = "Forbidden by @connect allowlist: $reqHost")
                return
            }
        }

        try {
            val method = details.get("method")?.asString?.uppercase() ?: "GET"
            val reqBuilder = Request.Builder().url(url)

            if (details.has("headers")) {
                val headers = details.getAsJsonObject("headers")
                for (headerName in headers.keySet()) {
                    reqBuilder.addHeader(headerName, headers.get(headerName).asString)
                }
            }

            if (method == "POST" || method == "PUT" || method == "PATCH") {
                val bodyStr = details.get("data")?.asString ?: ""
                reqBuilder.method(method, bodyStr.toRequestBody())
            } else {
                reqBuilder.method(method, null)
            }

            httpClient.newCall(reqBuilder.build()).execute().use { response ->
                val respBody = response.body?.string() ?: ""
                val respObj = JsonObject().apply {
                    addProperty("status", response.code)
                    addProperty("statusText", response.message)
                    addProperty("responseText", respBody)
                }
                sendResponse(replyProxy, requestId, success = true, result = respObj.toString())
            }
        } catch (e: Exception) {
            sendResponse(replyProxy, requestId, success = false, error = e.localizedMessage ?: "Network error")
        }
    }

    private fun sendResponse(
        replyProxy: JavaScriptReplyProxy,
        requestId: String,
        success: Boolean,
        result: String? = null,
        error: String? = null
    ) {
        val resp = JsonObject().apply {
            addProperty("requestId", requestId)
            addProperty("success", success)
            if (result != null) addProperty("result", result)
            if (error != null) addProperty("error", error)
        }
        try {
            replyProxy.postMessage(resp.toString())
        } catch (_: Exception) {}
    }

    fun buildInjectedScript(extension: Extension): String {
        val extId = extension.id
        val extName = extension.name.replace("'", "\\'")

        val polyfillJs = """
            var __reqCounter = 0;
            var __reqCallbacks = {};
            if (window.velocityGM && window.velocityGM.onmessage === undefined) {
                window.velocityGM.onmessage = function(event) {
                    try {
                        var data = JSON.parse(event.data);
                        if (data && data.requestId && __reqCallbacks[data.requestId]) {
                            __reqCallbacks[data.requestId](data);
                            delete __reqCallbacks[data.requestId];
                        }
                    } catch(e) {}
                };
            }

            function __sendGM(action, payload, callback) {
                var reqId = 'gm_' + Date.now() + '_' + (++__reqCounter);
                if (callback) __reqCallbacks[reqId] = callback;
                payload.action = action;
                payload.extId = '$extId';
                payload.requestId = reqId;
                if (window.velocityGM) {
                    window.velocityGM.postMessage(JSON.stringify(payload));
                }
            }

            window.__gm_cache = window.__gm_cache || {};
            window.GM_getValue = function(key, defaultVal, callback) {
                if (typeof callback === 'function') {
                    __sendGM('GM_getValue', { key: key, defaultValue: defaultVal }, function(resp) {
                        callback(resp.success && resp.result !== undefined ? resp.result : defaultVal);
                    });
                    return;
                }
                if (window.__gm_cache && window.__gm_cache[key] !== undefined) {
                    return window.__gm_cache[key];
                }
                return defaultVal;
            };
            window.GM_setValue = function(key, val) {
                window.__gm_cache = window.__gm_cache || {};
                window.__gm_cache[key] = val;
                __sendGM('GM_setValue', { key: key, value: String(val) });
            };
            window.GM_deleteValue = function(key) {
                if (window.__gm_cache) delete window.__gm_cache[key];
                __sendGM('GM_deleteValue', { key: key });
            };
            window.GM_addStyle = function(css) {
                var s = document.createElement('style');
                s.textContent = css;
                (document.head || document.documentElement).appendChild(s);
                return s;
            };
            window.GM_openInTab = function(url) {
                __sendGM('GM_openInTab', { url: url });
            };
            window.GM_setClipboard = function(text) {
                __sendGM('GM_setClipboard', { text: text });
            };
            window.GM_xmlhttpRequest = function(details) {
                __sendGM('GM_xmlhttpRequest', { details: details }, function(resp) {
                    if (resp.success && details.onload) {
                        try {
                            details.onload(JSON.parse(resp.result));
                        } catch(e) {
                            if (details.onerror) details.onerror(e);
                        }
                    } else if (!resp.success && details.onerror) {
                        details.onerror({ error: resp.error });
                    }
                });
            };
            window.GM_info = {
                scriptHandler: 'Velocity',
                version: '1.0',
                script: { name: '$extName', version: '${extension.version}' }
            };
        """.trimIndent()

        // Crash-guarded isolated function scope
        return """
            (function() {
                try {
                    $polyfillJs
                    ${extension.source}
                } catch (e) {
                    console.error('[Velocity Userscript Exception: $extName]', e);
                }
            })();
        """.trimIndent()
    }
}
