package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "site_settings")
data class SiteSettingEntity(
    @PrimaryKey
    val origin: String, // e.g., "example.com" or "https://example.com"
    val javascriptEnabled: Boolean = true,
    val desktopMode: Boolean = false,
    val textZoom: Int = 100
)
