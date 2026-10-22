package com.aaryo.vedabrowser.extension

import com.aaryo.vedabrowser.model.Extension
import com.aaryo.vedabrowser.model.RunAt
import java.net.URI

object UserscriptParser {

    const val MAX_SCRIPT_SIZE_BYTES = 1024 * 1024 // 1 MB limit

    class UserscriptParseException(message: String) : Exception(message)

    fun parse(code: String): Extension {
        val bytes = code.toByteArray(Charsets.UTF_8)
        if (bytes.size > MAX_SCRIPT_SIZE_BYTES) {
            throw UserscriptParseException("Script exceeds maximum allowed size of 1 MB (${bytes.size} bytes)")
        }

        val startIdx = code.indexOf("// ==UserScript==")
        val endIdx = code.indexOf("// ==/UserScript==")

        var name = "Untitled Script"
        var version = "1.0"
        var author = "Anonymous"
        var description = ""
        val matchPatterns = mutableListOf<String>()
        val excludePatterns = mutableListOf<String>()
        var runAt = RunAt.DOCUMENT_END
        val grants = mutableListOf<String>()
        val connectRules = mutableListOf<String>()
        val requires = mutableListOf<String>()
        var updateUrl: String? = null
        var iconUrl: String? = null

        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            val headerBlock = code.substring(startIdx + "// ==UserScript==".length, endIdx)
            val lines = headerBlock.lines()

            for (line in lines) {
                val trimmed = line.trim().removePrefix("//").trim()
                if (!trimmed.startsWith("@")) continue

                val parts = trimmed.substring(1).split(Regex("\\s+"), limit = 2)
                val tag = parts[0].lowercase()
                val value = if (parts.size > 1) parts[1].trim() else ""

                when (tag) {
                    "name" -> if (value.isNotEmpty()) name = value
                    "version" -> if (value.isNotEmpty()) version = value
                    "author" -> if (value.isNotEmpty()) author = value
                    "description" -> if (value.isNotEmpty()) description = value
                    "match", "include" -> if (value.isNotEmpty()) matchPatterns.add(value)
                    "exclude" -> if (value.isNotEmpty()) excludePatterns.add(value)
                    "run-at" -> {
                        runAt = when (value.lowercase()) {
                            "document-start" -> RunAt.DOCUMENT_START
                            "document-idle" -> RunAt.DOCUMENT_IDLE
                            else -> RunAt.DOCUMENT_END
                        }
                    }
                    "grant" -> if (value.isNotEmpty() && value != "none") grants.add(value)
                    "connect" -> if (value.isNotEmpty()) connectRules.add(value)
                    "require" -> {
                        if (value.isNotEmpty()) {
                            val sanitized = sanitizeRequireUrl(value)
                            if (sanitized != null) requires.add(sanitized)
                        }
                    }
                    "icon" -> if (value.isNotEmpty() && value.startsWith("https://")) iconUrl = value
                    "updateurl", "downloadurl" -> if (value.isNotEmpty() && value.startsWith("https://")) updateUrl = value
                }
            }
        }

        if (matchPatterns.isEmpty()) {
            matchPatterns.add("<all_urls>")
        }

        return Extension(
            name = name,
            version = version,
            author = author,
            description = description,
            source = code,
            matchPatterns = matchPatterns,
            excludePatterns = excludePatterns,
            runAt = runAt,
            grants = grants,
            connectRules = connectRules,
            updateUrl = updateUrl,
            iconUrl = iconUrl,
            isBuiltIn = false,
            enabled = true,
            allowInIncognito = false
        )
    }

    private fun sanitizeRequireUrl(url: String): String? {
        return try {
            val uri = URI(url)
            if (uri.scheme?.lowercase() != "https") return null
            val host = uri.host?.lowercase() ?: return null

            // Reject internal localhost or private IP addresses
            if (host == "localhost" || host.startsWith("127.") || host.startsWith("192.168.") || host.startsWith("10.")) {
                return null
            }
            url
        } catch (_: Exception) {
            null
        }
    }
}
