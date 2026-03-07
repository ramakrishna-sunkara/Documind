package com.documind.app.data.extractor

import android.util.Log
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.InputStream

class PdfExtractor : StreamContentExtractor {
    
    companion object {
        private const val TAG = "PdfExtractor"
    }
    
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
                // Buffer the entire stream first - PDFBox needs this
                val bufferedBytes = BufferedInputStream(inputStream).use { buffered ->
                    buffered.readBytes()
                }
                
                if (bufferedBytes.isEmpty()) {
                    return@withContext Result.failure(
                        IllegalStateException("PDF file is empty or could not be read")
                    )
                }
                
                Log.d(TAG, "PDF bytes read: ${bufferedBytes.size}")
                
                // Create a new stream from the buffered bytes
                val byteStream = ByteArrayInputStream(bufferedBytes)
                
                val document = PDDocument.load(byteStream)
                document.use { doc ->
                    val pageCount = doc.numberOfPages
                    Log.d(TAG, "PDF page count: $pageCount")
                    
                    if (pageCount == 0) {
                        return@withContext Result.failure(
                            IllegalStateException("PDF has no pages")
                        )
                    }
                    
                    val stripper = PDFTextStripper().apply {
                        sortByPosition = true
                        startPage = 1
                        endPage = pageCount
                    }
                    
                    val text = stripper.getText(doc)
                    Log.d(TAG, "Extracted text length: ${text.length}")
                    
                    if (text.isBlank()) {
                        // PDF might be scanned/image-based
                        return@withContext Result.failure(
                            IllegalStateException("PDF contains no extractable text. It may be a scanned document or image-based PDF.")
                        )
                    }
                    
                    Result.success(text)
                }
            } catch (e: Exception) {
                Log.e(TAG, "PDF extraction failed", e)
                Result.failure(e)
            }
        }
    }
}
