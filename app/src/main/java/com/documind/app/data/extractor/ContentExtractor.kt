package com.documind.app.data.extractor

import java.io.InputStream

enum class SourceType {
    PDF, DOCX, URL, TEXT
}

interface ContentExtractor {
    val sourceType: SourceType
    suspend fun extract(source: Any): Result<String>
}

interface StreamContentExtractor : ContentExtractor {
    suspend fun extractFromStream(inputStream: InputStream): Result<String>
}
