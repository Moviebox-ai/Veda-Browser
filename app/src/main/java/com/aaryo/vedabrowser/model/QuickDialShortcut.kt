package com.aaryo.vedabrowser.model

import java.util.UUID

data class QuickDialShortcut(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val brandType: String = "CUSTOM"
)
