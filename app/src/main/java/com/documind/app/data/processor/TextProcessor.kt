package com.documind.app.data.processor

class TextProcessor {
    
    companion object {
        const val DEFAULT_CHUNK_SIZE = 600
        const val DEFAULT_OVERLAP = 100
        const val LARGE_DOCUMENT_THRESHOLD = 10000
        const val MAX_EMBEDDING_CHUNK_CHARS = 480
        const val MIN_EMBEDDING_CHUNK_CHARS = 20
    }
    
    fun cleanText(text: String): String {
        return text
            .replace('\u00A0', ' ')
            .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), "")
            .lines()
            .map { line -> line.trim().replace(Regex("\\s+"), " ") }
            .filter { line -> line.isNotBlank() }
            .joinToString("\n")
            .trim()
    }

    fun sanitizeForEmbedding(text: String): String {
        return text
            .replace('\u00A0', ' ')
            .replace(Regex("[\\p{C}&&[^\\n\\t]]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(MAX_EMBEDDING_CHUNK_CHARS)
    }

    fun prepareChunksForIndexing(chunks: List<String>): List<String> {
        val sanitizedChunks = chunks
            .map { chunk -> sanitizeForEmbedding(chunk) }
            .filter { chunk -> chunk.isNotBlank() }
            .distinct()
        if (sanitizedChunks.isEmpty()) {
            return emptyList()
        }
        val substantialChunks = sanitizedChunks.filter { chunk ->
            chunk.length >= MIN_EMBEDDING_CHUNK_CHARS
        }
        return if (substantialChunks.isNotEmpty()) {
            substantialChunks
        } else {
            listOf(sanitizedChunks.maxBy { chunk -> chunk.length })
        }
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
        var previousStart = -1
        
        while (start < cleanedText.length) {
            if (start <= previousStart) {
                break
            }
            previousStart = start
            val end = minOf(start + chunkSize, cleanedText.length)
            var chunkEnd = end
            
            if (end < cleanedText.length) {
                val lastSpace = cleanedText.lastIndexOf(' ', end)
                if (lastSpace > start) {
                    chunkEnd = lastSpace
                }
            }
            
            val chunk = cleanedText.substring(start, chunkEnd).trim()
            if (chunk.isNotBlank()) {
                chunks.add(chunk)
            }
            if (chunkEnd >= cleanedText.length) {
                break
            }
            val nextStart = chunkEnd - overlap
            start = if (nextStart > start) nextStart else chunkEnd
        }
        
        return chunks.ifEmpty { listOf(cleanedText) }
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

    fun isSummaryQuery(query: String): Boolean {
        val normalizedQuery = query.trim().lowercase()
        return normalizedQuery.contains("summar") ||
            normalizedQuery.contains("overview") ||
            normalizedQuery.contains("key points") ||
            normalizedQuery.contains("main points")
    }

    fun buildSummaryContext(text: String, maxContextLength: Int): String {
        val keyFacts = extractKeyFacts(text)
        val reservedForFacts = keyFacts.length + if (keyFacts.isNotEmpty()) 2 else 0
        val remainingLength = maxContextLength - reservedForFacts
        if (remainingLength <= 0) {
            return keyFacts
        }
        val headLength = remainingLength / 2
        val tailLength = remainingLength - headLength
        val head = text.take(headLength).trim()
        val tail = text.takeLast(tailLength).trim()
        return buildString {
            if (keyFacts.isNotEmpty()) {
                append(keyFacts)
                append("\n\n")
            }
            append(head)
            if (head.isNotEmpty() && tail.isNotEmpty()) {
                append("\n\n...\n\n")
            }
            append(tail)
        }.trim()
    }

    fun extractKeyFactsMap(text: String): Map<String, String> {
        val factPatterns = listOf(
            "Invoice No" to Regex("Invoice\\s*No\\s*:\\s*(\\S+)", RegexOption.IGNORE_CASE),
            "Invoice Date" to Regex("Invoice\\s*Date\\s*:?\\s*(\\S+)", RegexOption.IGNORE_CASE),
            "Grand Total" to Regex("Grand\\s*Total\\s*:\\s*([\\d,.]+)", RegexOption.IGNORE_CASE),
            "Gross Amount" to Regex("Gross\\s*Amount\\s*:\\s*([\\d,.]+)", RegexOption.IGNORE_CASE),
            "Vehicle Regn. No" to Regex("Vehicle\\s*Regn\\.?\\s*No\\s*:\\s*(\\S+)", RegexOption.IGNORE_CASE),
            "Model" to Regex("Model\\s*:\\s*(\\S+)", RegexOption.IGNORE_CASE),
            "Customer" to Regex("(?:^|\\n)(Mr\\.\\s+[^\\n]+)", RegexOption.IGNORE_CASE),
            "Service Type" to Regex("Service\\s*Request\\s*Type\\s*:\\s*(.+)", RegexOption.IGNORE_CASE)
        )
        return factPatterns.mapNotNull { (label, pattern) ->
            pattern.find(text)?.let { match ->
                val value = match.groupValues.last().trim()
                if (value.isNotBlank()) label to value else null
            }
        }.toMap()
    }

    fun extractKeyFacts(text: String): String {
        val facts = extractKeyFactsMap(text)
        if (facts.isEmpty()) {
            return ""
        }
        return "Key facts:\n" + facts.entries.joinToString("\n") { (label, value) -> "$label: $value" }
    }
}
