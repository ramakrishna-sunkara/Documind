package com.documind.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

object ChatQuickActions {
    val suggestions: List<String> = listOf(
        "Summarize this document",
        "What are the key points?",
        "What is the main topic?"
    )
}

@Composable
fun ChatQuickActionsRow(
    onActionClick: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChatQuickActions.suggestions.forEach { suggestion ->
            ChatSuggestionChip(
                text = suggestion,
                onClick = { if (enabled) onActionClick(suggestion) }
            )
        }
    }
}
