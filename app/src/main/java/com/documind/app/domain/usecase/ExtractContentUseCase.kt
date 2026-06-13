package com.documind.app.domain.usecase

import com.documind.app.data.extractor.DocxExtractor
import com.documind.app.data.extractor.PdfExtractor
import com.documind.app.data.extractor.SourceType
import com.documind.app.data.extractor.UrlExtractor
import com.documind.app.data.processor.TextProcessor
import com.documind.app.domain.model.DocumentContent
import com.documind.app.util.ErrorCategory
import com.documind.app.util.UserFacingErrors
import java.io.InputStream

class ExtractContentUseCase(
    private val pdfExtractor: PdfExtractor = PdfExtractor(),
    private val docxExtractor: DocxExtractor = DocxExtractor(),
    private val urlExtractor: UrlExtractor = UrlExtractor(),
    private val textProcessor: TextProcessor = TextProcessor()
) {
    
    suspend fun extractFromPdf(
        inputStream: InputStream,
        fileName: String
    ): Result<DocumentContent> {
        return pdfExtractor.extractFromStream(inputStream).mapToDocumentContent(
            sourceType = SourceType.PDF,
            sourceName = fileName
        )
    }
    
    suspend fun extractFromDocx(
        inputStream: InputStream,
        fileName: String
    ): Result<DocumentContent> {
        return docxExtractor.extractFromStream(inputStream).mapToDocumentContent(
            sourceType = SourceType.DOCX,
            sourceName = fileName
        )
    }
    
    suspend fun extractFromUrl(url: String): Result<DocumentContent> {
        return urlExtractor.extract(url).mapToDocumentContent(
            sourceType = SourceType.URL,
            sourceName = url.take(50)
        )
    }
    
    fun extractFromText(text: String, label: String = "Pasted Text"): Result<DocumentContent> {
        return if (text.isBlank()) {
            Result.failure(IllegalArgumentException("Text cannot be empty"))
        } else {
            Result.success(text).mapToDocumentContent(
                sourceType = SourceType.TEXT,
                sourceName = label
            )
        }
    }
    
    private fun Result<String>.mapToDocumentContent(
        sourceType: SourceType,
        sourceName: String
    ): Result<DocumentContent> {
        return this.mapCatching { rawText ->
            val cleanedText = textProcessor.cleanText(rawText)
            if (cleanedText.isBlank()) {
                throw IllegalStateException(
                    UserFacingErrors.forMessage(
                        "No readable text was found in this document.",
                        ErrorCategory.EXTRACTION
                    )
                )
            }
            val wordCount = textProcessor.countWords(cleanedText)
            if (wordCount == 0) {
                throw IllegalStateException(
                    UserFacingErrors.forMessage(
                        "No readable text was found in this document.",
                        ErrorCategory.EXTRACTION
                    )
                )
            }
            val chunks = textProcessor.chunkText(cleanedText)
            val indexableChunks = textProcessor.prepareChunksForIndexing(chunks)
            if (indexableChunks.isEmpty()) {
                throw IllegalStateException(
                    UserFacingErrors.forMessage(
                        "No indexable text was found after cleanup.",
                        ErrorCategory.INDEXING
                    )
                )
            }
            val isLarge = textProcessor.isLargeDocument(cleanedText)
            DocumentContent(
                text = cleanedText,
                wordCount = wordCount,
                sourceType = sourceType,
                sourceName = sourceName,
                chunks = indexableChunks,
                isLargeDocument = isLarge
            )
        }
    }
}
