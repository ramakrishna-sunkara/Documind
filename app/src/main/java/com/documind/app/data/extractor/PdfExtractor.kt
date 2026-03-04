package com.documind.app.data.extractor

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class PdfExtractor : StreamContentExtractor {
    
    override val sourceType = SourceType.PDF
    
    override suspend fun extract(source: Any): Result<String> {
        return when (source) {
            is InputStream -> extractFromStream(source)
            else -> Result.failure(IllegalArgumentException("PdfExtractor requires InputStream"))
        }
    }
    
    override suspend fun extractFromStream(inputStream: InputStream): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val document = PDDocument.load(inputStream)
                document.use { doc ->
                    val stripper = PDFTextStripper().apply {
                        sortByPosition = true
                    }
                    val text = stripper.getText(doc)
                    Result.success(text)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
