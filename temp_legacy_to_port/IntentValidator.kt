package temp_legacy_to_port

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Validates parsed intents from intent:// or custom scheme URLs to prevent
 * intent redirection vulnerabilities and unsafe component launching.
 */
object IntentValidator {

    fun parseSafeIntent(context: Context, uriString: String): Intent? {
        return try {
            val intent = Intent.parseUri(uriString, Intent.URI_INTENT_SCHEME)
            // Prevent launching internal private activities
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.component = null
            intent.selector = null

            // Verify an app can handle this intent
            val resolveInfo = context.packageManager.resolveActivity(intent, 0)
            if (resolveInfo != null) intent else null
        } catch (_: Exception) {
            null
        }
    }
}
