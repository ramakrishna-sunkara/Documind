package com.documind.prescription.data

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalLLMWrapper(private val context: Context) {
    
    companion object {
        private const val TAG = "LocalLLMWrapper"
        private const val MAX_TOKENS = 4096
        private const val MAX_CONTEXT_CHARS = 8000
        private const val CHARS_PER_TOKEN = 4
    }
    
    private var llmInference: LlmInference? = null
    private var isInitialized = false
    
    suspend fun initialize(modelPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val modelFile = File(modelPath)
            if (!modelFile.exists()) {
                Log.e(TAG, "Model file not found: $modelPath")
                return@withContext Result.failure(IllegalStateException("Model file not found"))
            }
            
            Log.d(TAG, "Initializing LLM from: $modelPath")
            
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(MAX_TOKENS)
                .build()
            
            llmInference = LlmInference.createFromOptions(context, options)
            isInitialized = true
            Log.d(TAG, "LLM initialized successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "LLM initialization failed: ${e.message}", e)
            isInitialized = false
            Result.failure(e)
        }
    }
    
    suspend fun generateResponse(context: String, query: String): Result<String> = 
        withContext(Dispatchers.IO) {
            try {
                val inference = llmInference
                    ?: return@withContext Result.failure(IllegalStateException("LLM not initialized"))
                
                val truncatedContext = if (context.length > MAX_CONTEXT_CHARS) {
                    context.take(MAX_CONTEXT_CHARS)
                } else context
                
                val prompt = """You are a helpful medical AI assistant.

$truncatedContext

Question: $query

Answer:"""
                
                Log.d(TAG, "Prompt length: ${prompt.length} chars")
                val response = inference.generateResponse(prompt)
                Log.d(TAG, "Response length: ${response.length} chars")
                
                Result.success(response.trim())
            } catch (e: Exception) {
                Log.e(TAG, "Generation failed: ${e.message}", e)
                Result.failure(e)
            }
        }
    
    fun isReady(): Boolean = isInitialized
    
    fun close() {
        llmInference?.close()
        llmInference = null
        isInitialized = false
    }
}
