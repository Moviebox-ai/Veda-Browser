package com.example.domain.model

import android.graphics.Bitmap
import android.os.Bundle
import java.util.UUID

/**
 * Represents a single tab in Velocity Browser.
 */
data class Tab(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "velocity:home",
    val title: String = "New Tab",
    val isPrivate: Boolean = false,
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isSecure: Boolean = false,
    val sslError: String? = null,
    val desktopMode: Boolean = false,
    val blockedRequestsCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis(),
    val favicon: Bitmap? = null,
    val savedState: Bundle? = null
) {
    val isInternal: Boolean
        get() = url.startsWith("velocity:") || url == "about:blank"
}
