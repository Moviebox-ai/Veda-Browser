package com.example.data.network

import com.example.domain.model.SearchEngine
import com.example.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * Service to fetch query suggestions from search engine APIs.
 */
class SearchSuggestionService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .build()
) {

    suspend fun getSuggestions(query: String, engine: SearchEngine): List<String> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()
        val template = engine.suggestionUrlTemplate ?: return@withContext emptyList()

        try {
            val encoded = URLEncoder.encode(trimmed, StandardCharsets.UTF_8.name())
            val url = template.replace("%s", encoded)

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()

                parseSuggestionsJson(body)
            }
        } catch (e: Exception) {
            AppLogger.d("SearchSuggestionService", "Suggestions fetch error: ${e.message}")
            emptyList()
        }
    }

    private fun parseSuggestionsJson(jsonString: String): List<String> {
        val list = mutableListOf<String>()
        try {
            // Most open search suggestion endpoints return format: ["query", ["item1", "item2", ...]]
            val jsonArray = JSONArray(jsonString)
            if (jsonArray.length() > 1) {
                val suggestionsArray = jsonArray.optJSONArray(1)
                if (suggestionsArray != null) {
                    for (i in 0 until suggestionsArray.length().coerceAtMost(6)) {
                        val suggestion = suggestionsArray.optString(i)
                        if (!suggestion.isNullOrBlank()) {
                            list.add(suggestion)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // In case of non-standard JSON response format
        }
        return list
    }
}
