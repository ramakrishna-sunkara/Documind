package com.documind.app.data.processor

class TextProcessor {
    
    companion object {
        const val DEFAULT_CHUNK_SIZE = 1000
        const val DEFAULT_OVERLAP = 100
        const val LARGE_DOCUMENT_THRESHOLD = 10000
    }
    
    fun cleanText(text: String): String {
        return text
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), "")
            .trim()
    }
    
    fun countWords(text: String): Int {
        return text.split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .size
    }
    
    fun chunkText(
        text: String,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        overlap: Int = DEFAULT_OVERLAP
    ): List<String> {
        val cleanedText = cleanText(text)
        
        if (cleanedText.length <= chunkSize) {
            return listOf(cleanedText)
        }
        
        val chunks = mutableListOf<String>()
        var start = 0
        
        while (start < cleanedText.length) {
            val end = minOf(start + chunkSize, cleanedText.length)
            var chunkEnd = end
            
            if (end < cleanedText.length) {
                val lastSpace = cleanedText.lastIndexOf(' ', end)
                if (lastSpace > start) {
                    chunkEnd = lastSpace
                }
            }
            
            chunks.add(cleanedText.substring(start, chunkEnd).trim())
            start = chunkEnd - overlap
            
            if (start < 0) start = 0
            if (chunkEnd >= cleanedText.length) break
        }
        
        return chunks
    }
    
    fun isLargeDocument(text: String): Boolean {
        return countWords(text) > LARGE_DOCUMENT_THRESHOLD
    }
    
    fun buildContextForQuery(chunks: List<String>, maxContextLength: Int = 4000): String {
        val contextBuilder = StringBuilder()
        
        for (chunk in chunks) {
            if (contextBuilder.length + chunk.length > maxContextLength) {
                break
            }
            if (contextBuilder.isNotEmpty()) {
                contextBuilder.append("\n\n")
            }
            contextBuilder.append(chunk)
        }
        
        return contextBuilder.toString()
    }
}
