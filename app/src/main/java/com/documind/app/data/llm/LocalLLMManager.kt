package com.documind.app.data.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalLLMManager(private val context: Context) {
    
    companion object {
        private const val SYSTEM_PROMPT = """You are DocuMind, a helpful AI assistant for document analysis. 
Your responses must be:
1. Based ONLY on the provided document context
2. Concise and to the point
3. If information is not found in the context, say "Information not found in the document"
4. No speculation or external knowledge

Always prioritize accuracy over completeness."""

        private const val MAX_TOKENS = 1024
    }
    
    private var llmInference: LlmInference? = null
    private var isInitialized = false
    
    suspend fun initialize(modelPath: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val modelFile = File(modelPath)
                if (!modelFile.exists()) {
                    return@withContext Result.failure(
                        IllegalStateException("Model file not found at: $modelPath")
                    )
                }
                
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(MAX_TOKENS)
                    .build()
                
                llmInference = LlmInference.createFromOptions(context, options)
                isInitialized = true
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun generateResponse(documentContext: String, userQuery: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val inference = llmInference
                    ?: return@withContext Result.failure(
                        IllegalStateException("LLM not initialized")
                    )
                
                val prompt = buildPrompt(documentContext, userQuery)
                val response = inference.generateResponse(prompt)
                
                Result.success(response.trim())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    private fun buildPrompt(documentContext: String, userQuery: String): String {
        return """$SYSTEM_PROMPT

--- DOCUMENT CONTEXT START ---
$documentContext
--- DOCUMENT CONTEXT END ---

User Question: $userQuery

Answer:"""
    }
    
    fun isReady(): Boolean = isInitialized
    
    fun close() {
        llmInference?.close()
        llmInference = null
        isInitialized = false
    }
}
