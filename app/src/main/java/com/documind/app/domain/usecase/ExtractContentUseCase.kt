package com.documind.app.domain.usecase

import com.documind.app.data.extractor.DocxExtractor
import com.documind.app.data.extractor.PdfExtractor
import com.documind.app.data.extractor.SourceType
import com.documind.app.data.extractor.UrlExtractor
import com.documind.app.data.processor.TextProcessor
import com.documind.app.domain.model.DocumentContent
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
        return this.map { rawText ->
            val cleanedText = textProcessor.cleanText(rawText)
            val wordCount = textProcessor.countWords(cleanedText)
            val chunks = textProcessor.chunkText(cleanedText)
            val isLarge = textProcessor.isLargeDocument(cleanedText)
            
            DocumentContent(
                text = cleanedText,
                wordCount = wordCount,
                sourceType = sourceType,
                sourceName = sourceName,
                chunks = chunks,
                isLargeDocument = isLarge
            )
        }
    }
}
