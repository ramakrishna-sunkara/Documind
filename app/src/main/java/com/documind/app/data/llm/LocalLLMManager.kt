package com.documind.app.data.llm

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalLLMManager(private val context: Context) {
    
    companion object {
        private const val TAG = "LocalLLMManager"
        
        private const val SYSTEM_PROMPT = """You are DocuMind, an AI for document analysis.
Rules:
- Answer ONLY from the document context provided
- Be concise and accurate
- Say "Not found in document" if info is missing"""

        // Gemma 1B supports up to 8192 tokens, use 4096 for safety on mobile
        private const val MAX_TOKENS = 4096
        
        // Reserve tokens for output (approx 512 tokens = 2048 chars)
        private const val MAX_OUTPUT_CHARS = 2048
        
        // Rough estimate: 1 token ≈ 4 characters
        private const val CHARS_PER_TOKEN = 4
        
        // Max input tokens = MAX_TOKENS - reserved output tokens
        // 4096 - 512 = 3584 tokens ≈ 14336 chars
        // But we'll be conservative: 2500 tokens ≈ 10000 chars for input
        const val MAX_CONTEXT_CHARS = 8000
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
                
                Log.d(TAG, "Initializing LLM with MAX_TOKENS=$MAX_TOKENS")
                
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(MAX_TOKENS)
                    .build()
                
                llmInference = LlmInference.createFromOptions(context, options)
                isInitialized = true
                Log.d(TAG, "LLM initialized successfully")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "LLM initialization failed", e)
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
                
                // Truncate context if needed to prevent token overflow
                val truncatedContext = truncateContext(documentContext)
                val prompt = buildPrompt(truncatedContext, userQuery)
                
                Log.d(TAG, "Prompt length: ${prompt.length} chars (~${prompt.length / CHARS_PER_TOKEN} tokens)")
                
                val response = inference.generateResponse(prompt)
                
                Log.d(TAG, "Response length: ${response.length} chars")
                Result.success(response.trim())
            } catch (e: Exception) {
                Log.e(TAG, "Generation failed", e)
                // Provide more helpful error message
                val errorMessage = when {
                    e.message?.contains("too long", ignoreCase = true) == true -> 
                        "Document is too large. Try with a smaller section."
                    e.message?.contains("not initialized", ignoreCase = true) == true ->
                        "AI model not ready. Please restart the app."
                    else -> e.message ?: "Failed to generate response"
                }
                Result.failure(Exception(errorMessage))
            }
        }
    }
    
    private fun truncateContext(context: String): String {
        if (context.length <= MAX_CONTEXT_CHARS) {
            return context
        }
        
        Log.d(TAG, "Truncating context from ${context.length} to $MAX_CONTEXT_CHARS chars")
        
        // Find a good break point (end of sentence or word)
        val truncated = context.take(MAX_CONTEXT_CHARS)
        val lastPeriod = truncated.lastIndexOf('.')
        val lastSpace = truncated.lastIndexOf(' ')
        
        return when {
            lastPeriod > MAX_CONTEXT_CHARS - 500 -> truncated.substring(0, lastPeriod + 1)
            lastSpace > MAX_CONTEXT_CHARS - 200 -> truncated.substring(0, lastSpace)
            else -> truncated
        }
    }
    
    private fun buildPrompt(documentContext: String, userQuery: String): String {
        return """$SYSTEM_PROMPT

Document:
$documentContext

Question: $userQuery

Answer:"""
    }
    
    fun isReady(): Boolean = isInitialized
    
    fun close() {
        llmInference?.close()
        llmInference = null
        isInitialized = false
    }
}
