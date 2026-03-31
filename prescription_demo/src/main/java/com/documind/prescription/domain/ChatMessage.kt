package com.documind.prescription.domain

import androidx.compose.runtime.Immutable
import java.util.UUID

@Immutable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val source: ResponseSource = ResponseSource.NONE
)

enum class ResponseSource {
    NONE,
    OFFLINE,
    ONLINE
}
