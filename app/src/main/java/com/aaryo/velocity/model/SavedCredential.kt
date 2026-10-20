package com.aaryo.velocity.model

import java.util.UUID

data class SavedCredential(
    val id: String = UUID.randomUUID().toString(),
    val website: String,
    val username: String,
    val passwordSecret: String,
    val dateCreated: Long = System.currentTimeMillis()
)
