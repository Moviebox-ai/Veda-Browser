package com.aaryo.vedabrowser.viewmodel

import android.net.Uri

/** Shared URL helpers for browser UI and view-model code. */
object UriHelper {
    fun extractDomain(url: String): String? = try {
        Uri.parse(url).host?.removePrefix("www.")
    } catch (_: Exception) {
        null
    }
}
