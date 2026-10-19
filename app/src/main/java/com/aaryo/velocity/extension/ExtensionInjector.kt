package com.aaryo.velocity.extension

import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.aaryo.velocity.model.Extension
import com.aaryo.velocity.model.RunAt
import java.net.URI

object ExtensionInjector {

    fun injectForStage(
        webView: WebView,
        targetUrl: String,
        stage: RunAt,
        extensions: List<Extension>,
        isIncognito: Boolean,
        isGlobalEnabled: Boolean,
        gmBridge: GmBridge
    ): Int {
        if (!isGlobalEnabled) return 0
        if (targetUrl.isBlank() || targetUrl.startsWith("about:") || targetUrl.startsWith("velocity:")) {
            return 0
        }

        val matching = extensions.filter { ext ->
            ext.enabled &&
            ext.runAt == stage &&
            (!isIncognito || ext.allowInIncognito) &&
            MatchPatternMatcher.isUrlMatched(ext.matchPatterns, ext.excludePatterns, targetUrl) &&
            isSiteAllowed(ext, targetUrl)
        }

        var injectedCount = 0
        for (ext in matching) {
            val executableJs = gmBridge.buildInjectedScript(ext)

            if (stage == RunAt.DOCUMENT_START && WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                try {
                    WebViewCompat.addDocumentStartJavaScript(webView, executableJs, setOf("*"))
                    injectedCount++
                } catch (_: Exception) {
                    webView.evaluateJavascript(executableJs, null)
                    injectedCount++
                }
            } else {
                webView.evaluateJavascript(executableJs, null)
                injectedCount++
            }
        }

        return injectedCount
    }

    fun countMatchingExtensions(
        targetUrl: String,
        extensions: List<Extension>,
        isIncognito: Boolean,
        isGlobalEnabled: Boolean
    ): Int {
        if (!isGlobalEnabled || targetUrl.isBlank() || targetUrl.startsWith("about:") || targetUrl.startsWith("velocity:")) {
            return 0
        }
        return extensions.count { ext ->
            ext.enabled &&
            (!isIncognito || ext.allowInIncognito) &&
            MatchPatternMatcher.isUrlMatched(ext.matchPatterns, ext.excludePatterns, targetUrl) &&
            isSiteAllowed(ext, targetUrl)
        }
    }

    private fun isSiteAllowed(extension: Extension, targetUrl: String): Boolean {
        if (extension.siteAllowlist.isEmpty()) return true
        val host = try {
            URI(targetUrl).host?.lowercase() ?: ""
        } catch (_: Exception) {
            ""
        }
        val cleanHost = host.removePrefix("www.")
        return extension.siteAllowlist.any {
            it.lowercase().removePrefix("www.") == cleanHost
        }
    }
}
