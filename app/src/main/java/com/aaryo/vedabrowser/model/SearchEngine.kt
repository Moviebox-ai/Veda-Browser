package com.aaryo.vedabrowser.model

enum class SearchEngine(
    val displayName: String,
    val searchUrl: String,
    val homeUrl: String,
    val iconName: String
) {
    GOOGLE(
        displayName = "Google",
        searchUrl = "https://www.google.com/search?q=%s",
        homeUrl = "https://www.google.com",
        iconName = "ic_google"
    ),
    DUCKDUCKGO(
        displayName = "DuckDuckGo",
        searchUrl = "https://duckduckgo.com/?q=%s",
        homeUrl = "https://duckduckgo.com",
        iconName = "ic_ddg"
    ),
    BING(
        displayName = "Bing",
        searchUrl = "https://www.bing.com/search?q=%s",
        homeUrl = "https://www.bing.com",
        iconName = "ic_bing"
    ),
    BRAVE(
        displayName = "Brave Search",
        searchUrl = "https://search.brave.com/search?q=%s",
        homeUrl = "https://search.brave.com",
        iconName = "ic_brave"
    ),
    ECOSIA(
        displayName = "Ecosia",
        searchUrl = "https://www.ecosia.org/search?q=%s",
        homeUrl = "https://www.ecosia.org",
        iconName = "ic_ecosia"
    );

    fun buildQueryUrl(query: String): String {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        return String.format(searchUrl, encoded)
    }
}
