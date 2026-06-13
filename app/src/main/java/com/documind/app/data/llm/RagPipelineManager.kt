package com.documind.app.data.llm

import android.content.Context
import android.util.Log
import com.documind.app.data.analytics.CrashAnalytics
import com.documind.app.data.processor.TextProcessor
import com.google.ai.edge.localagents.rag.chains.ChainConfig
import com.google.ai.edge.localagents.rag.chains.RetrievalAndInferenceChain
import com.google.ai.edge.localagents.rag.memory.DefaultSemanticTextMemory
import com.google.ai.edge.localagents.rag.memory.SqliteVectorStore
import com.google.ai.edge.localagents.rag.models.GeckoEmbeddingModel
import com.google.ai.edge.localagents.rag.models.MediaPipeLlmBackend
import com.google.ai.edge.localagents.rag.prompt.PromptBuilder
import com.google.ai.edge.localagents.rag.retrieval.RetrievalConfig
import com.google.ai.edge.localagents.rag.retrieval.RetrievalRequest
import com.google.common.collect.ImmutableList
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.Optional

class RagPipelineManager(private val context: Context) {

    companion object {
        private const val TAG = "RagPipelineManager"
        private const val EMBEDDING_DIMENSION = 768
        private const val MAX_TOKENS = 2048
        private const val TEMPERATURE = 0.7f
        private const val TOP_K = 40
        private const val TOP_P = 0.95f
        private const val RAG_DB_FILE_NAME = "documind_rag.db"
        private const val INDEXING_TIMEOUT_MS = 120_000L
        private const val QA_PROMPT_TEMPLATE = """
Answer using ONLY the context below. Be concise and factual.
If the answer is not in the context, say "Not found in document".

Context: {0}

Question: {1}

Answer:"""
    }

    private var retrievalChain: RetrievalAndInferenceChain? = null
    private var semanticMemory: DefaultSemanticTextMemory? = null
    private var llmBackend: MediaPipeLlmBackend? = null
    private var geckoModelPath: String? = null
    private var tokenizerPath: String? = null
    private var isInitialized = false
    private var isDocumentIndexed = false
    private val textProcessor = TextProcessor()

