package com.example.domain.model

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Search engines supported in Velocity Browser.
 */
enum class SearchEngine(
    val id: String,
    val displayName: String,
    val queryUrlTemplate: String,
    val suggestionUrlTemplate: String? = null
) {
    GOOGLE(
        id = "google",
        displayName = "Google",
        queryUrlTemplate = "https://www.google.com/search?q=%s",
        suggestionUrlTemplate = "https://suggestqueries.google.com/complete/search?client=firefox&q=%s"
    ),
    DUCKDUCKGO(
        id = "duckduckgo",
        displayName = "DuckDuckGo",
        queryUrlTemplate = "https://duckduckgo.com/?q=%s",
        suggestionUrlTemplate = "https://duckduckgo.com/ac/?q=%s&type=list"
    ),
    BRAVE(
        id = "brave",
        displayName = "Brave Search",
        queryUrlTemplate = "https://search.brave.com/search?q=%s",
        suggestionUrlTemplate = "https://search.brave.com/api/suggest?q=%s"
    ),
    BING(
        id = "bing",
        displayName = "Bing",
        queryUrlTemplate = "https://www.bing.com/search?q=%s",
        suggestionUrlTemplate = "https://api.bing.com/osjson.aspx?query=%s"
    );

    fun buildSearchUrl(query: String): String {
        val encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.name())
        return queryUrlTemplate.replace("%s", encodedQuery)
    }

    companion object {
        fun fromId(id: String): SearchEngine {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: GOOGLE
        }
    }
}
