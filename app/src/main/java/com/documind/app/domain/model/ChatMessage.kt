package com.documind.app.domain.model

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val isInfo: Boolean = false
)
