package com.aaryo.vedabrowser

import android.app.Application
import android.content.Context
import java.io.File

class VedaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        cleanCorruptedVariationsSeeds(this)
        com.aaryo.vedabrowser.webview.ChromiumEngineManager.initializeEngine(this)
        com.aaryo.vedabrowser.performance.ChromiumPrewarmer.initialize(this)
        // AdBlocker is initialized by BrowserViewModel after persisted settings are loaded.
        // Initializing it here would lock it to defaults before the user's filter preferences are known.
    }

    companion object {
        /**
         * Cleans up unsigned, corrupted, or stale variations seed files before Chromium's
         * native WebView engine initializes to prevent variations_seed_loader.cc:
         * "Seed missing signature" errors.
         */
        fun cleanCorruptedVariationsSeeds(context: Context) {
            try {
                val appDataDir = context.applicationInfo.dataDir?.let { File(it) }
                    ?: context.filesDir?.parentFile
                if (appDataDir != null && appDataDir.exists()) {
                    val webviewDir = File(appDataDir, "app_webview")
                    if (webviewDir.exists()) {
                        webviewDir.listFiles { file ->
                            file.name.startsWith("variations_seed") || file.name.startsWith("variations_stamp")
                        }?.forEach { file ->
                            try { file.delete() } catch (_: Throwable) {}
                        }
                    }

                    val prefsDir = File(appDataDir, "shared_prefs")
                    if (prefsDir.exists()) {
                        prefsDir.listFiles { file ->
                            file.name.contains("Variations", ignoreCase = true)
                        }?.forEach { file ->
                            try { file.delete() } catch (_: Throwable) {}
                        }
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }
}
