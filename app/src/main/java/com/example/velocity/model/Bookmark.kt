package com.example.velocity.model

import java.util.UUID

data class Bookmark(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val category: String = "Favorites",
    val dateAdded: Long = System.currentTimeMillis(),
    val isSpeedDial: Boolean = false
)
