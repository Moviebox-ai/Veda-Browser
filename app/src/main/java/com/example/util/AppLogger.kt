package com.example.util

import android.util.Log
import com.example.BuildConfig

/**
 * Privacy-preserving structured logger for Velocity Browser.
 * In release builds, query parameters and sensitive URL fragments are stripped.
 */
object AppLogger {
    private const val TAG = "VelocityBrowser"

    fun d(tag: String = TAG, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, sanitize(message))
        }
    }

    fun i(tag: String = TAG, message: String) {
        Log.i(tag, sanitize(message))
    }

    fun w(tag: String = TAG, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(tag, sanitize(message), throwable)
        } else {
            Log.w(tag, sanitize(message))
        }
    }

    fun e(tag: String = TAG, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, sanitize(message), throwable)
        } else {
            Log.e(tag, sanitize(message))
        }
    }

    private fun sanitize(message: String): String {
        if (BuildConfig.DEBUG) return message
        // Redact query parameters in release mode for user privacy
        return message.replace(Regex("([?&][a-zA-Z0-9_.-]+)=([^&\\s]+)"), "$1=[REDACTED]")
    }
}
