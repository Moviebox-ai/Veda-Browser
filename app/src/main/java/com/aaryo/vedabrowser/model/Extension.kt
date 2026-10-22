package com.aaryo.vedabrowser.model

import java.util.UUID

enum class RunAt {
    DOCUMENT_START,
    DOCUMENT_END,
    DOCUMENT_IDLE
}

data class Extension(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val version: String = "1.0",
    val author: String = "Anonymous",
    val description: String = "",
    val enabled: Boolean = true,
    val source: String,
    val matchPatterns: List<String> = listOf("<all_urls>"),
    val excludePatterns: List<String> = emptyList(),
    val runAt: RunAt = RunAt.DOCUMENT_END,
    val grants: List<String> = emptyList(),
    val connectRules: List<String> = emptyList(),
    val installedAt: Long = System.currentTimeMillis(),
    val updateUrl: String? = null,
    val iconUrl: String? = null,
    val isBuiltIn: Boolean = false,
    val allowInIncognito: Boolean = false,
    val siteAllowlist: List<String> = emptyList() // If non-empty, restricts execution solely to these hosts
)
