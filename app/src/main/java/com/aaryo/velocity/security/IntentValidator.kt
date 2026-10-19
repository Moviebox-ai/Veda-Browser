package com.aaryo.velocity.security

import android.content.Context
import android.content.Intent

object IntentValidator {

    fun parseSafeIntent(context: Context, uriString: String): Intent? {
        return try {
            val intent = Intent.parseUri(uriString, Intent.URI_INTENT_SCHEME)
            // Strip explicit components to prevent launching unexported internal components
            intent.component = null
            intent.selector = null
            intent.addCategory(Intent.CATEGORY_BROWSABLE)

            // Verify that an activity exists on device to handle this intent
            val resolveInfo = context.packageManager.resolveActivity(intent, 0)
            if (resolveInfo != null) intent else null
        } catch (_: Exception) {
            null
        }
    }
}