    suspend fun initialize(
        llmModelPath: String,
        geckoModelPathValue: String,
        tokenizerPathValue: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                CrashAnalytics.recordDeviceMemory(context)
                CrashAnalytics.logRagPhase("initialize_start")
                closeInternal(deleteDatabase = true)
                geckoModelPath = geckoModelPathValue
                tokenizerPath = tokenizerPathValue
                val llmOptions = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(llmModelPath)
                    .setMaxTokens(MAX_TOKENS)
                    .setPreferredBackend(LlmInference.Backend.CPU)
                    .build()
                val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setTemperature(TEMPERATURE)
                    .setTopK(TOP_K)
                    .setTopP(TOP_P)
                    .build()
                val backend = MediaPipeLlmBackend(context, llmOptions, sessionOptions)
                CrashAnalytics.logRagPhase("llm_backend_creating")
                backend.initialize().get()
                llmBackend = backend
                isInitialized = true
                isDocumentIndexed = false
                CrashAnalytics.logRagPhase("llm_backend_ready")
                Log.d(TAG, "LLM backend initialized (RAG memory loads on first document)")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "RAG initialization failed", e)
                CrashAnalytics.recordNonFatal(e, "rag_initialize")
                closeInternal(deleteDatabase = true)
                Result.failure(
                    Exception("RAG pipeline failed to initialize: ${e.message ?: "Unknown error"}")
                )
            }
        }
    }

    suspend fun indexChunks(chunks: List<String>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (!isInitialized) {
                    return@withContext Result.failure(IllegalStateException("RAG not initialized"))
                }
                createFreshMemoryChain()
                val memory = semanticMemory
                    ?: return@withContext Result.failure(IllegalStateException("RAG memory unavailable"))
                val validChunks = textProcessor.prepareChunksForIndexing(chunks)
                if (validChunks.isEmpty()) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Document has no indexable text after cleanup")
                    )
                }
                Log.d(TAG, "Indexing ${validChunks.size} chunks")
                CrashAnalytics.logRagPhase("indexing", "chunks=${validChunks.size}")
                withTimeout(INDEXING_TIMEOUT_MS) {
                    indexChunksWithFallback(memory, validChunks)
                }
                isDocumentIndexed = true
                CrashAnalytics.logRagPhase("indexing_complete")
                Log.d(TAG, "Document indexed successfully")
                Result.success(Unit)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Chunk indexing timed out", e)
                CrashAnalytics.recordNonFatal(e, "rag_indexing_timeout")
                isDocumentIndexed = false
                Result.failure(Exception("Document indexing timed out. Tap Retry to try again."))
            } catch (e: Exception) {
                Log.e(TAG, "Chunk indexing failed", e)
                CrashAnalytics.recordNonFatal(e, "rag_indexing")
                isDocumentIndexed = false
                Result.failure(Exception("Failed to index document: ${e.message ?: "Unknown error"}"))
            }
        }
    }

    private fun indexChunksWithFallback(
        memory: DefaultSemanticTextMemory,
        validChunks: List<String>
    ) {
        try {
            memory.recordBatchedMemoryItems(ImmutableList.copyOf(validChunks)).get()
        } catch (batchError: Exception) {
            Log.w(TAG, "Batch indexing failed, retrying chunk-by-chunk", batchError)
            validChunks.forEachIndexed { index, chunk ->
                try {
                    memory.recordBatchedMemoryItems(ImmutableList.of(chunk)).get()
                } catch (chunkError: Exception) {
                    Log.e(TAG, "Failed to index chunk $index (${chunk.length} chars)", chunkError)
                    throw chunkError
                }
            }
        }
    }

    suspend fun generateResponse(query: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val chain = retrievalChain
                    ?: return@withContext Result.failure(IllegalStateException("RAG not initialized"))
                if (!isDocumentIndexed) {
                    return@withContext Result.failure(IllegalStateException("Document is not indexed yet"))
                }
                val topK = resolveTopK(query)
                val request = RetrievalRequest.create(
                    query.trim(),
                    RetrievalConfig.create(
                        topK,
                        0.0f,
                        RetrievalConfig.TaskType.QUESTION_ANSWERING
                    )
                )
                val response = chain.invoke(request).get()
                val text = response.text.trim()
                if (ResponseQualityChecker.isDegenerateResponse(text)) {
                    Log.w(TAG, "Degenerate RAG response detected")
                    return@withContext Result.success(ResponseQualityChecker.degenerateResponseMessage())
                }
                Result.success(text)
            } catch (e: Exception) {
                Log.e(TAG, "RAG generation failed", e)
                Result.failure(Exception(e.message ?: "Failed to generate response"))
            }
        }
    }

    private fun createFreshMemoryChain() {
        val backend = llmBackend
            ?: throw IllegalStateException("LLM backend not initialized")
        val geckoPath = geckoModelPath
            ?: throw IllegalStateException("Gecko model path not set")
        val tokenizer = tokenizerPath
            ?: throw IllegalStateException("Tokenizer path not set")
        val dbFile = File(getDatabasePath())
        if (dbFile.exists()) {
            dbFile.delete()
        }
        val embedder = GeckoEmbeddingModel(geckoPath, Optional.of(tokenizer), false)
        val vectorStore = SqliteVectorStore(EMBEDDING_DIMENSION, getDatabasePath())
        val memory = DefaultSemanticTextMemory(vectorStore, embedder)
        semanticMemory = memory
        retrievalChain = RetrievalAndInferenceChain(
            ChainConfig.create(
                backend,
                PromptBuilder(QA_PROMPT_TEMPLATE.trim()),
                memory
            )
        )
        isDocumentIndexed = false
    }

    private fun resolveTopK(query: String): Int {
        val normalizedQuery = query.trim().lowercase()
        return if (
            normalizedQuery.contains("summar") ||
            normalizedQuery.contains("overview") ||
            normalizedQuery.contains("key points") ||
            normalizedQuery.contains("main points")
        ) {
            6
        } else {
            4
        }
    }

    private fun getDatabasePath(): String {
        return File(context.filesDir, RAG_DB_FILE_NAME).absolutePath
    }

    fun isReady(): Boolean = isInitialized

    fun hasIndexedDocument(): Boolean = isDocumentIndexed

    fun close() {
        closeInternal(deleteDatabase = true)
    }

    private fun closeInternal(deleteDatabase: Boolean) {
        try {
            llmBackend?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing LLM backend", e)
        }
        llmBackend = null
        semanticMemory = null
        retrievalChain = null
        geckoModelPath = null
        tokenizerPath = null
        isInitialized = false
        isDocumentIndexed = false
        if (deleteDatabase) {
            val dbFile = File(getDatabasePath())
            if (dbFile.exists()) {
                dbFile.delete()
            }
        }
    }
}
