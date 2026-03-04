package com.documind.app.domain.model

import com.documind.app.data.extractor.SourceType

data class DocumentContent(
    val text: String,
    val wordCount: Int,
    val sourceType: SourceType,
    val sourceName: String,
    val chunks: List<String> = emptyList(),
    val isLargeDocument: Boolean = false
)
