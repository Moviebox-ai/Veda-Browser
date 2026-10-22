package com.aaryo.vedabrowser.extension

import java.util.regex.Pattern

object MatchPatternMatcher {

    fun isUrlMatched(
        includePatterns: List<String>,
        excludePatterns: List<String>,
        targetUrl: String?
    ): Boolean {
        if (targetUrl.isNullOrBlank()) return false
        if (targetUrl.startsWith("about:") || targetUrl.startsWith("velocity:") || targetUrl.startsWith("chrome:")) {
            return false
        }

        // 1. If any exclude pattern matches, deny immediately
        for (exclude in excludePatterns) {
            if (matches(exclude, targetUrl)) {
                return false
            }
        }

        // 2. If no include patterns specified, default to false
        if (includePatterns.isEmpty()) return false

        // 3. Match against includes
        for (include in includePatterns) {
            if (matches(include, targetUrl)) {
                return true
            }
        }

        return false
    }

    fun matches(pattern: String, url: String): Boolean {
        val trimmed = pattern.trim()
        if (trimmed == "<all_urls>") {
            return url.startsWith("http://") || url.startsWith("https://")
        }

        val regex = patternToRegex(trimmed) ?: return false
        return regex.matcher(url).matches()
    }

    fun patternToRegex(patternString: String): Pattern? {
        val p = patternString.trim()
        if (p == "<all_urls>") {
            return Pattern.compile("^https?://.*$", Pattern.CASE_INSENSITIVE)
        }

        val schemeSeparator = p.indexOf("://")
        if (schemeSeparator == -1) {
            // Check if it's a simple wildcard pattern (e.g. *google.com*)
            val regex = p.replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", "\\?")
            return try {
                Pattern.compile("^.*$regex.*$", Pattern.CASE_INSENSITIVE)
            } catch (_: Exception) {
                null
            }
        }

        val scheme = p.substring(0, schemeSeparator)
        val rest = p.substring(schemeSeparator + 3)

        val pathSeparator = rest.indexOf('/')
        val host: String
        val path: String
        if (pathSeparator != -1) {
            host = rest.substring(0, pathSeparator)
            path = rest.substring(pathSeparator)
        } else {
            host = rest
            path = "/*"
        }

        val schemeRegex = when (scheme) {
            "*" -> "https?"
            "http", "https" -> scheme
            else -> Pattern.quote(scheme)
        }

        val hostRegex = when {
            host == "*" -> "[^/]+"
            host.startsWith("*.") -> {
                val base = Pattern.quote(host.substring(2))
                "([a-zA-Z0-9.-]+\\.)?$base"
            }
            else -> Pattern.quote(host)
        }

        val pathRegex = buildPathRegex(path)

        val fullRegex = "^$schemeRegex://$hostRegex$pathRegex$"
        return try {
            Pattern.compile(fullRegex, Pattern.CASE_INSENSITIVE)
        } catch (_: Exception) {
            null
        }
    }

    private fun buildPathRegex(path: String): String {
        val sb = StringBuilder()
        for (char in path) {
            when (char) {
                '*' -> sb.append(".*")
                '?' -> sb.append(".")
                '.', '+', '(', ')', '[', ']', '{', '}', '^', '$', '|' -> {
                    sb.append('\\').append(char)
                }
                else -> sb.append(char)
            }
        }
        return sb.toString()
    }
}
