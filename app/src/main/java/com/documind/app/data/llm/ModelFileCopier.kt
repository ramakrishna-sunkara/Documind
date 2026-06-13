package com.documind.app.data.llm

import android.content.Context
import android.util.Log
import com.documind.app.data.analytics.CrashAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.documind.app.util.ErrorCategory
import com.documind.app.util.UserFacingErrors
import java.io.File

data class LocalModelPaths(
    val llmPath: String,
    val geckoPath: String,
    val tokenizerPath: String
)

class ModelFileCopier(private val context: Context) {

    companion object {
        private const val TAG = "ModelFileCopier"
        private const val MODELS_DIR = "models"
    }

    private var cachedPaths: LocalModelPaths? = null

    suspend fun ensureLocalCopies(
        llmSourcePath: String,
        geckoSourcePath: String,
        tokenizerSourcePath: String
    ): Result<LocalModelPaths> {
        return withContext(Dispatchers.IO) {
            try {
                cachedPaths?.let { cached ->
                    if (cachedPathsAreValid(cached)) {
                        CrashAnalytics.logRagPhase("model_copy_cached")
                        return@withContext Result.success(cached)
                    }
                }
                CrashAnalytics.logRagPhase("model_copy_start")
                CrashAnalytics.recordDeviceMemory(context)
                val modelsDir = File(context.filesDir, MODELS_DIR)
                if (!modelsDir.exists()) {
                    modelsDir.mkdirs()
                }
                val llmDest = File(modelsDir, ModelAssetConstants.LLM_FILE_NAME)
                val geckoDest = File(modelsDir, ModelAssetConstants.GECKO_MODEL_FILE_NAME)
                val tokenizerDest = File(modelsDir, ModelAssetConstants.TOKENIZER_FILE_NAME)
                copyIfNeeded(File(llmSourcePath), llmDest, ModelAssetConstants.MIN_LLM_SIZE_BYTES)
                copyIfNeeded(File(geckoSourcePath), geckoDest, ModelAssetConstants.MIN_GECKO_SIZE_BYTES)
                copyIfNeeded(File(tokenizerSourcePath), tokenizerDest, 1L)
                val paths = LocalModelPaths(
                    llmPath = llmDest.absolutePath,
                    geckoPath = geckoDest.absolutePath,
                    tokenizerPath = tokenizerDest.absolutePath
                )
                cachedPaths = paths
                CrashAnalytics.logRagPhase("model_copy_complete")
                Log.d(TAG, "Local model copies ready in ${modelsDir.absolutePath}")
                Result.success(paths)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to prepare local model copies", e)
                CrashAnalytics.recordNonFatal(e, "model_file_copy")
                Result.failure(
                    Exception(UserFacingErrors.forThrowable(e, ErrorCategory.MODEL))
                )
            }
        }
    }

    fun getCachedPaths(): LocalModelPaths? {
        val cached = cachedPaths ?: return null
        return if (cachedPathsAreValid(cached)) cached else null
    }

    private fun cachedPathsAreValid(paths: LocalModelPaths): Boolean {
        return File(paths.llmPath).length() >= ModelAssetConstants.MIN_LLM_SIZE_BYTES &&
            File(paths.geckoPath).length() >= ModelAssetConstants.MIN_GECKO_SIZE_BYTES &&
            File(paths.tokenizerPath).length() > 0L
    }

    private fun copyIfNeeded(source: File, destination: File, minSizeBytes: Long) {
        if (!source.exists()) {
            throw IllegalStateException("Source model missing: ${source.absolutePath}")
        }
        if (destination.exists() && destination.length() >= minSizeBytes && destination.length() == source.length()) {
            return
        }
        Log.d(TAG, "Copying ${source.name} to app storage...")
        CrashAnalytics.logRagPhase("copy_file", source.name)
        source.inputStream().use { inputStream ->
            destination.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        if (destination.length() < minSizeBytes) {
            throw IllegalStateException("Copied file is too small: ${destination.name}")
        }
    }
}
