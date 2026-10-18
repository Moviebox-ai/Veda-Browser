package com.example.security

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.util.AppLogger
import java.net.URISyntaxException

/**
 * Validates and safely creates external Intents to prevent intent-redirection and privilege escalation.
 */
object IntentValidator {

    fun handleExternalUri(context: Context, uriString: String): Boolean {
        try {
            val uri = Uri.parse(uriString)
            val scheme = uri.scheme?.lowercase() ?: return false

            if (scheme == "intent") {
                return launchSanitizedIntentUri(context, uriString)
            }

            if (SchemePolicy.isSafeExternalScheme(scheme)) {
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return true
                }
            }
        } catch (e: Exception) {
            AppLogger.w("IntentValidator", "Error handling external URI: ${e.message}")
        }
        return false
    }

    private fun launchSanitizedIntentUri(context: Context, intentUriString: String): Boolean {
        return try {
            val intent = Intent.parseUri(intentUriString, Intent.URI_INTENT_SCHEME)

            // Strip sensitive flags that might leak permissions
            intent.flags = intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION.inv()
            intent.flags = intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION.inv()
            intent.flags = intent.flags or Intent.FLAG_ACTIVITY_NEW_TASK

            // Clear selector and component to prevent targeting non-exported internal components
            intent.selector = null
            intent.component = null

            intent.addCategory(Intent.CATEGORY_BROWSABLE)

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                // If package is specified, fallback to browser or play store fallback
                val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                if (!fallbackUrl.isNullOrEmpty() && SchemePolicy.isWebScheme(Uri.parse(fallbackUrl).scheme)) {
                    val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(fallbackIntent)
                    true
                } else {
                    false
                }
            }
        } catch (e: URISyntaxException) {
            AppLogger.e("IntentValidator", "Invalid intent URI syntax", e)
            false
        } catch (e: Exception) {
            AppLogger.e("IntentValidator", "Failed to launch intent URI", e)
            false
        }
    }
}
