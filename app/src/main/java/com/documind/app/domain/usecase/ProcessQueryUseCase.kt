package com.documind.app.domain.usecase

import com.documind.app.data.llm.LocalLLMManager
import com.documind.app.data.processor.TextProcessor
import com.documind.app.domain.model.DocumentContent

class ProcessQueryUseCase(
    private val llmManager: LocalLLMManager,
    private val textProcessor: TextProcessor = TextProcessor()
) {
    
    companion object {
        private const val MAX_CONTEXT_LENGTH = 4000
    }
    
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
        
        val context = textProcessor.buildContextForQuery(
            chunks = document.chunks,
            maxContextLength = MAX_CONTEXT_LENGTH
        )
        
        return llmManager.generateResponse(
            documentContext = context,
            userQuery = query.trim()
        )
    }
    
    fun isReady(): Boolean = llmManager.isReady()
}
