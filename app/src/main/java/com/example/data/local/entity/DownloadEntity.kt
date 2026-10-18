package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val filename: String,
    val filePath: String,
    val mimeType: String,
    val totalBytes: Long = -1,
    val downloadedBytes: Long = 0,
    val status: String = "QUEUED", // QUEUED, RUNNING, PAUSED, COMPLETED, FAILED, CANCELED
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val errorMessage: String? = null
)
