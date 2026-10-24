package com.aaryo.vedabrowser.performance

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsService
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.webkit.WebViewCompat

/**
 * Chromium Pre-warming and Speculative Loading Engine.
 *
 * Utilizes CustomTabsClient.warmup() to prime the underlying Google Chromium (Blink + V8)
 * process, initialize network sockets, pre-allocate GPU memory, and pre-compile JavaScript
 * bytecode before user navigation, achieving near 0-second page load latencies.
 */
object ChromiumPrewarmer {

    private var customTabsClient: CustomTabsClient? = null
    private var customTabsSession: CustomTabsSession? = null
    private var isWarmingUp = false
    private var isWarmedUp = false
    private var lastWarmedUrl: String? = null

    private val warmedUrls = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    val isWarm: Boolean
        get() = isWarmedUp

    /**
     * Initializes and warms up Chromium native engine services asynchronously at application startup.
     */
    fun initialize(context: Context) {
        if (isWarmedUp || isWarmingUp) return
        isWarmingUp = true

        val appContext = context.applicationContext

        // Determine best target package (Chromium WebView provider or Chrome)
        val webviewPkg = try {
            WebViewCompat.getCurrentWebViewPackage(appContext)?.packageName
        } catch (_: Throwable) {
            null
        }

        val candidatePackages = listOfNotNull(
            webviewPkg,
            "com.android.chrome",
            "com.google.android.webview",
            "com.chrome.beta",
            "com.chrome.dev",
            "com.chrome.canary"
        ).distinct()

        for (pkg in candidatePackages) {
            try {
                val initialized = CustomTabsClient.connectAndInitialize(appContext, pkg)
                if (initialized) {
                    isWarmedUp = true
                    break
                }
            } catch (_: Throwable) {
            }
        }

        // Also bind a service connection to retain a persistent session for speculative mayLaunchUrl
        for (pkg in candidatePackages) {
            try {
                val connection = object : CustomTabsServiceConnection() {
                    override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
                        customTabsClient = client
                        try {
                            client.warmup(0L)
                            customTabsSession = client.newSession(object : CustomTabsCallback() {})
                            isWarmedUp = true
                        } catch (_: Throwable) {
                        }
                    }

                    override fun onServiceDisconnected(name: ComponentName) {
                        customTabsClient = null
                        customTabsSession = null
                        isWarmedUp = false
                    }
                }

                val bound = CustomTabsClient.bindCustomTabsService(appContext, pkg, connection)
                if (bound) {
                    break
                }
            } catch (_: Throwable) {
            }
        }

        isWarmingUp = false
    }

    /**
     * Speculatively pre-connects and pre-renders an anticipated target URL (e.g. when typing
     * in Omnibox, hovering over Quick Dial shortcuts, or viewing search suggestions).
     * Initiates CustomTabs mayLaunchUrl and pre-resolves DNS in parallel.
     */
    fun speculativeWarmUrl(url: String) {
        val cleanUrl = url.trim()
        if (cleanUrl.isEmpty() || cleanUrl.startsWith("velocity://") || warmedUrls.contains(cleanUrl)) {
            return
        }

        val targetUri = try {
            if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
                Uri.parse(cleanUrl)
            } else if (cleanUrl.contains(".") && !cleanUrl.contains(" ")) {
                Uri.parse("https://$cleanUrl")
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } ?: return

        lastWarmedUrl = cleanUrl
        warmedUrls.add(cleanUrl)

        // 1. Tell Chromium speculative pre-renderer about anticipated navigation
        try {
            customTabsSession?.mayLaunchUrl(targetUri, Bundle.EMPTY, null)
        } catch (_: Throwable) {
        }

        // 2. Pre-resolve DNS records asynchronously in background thread to guarantee 0-second DNS lookup
        val host = targetUri.host
        if (!host.isNullOrBlank()) {
            Thread {
                try {
                    java.net.InetAddress.getAllByName(host)
                } catch (_: Throwable) {
                }
            }.start()
        }
    }

    /**
     * Batch pre-warms a list of likely destinations (e.g. top Quick Dial links).
     */
    fun speculativeWarmUrls(urls: List<String>) {
        if (urls.isEmpty()) return
        val validUris = urls.mapNotNull { raw ->
            val clean = raw.trim()
            if (clean.startsWith("http://") || clean.startsWith("https://")) {
                try { Uri.parse(clean) } catch (_: Exception) { null }
            } else null
        }
        if (validUris.isEmpty()) return

        val primary = validUris.first()
        val others = if (validUris.size > 1) {
            validUris.drop(1).map { Bundle().apply { putParcelable(CustomTabsService.KEY_URL, it) } }
        } else null

        try {
            @Suppress("DEPRECATION")
            customTabsSession?.mayLaunchUrl(primary, Bundle.EMPTY, others)
        } catch (_: Throwable) {
        }

        // DNS pre-resolve
        Thread {
            for (uri in validUris) {
                uri.host?.let { host ->
                    try { java.net.InetAddress.getAllByName(host) } catch (_: Throwable) {}
                }
            }
        }.start()
    }

    /**
     * Builds speculative DNS prefetch and preconnect HTML hints for injection
     * into new or upcoming pages.
     */
    fun buildDnsPrefetchScript(domains: List<String>): String {
        if (domains.isEmpty()) return ""
        val linkTags = domains.filter { it.isNotBlank() }.joinToString("") { domain ->
            "<link rel=\"dns-prefetch\" href=\"//$domain\"><link rel=\"preconnect\" href=\"https://$domain\" crossorigin>"
        }
        return """
            (function() {
                try {
                    if (document.head) {
                        var div = document.createElement('div');
                        div.innerHTML = '$linkTags';
                        while (div.firstChild) {
                            document.head.appendChild(div.firstChild);
                        }
                    }
                } catch(e) {}
            })();
        """.trimIndent()
    }
}
