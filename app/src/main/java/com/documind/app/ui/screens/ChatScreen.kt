package com.documind.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.documind.app.domain.model.ChatMessage
import com.documind.app.domain.model.DocumentContent
import com.documind.app.domain.model.ExtractionState
import com.documind.app.domain.model.QueryState
import com.documind.app.ui.components.ChatQuickActions
import com.documind.app.ui.components.ChatQuickActionsRow
import com.documind.app.ui.components.ChatSuggestionChip
import com.documind.app.ui.components.DocumentStatusBar
import com.documind.app.ui.components.IndexingOverlay
import com.documind.app.ui.components.MessageBubble
import com.documind.app.ui.theme.DocumindDimens
import com.documind.app.ui.theme.DocumindGradients
import com.documind.app.ui.theme.DocumindScreenBackground
import com.documind.app.ui.theme.OnDeviceBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    document: DocumentContent,
    messages: List<ChatMessage>,
    queryState: QueryState,
    extractionState: ExtractionState,
    onSendQuery: (String) -> Unit,
    onRetryIndexing: () -> Unit,
    onRetryQuery: () -> Unit,
    onClearDocument: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val isProcessing: Boolean = queryState is QueryState.Processing
    val isDocumentIndexing: Boolean = extractionState is ExtractionState.Indexing
    val indexingErrorMessage: String? = (extractionState as? ExtractionState.Error)?.message
    val queryErrorMessage: String? = (queryState as? QueryState.Error)?.message
    val canSendQuery: Boolean = !isProcessing && !isDocumentIndexing && indexingErrorMessage == null
    val emptyStateTitle: String = when {
        isDocumentIndexing -> "Preparing Document"
        indexingErrorMessage != null -> "Document Not Ready"
        else -> "Ready to Answer"
    }
    val emptyStateSubtitle: String = when {
        isDocumentIndexing ->
            "Setting up on-device AI for this document.\nYou can ask questions once preparation finishes."
        indexingErrorMessage != null ->
            "Fix the issue above, then tap Retry indexing."
        else ->
            "Ask anything about your document.\nAnswers are generated on your device."
    }
    val sendQuery: (String) -> Unit = { query ->
        if (query.isNotBlank() && canSendQuery) {
            onSendQuery(query)
            inputText = ""
        }
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    BackHandler(onBack = onClearDocument)
    DocumindScreenBackground(modifier = modifier) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    navigationIcon = {
                        IconButton(onClick = onClearDocument) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = document.sourceName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = OnDeviceBadge
                                )
                                Text(
                                    text = "Offline · On-Device",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = OnDeviceBadge
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding()
            ) {
                DocumentStatusBar(
                    document = document,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                indexingErrorMessage?.let { errorMessage ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Could not prepare this document for AI",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Button(onClick = onRetryIndexing) {
                                Text(text = "Retry indexing")
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (messages.isEmpty()) {
                        EmptyStateContent(
                            title = emptyStateTitle,
                            subtitle = emptyStateSubtitle,
                            onSuggestionClick = sendQuery,
                            enabled = canSendQuery
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = messages,
                                key = { it.id }
                            ) { message ->
                                MessageBubble(message = message)
                            }
                        }
                    }
                    if (isDocumentIndexing) {
                        IndexingOverlay(
                            title = "Preparing on-device AI",
                            subtitle = "Indexing your document for private, offline Q&A. This usually takes a few seconds."
                        )
                    }
                }
                queryErrorMessage?.let {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = it,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Button(onClick = onRetryQuery) {
                                Text(text = "Retry")
                            }
                        }
                    }
                }
                ChatInputSection(
                    inputText = inputText,
                    onInputChange = { inputText = it },
                    isProcessing = isProcessing || isDocumentIndexing,
                    onSend = { sendQuery(inputText) },
                    onQuickAction = sendQuery
                )
            }
        }
    }
}

@Composable
private fun ChatInputSection(
    inputText: String,
    onInputChange: (String) -> Unit,
    isProcessing: Boolean,
    onSend: () -> Unit,
    onQuickAction: (String) -> Unit
) {
    val canSend: Boolean = inputText.isNotBlank() && !isProcessing
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column {
            ChatQuickActionsRow(
                onActionClick = onQuickAction,
                enabled = !isProcessing
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "Ask about the document...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    enabled = !isProcessing,
                    maxLines = 4,
                    shape = RoundedCornerShape(DocumindDimens.InputRadius),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                GradientSendButton(
                    enabled = canSend,
                    onClick = onSend
                )
            }
        }
    }
}

@Composable
private fun GradientSendButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                brush = if (enabled) {
                    DocumindGradients.brand()
                } else {
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send",
            tint = if (enabled) {
                Color.White
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            }
        )
    }
}

@Composable
private fun EmptyStateContent(
    title: String,
    subtitle: String,
    onSuggestionClick: (String) -> Unit,
    enabled: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(brush = DocumindGradients.brand()),
            color = Color.Transparent,
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.QuestionAnswer,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = "Try asking:",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ChatQuickActions.suggestions.forEach { suggestion ->
                ChatSuggestionChip(
                    text = suggestion,
                    onClick = {
                        if (enabled) {
                            onSuggestionClick(suggestion)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
