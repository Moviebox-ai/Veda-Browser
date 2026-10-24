package com.aaryo.vedabrowser.performance

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.aaryo.vedabrowser.model.PagePerformanceMetrics
import com.google.gson.JsonParser

/**
 * Chromium Web Message Listener Bridge (Real-Time Zero-Lag Bridge).
 *
 * Replaces old reflection-based @JavascriptInterface with modern WebViewCompat.addWebMessageListener
 * to establish an asynchronous, non-blocking bidirectional data stream between the webpage
 * and Android native code for real-time blocked tracker counters, HTTP/3 protocol indicators,
 * and navigation timing performance meters.
 */
class ChromiumWebMessageBridge(
    private val tabId: String,
    private val onTrackerBlocked: (isTracker: Boolean) -> Unit,
    private val onMetricsReceived: (PagePerformanceMetrics) -> Unit,
    private val onMediaDetected: ((url: String, pageUrl: String, pageTitle: String, mimeType: String?, mediaType: String?) -> Unit)? = null
) {

    private val mainHandler: Handler? by lazy {
        try { Handler(Looper.getMainLooper()) } catch (_: Throwable) { null }
    }

    private fun dispatchToMain(action: () -> Unit) {
        val handler = mainHandler
        if (handler != null) {
            try {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    action()
                } else {
                    handler.post(action)
                }
                return
            } catch (_: Throwable) {
            }
        }
        action()
    }

    companion object {
        const val BRIDGE_OBJECT_NAME = "vedaBridge"

        /**
         * Client-side JavaScript snippet that continuously streams telemetry
         * and Navigation Timing (DOM interactive, TTFB, DNS, HTTP/3 QUIC nextHopProtocol)
         * to native Android through window.vedaBridge.postMessage().
         * Also provides window.vedaBridgeNotifyBlocked for real-time zero-lag ad/tracker counters.
         */
        val TELEMETRY_SCRIPT = """
            (function() {
                // Global real-time zero-lag bridge dispatcher for blocked ads & trackers
                window.vedaBridgeNotifyBlocked = function(isTracker) {
                    try {
                        var payload = JSON.stringify({
                            type: 'AD_BLOCKED',
                            isTracker: !!isTracker
                        });
                        if (window.vedaBridge && typeof window.vedaBridge.postMessage === 'function') {
                            window.vedaBridge.postMessage(payload);
                        } else if (window.vedaBridgeLegacy && typeof window.vedaBridgeLegacy.postMessageLegacy === 'function') {
                            window.vedaBridgeLegacy.postMessageLegacy(payload);
                        }
                    } catch(e) {}
                };

                if (window.__vedaTelemetryInstalled) return;
                window.__vedaTelemetryInstalled = true;

                function sendTelemetry() {
                    try {
                        var perf = window.performance;
                        if (!perf) return;
                        
                        var entries = (typeof perf.getEntriesByType === 'function') 
                            ? perf.getEntriesByType('navigation') : null;
                        var nav = (entries && entries.length > 0) ? entries[0] : null;
                        var timing = perf.timing;

                        var dnsTime = 0;
                        var connectTime = 0;
                        var ttfb = 0;
                        var domInteractive = 0;
                        var totalLoadTime = 0;
                        var protocol = 'HTTP/2';

                        if (nav) {
                            dnsTime = Math.max(0, Math.round(nav.domainLookupEnd - nav.domainLookupStart));
                            connectTime = Math.max(0, Math.round(nav.connectEnd - nav.connectStart));
                            ttfb = Math.max(0, Math.round(nav.responseStart - nav.requestStart));
                            domInteractive = Math.max(0, Math.round(nav.domInteractive - nav.startTime));
                            totalLoadTime = Math.max(0, Math.round((nav.loadEventEnd || nav.domContentLoadedEventEnd || nav.responseEnd) - nav.startTime));
                            
                            var proto = (nav.nextHopProtocol || '').toLowerCase();
                            if (proto === 'h3' || proto.indexOf('h3-') === 0 || proto.indexOf('quic') !== -1 || proto.indexOf('spdy') !== -1) {
                                protocol = 'HTTP/3 (QUIC)';
                            } else if (proto === 'h2') {
                                protocol = 'HTTP/2';
                            } else if (proto === 'http/1.1') {
                                protocol = 'HTTP/1.1';
                            } else {
                                // Inspect sub-resource protocols if navigation hasn't finalized
                                var resEntries = (typeof perf.getEntriesByType === 'function') ? perf.getEntriesByType('resource') : null;
                                if (resEntries && resEntries.length > 0) {
                                    for (var i = 0; i < Math.min(resEntries.length, 10); i++) {
                                        var rProto = (resEntries[i].nextHopProtocol || '').toLowerCase();
                                        if (rProto.indexOf('h3') !== -1 || rProto.indexOf('quic') !== -1) {
                                            protocol = 'HTTP/3 (QUIC)';
                                            break;
                                        }
                                    }
                                }
                            }
                        } else if (timing) {
                            var navStart = timing.navigationStart || 0;
                            if (timing.domainLookupEnd > timing.domainLookupStart) {
                                dnsTime = timing.domainLookupEnd - timing.domainLookupStart;
                            }
                            if (timing.connectEnd > timing.connectStart) {
                                connectTime = timing.connectEnd - timing.connectStart;
                            }
                            if (timing.responseStart > timing.requestStart) {
                                ttfb = timing.responseStart - timing.requestStart;
                            }
                            if (timing.domInteractive > navStart) {
                                domInteractive = timing.domInteractive - navStart;
                            }
                            if (timing.loadEventEnd > navStart) {
                                totalLoadTime = timing.loadEventEnd - navStart;
                            } else if (timing.domContentLoadedEventEnd > navStart) {
                                totalLoadTime = timing.domContentLoadedEventEnd - navStart;
                            }
                        }

                        var payload = JSON.stringify({
                            type: 'PERFORMANCE_METRICS',
                            dnsTime: dnsTime,
                            connectTime: connectTime,
                            ttfb: ttfb,
                            domInteractive: domInteractive,
                            totalLoadTime: totalLoadTime,
                            protocol: protocol
                        });

                        if (window.vedaBridge && typeof window.vedaBridge.postMessage === 'function') {
                            window.vedaBridge.postMessage(payload);
                        } else if (window.vedaBridgeLegacy && typeof window.vedaBridgeLegacy.postMessageLegacy === 'function') {
                            window.vedaBridgeLegacy.postMessageLegacy(payload);
                        }
                    } catch(e) {}
                }

                // Send early telemetry as soon as DOM is interactive
                if (document.readyState === 'interactive' || document.readyState === 'complete') {
                    sendTelemetry();
                } else {
                    document.addEventListener('DOMContentLoaded', function() {
                        setTimeout(sendTelemetry, 50);
                    });
                }

                // Send final telemetry once all images and subresources have completed
                window.addEventListener('load', function() {
                    setTimeout(sendTelemetry, 150);
                });
            })();
        """.trimIndent()
    }

    /**
     * Attaches modern Web Message Listener or falls back to @JavascriptInterface.
     */
    fun attachToWebView(webView: WebView) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            try {
                WebViewCompat.addWebMessageListener(
                    webView,
                    BRIDGE_OBJECT_NAME,
                    setOf("*"),
                    object : WebViewCompat.WebMessageListener {
                        override fun onPostMessage(
                            view: WebView,
                            message: WebMessageCompat,
                            sourceOrigin: Uri,
                            isMainFrame: Boolean,
                            replyProxy: JavaScriptReplyProxy
                        ) {
                            val rawData = message.data ?: return
                            handleIncomingMessage(rawData, replyProxy)
                        }
                    }
                )
            } catch (_: Throwable) {
                attachLegacyInterface(webView)
            }
        } else {
            attachLegacyInterface(webView)
        }
    }

    private fun attachLegacyInterface(webView: WebView) {
        try {
            webView.addJavascriptInterface(LegacyInterface(), "vedaBridgeLegacy")
        } catch (_: Throwable) {
        }
    }

    fun handleIncomingMessage(jsonString: String, replyProxy: JavaScriptReplyProxy? = null) {
        try {
            val json = JsonParser.parseString(jsonString).asJsonObject
            val type = if (json.has("type") && !json.get("type").isJsonNull) json.get("type").asString else ""
            when (type) {
                "PERFORMANCE_METRICS" -> {
                    val metrics = PagePerformanceMetrics(
                        dnsTimeMs = if (json.has("dnsTime")) json.get("dnsTime").asLong else 0L,
                        connectTimeMs = if (json.has("connectTime")) json.get("connectTime").asLong else 0L,
                        ttfbMs = if (json.has("ttfb")) json.get("ttfb").asLong else 0L,
                        domInteractiveMs = if (json.has("domInteractive")) json.get("domInteractive").asLong else 0L,
                        totalLoadTimeMs = if (json.has("totalLoadTime")) json.get("totalLoadTime").asLong else 0L,
                        protocol = if (json.has("protocol")) json.get("protocol").asString else "HTTP/2",
                        isPrewarmed = ChromiumPrewarmer.isWarm
                    )
                    dispatchToMain {
                        onMetricsReceived(metrics)
                    }
                    replyProxy?.postMessage("{\"status\":\"ok\"}")
                }
                "AD_BLOCKED" -> {
                    val isTracker = if (json.has("isTracker")) json.get("isTracker").asBoolean else false
                    dispatchToMain {
                        onTrackerBlocked(isTracker)
                    }
                }
                "MEDIA_DETECTED" -> {
                    val url = if (json.has("url")) json.get("url").asString else ""
                    val pageUrl = if (json.has("pageUrl")) json.get("pageUrl").asString else ""
                    val pageTitle = if (json.has("pageTitle")) json.get("pageTitle").asString else ""
                    val mimeType = if (json.has("mimeType") && !json.get("mimeType").isJsonNull) json.get("mimeType").asString else null
                    val mediaType = if (json.has("mediaType") && !json.get("mediaType").isJsonNull) json.get("mediaType").asString else null
                    if (url.isNotBlank()) {
                        dispatchToMain {
                            onMediaDetected?.invoke(url, pageUrl, pageTitle, mimeType, mediaType)
                        }
                    }
                }
            }
        } catch (_: Throwable) {
        }
    }

    inner class LegacyInterface {
        @JavascriptInterface
        fun postMessageLegacy(jsonString: String) {
            handleIncomingMessage(jsonString, null)
        }
    }
}
