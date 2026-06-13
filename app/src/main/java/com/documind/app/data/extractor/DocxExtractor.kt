package com.documind.app.data.extractor

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.InputStream

class DocxExtractor : StreamContentExtractor {
    
    companion object {
        private const val TAG = "DocxExtractor"
    }
    
    override val sourceType = SourceType.DOCX
    
    override suspend fun extract(source: Any): Result<String> {
        return when (source) {
            is InputStream -> extractFromStream(source)
            else -> Result.failure(IllegalArgumentException("DocxExtractor requires InputStream"))
        }
    }
    
    override suspend fun extractFromStream(inputStream: InputStream): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val document = XWPFDocument(inputStream)
                document.use { doc ->
                    val textBuilder = StringBuilder()
                    for (paragraph in doc.paragraphs) {
                        val text = paragraph.text
                        if (text.isNotBlank()) {
                            textBuilder.append(text).append("\n")
                        }
                    }
                    for (table in doc.tables) {
                        for (row in table.rows) {
                            for (cell in row.tableCells) {
                                val cellText = cell.text
                                if (cellText.isNotBlank()) {
                                    textBuilder.append(cellText).append(" ")
                                }
                            }
                            textBuilder.append("\n")
                        }
                    }
                    val extractedText = textBuilder.toString().trim()
                    if (extractedText.isBlank()) {
                        return@withContext Result.failure(
                            IllegalStateException("Word document contains no readable text.")
                        )
                    }
                    Result.success(extractedText)
                }
            } catch (e: Exception) {
                Log.e(TAG, "DOCX extraction failed", e)
                Result.failure(e)
            }
        }
    }
}
