package com.aaryo.velocity.model

import android.graphics.Bitmap
import java.util.UUID

data class Tab(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Tab",
    val url: String = "velocity://newtab",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isIncognito: Boolean = false,
    val isDesktopMode: Boolean = false,
    val adBlockEnabled: Boolean = true,
    val blockedTrackersCount: Int = 0,
    val isSecure: Boolean = true,
    val favicon: Bitmap? = null,
    val screenshot: Bitmap? = null,
    val activeExtensionsCount: Int = 0,
    val lastVisitedTimestamp: Long = System.currentTimeMillis()
) {
    val isNewTab: Boolean
        get() = url == "velocity://newtab" || url.isEmpty()

    val displayUrl: String
        get() = if (isNewTab) "" else url
}
