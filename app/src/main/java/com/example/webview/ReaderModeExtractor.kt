package com.example.webview

import android.webkit.WebView
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlin.coroutines.resume

data class ReaderArticle(
    val title: String,
    val byline: String? = null,
    val content: String,
    val excerpt: String? = null
)

/**
 * Clean, safe Reader Mode extractor.
 * Only runs on-demand when requested by the user.
 */
object ReaderModeExtractor {

    private const val EXTRACTION_SCRIPT = """
        (function() {
            var article = document.querySelector('article') || document.querySelector('main') || document.body;
            var title = document.title || '';
            var h1 = document.querySelector('h1');
            if (h1 && h1.innerText) { title = h1.innerText; }
            
            // Collect paragraphs
            var paragraphs = article.querySelectorAll('p');
            var textParts = [];
            for (var i = 0; i < paragraphs.length; i++) {
                var p = paragraphs[i].innerText.trim();
                if (p.length > 20) {
                    textParts.push(p);
                }
            }
            
            return JSON.stringify({
                title: title,
                content: textParts.join('\n\n')
            });
        })();
    """

    suspend fun extract(webView: WebView): ReaderArticle? = suspendCancellableCoroutine { continuation ->
        webView.evaluateJavascript(EXTRACTION_SCRIPT) { result ->
            try {
                if (result == null || result == "null") {
                    continuation.resume(null)
                    return@evaluateJavascript
                }
                // evaluateJavascript returns a JSON-encoded string
                val unquoted = if (result.startsWith("\"") && result.endsWith("\"")) {
                    JSONObject("{ \"val\": $result }").getString("val")
                } else {
                    result
                }

                val json = JSONObject(unquoted)
                val title = json.optString("title", "")
                val content = json.optString("content", "")

                if (content.isNotBlank()) {
                    continuation.resume(ReaderArticle(title = title, content = content))
                } else {
                    continuation.resume(null)
                }
            } catch (_: Exception) {
                continuation.resume(null)
            }
        }
    }
}
