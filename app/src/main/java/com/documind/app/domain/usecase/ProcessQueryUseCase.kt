package com.documind.app.domain.usecase

import com.documind.app.data.llm.LocalLLMManager
import com.documind.app.data.processor.TextProcessor
import com.documind.app.domain.model.DocumentContent

class ProcessQueryUseCase(
    private val llmManager: LocalLLMManager,
    private val textProcessor: TextProcessor = TextProcessor()
) {
    
    suspend fun process(
        document: DocumentContent,
        query: String
    ): Result<String> {
        if (!llmManager.isReady()) {
            return Result.failure(IllegalStateException("AI model is not ready"))
        }
        
        if (query.isBlank()) {
            return Result.failure(IllegalArgumentException("Query cannot be empty"))
        }
        
        // Use the max context from LocalLLMManager to stay within token limits
        val context = textProcessor.buildContextForQuery(
            chunks = document.chunks,
            maxContextLength = LocalLLMManager.MAX_CONTEXT_CHARS
        )
        
        return llmManager.generateResponse(
            documentContext = context,
            userQuery = query.trim()
        )
    }
    
    fun isReady(): Boolean = llmManager.isReady()
}
