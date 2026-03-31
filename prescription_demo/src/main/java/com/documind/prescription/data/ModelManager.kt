package com.documind.prescription.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed class ModelLoadState {
    data object Idle : ModelLoadState()
    data object Loading : ModelLoadState()
    data class Copying(val progress: Int) : ModelLoadState()
    data object Ready : ModelLoadState()
    data class Error(val message: String) : ModelLoadState()
}

class ModelManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ModelManager"
        private const val MODEL_FILE_NAME = "gemma3-1b-it-int4.task"
        private const val ASSETS_MODEL_PATH = "model/$MODEL_FILE_NAME"
    }
    
    private val _modelState = MutableStateFlow<ModelLoadState>(ModelLoadState.Idle)
    val modelState: StateFlow<ModelLoadState> = _modelState.asStateFlow()
    
    private var modelPath: String? = null
    
    suspend fun checkAndLoadModel() = withContext(Dispatchers.IO) {
        _modelState.value = ModelLoadState.Loading
        
        try {
            val localModelFile = File(context.filesDir, MODEL_FILE_NAME)
            
            if (localModelFile.exists() && localModelFile.length() > 100_000_000) {
                modelPath = localModelFile.absolutePath
                _modelState.value = ModelLoadState.Ready
                Log.d(TAG, "Model ready from local: ${localModelFile.absolutePath}")
                return@withContext
            }
            
            if (isModelInAssets()) {
                Log.d(TAG, "Model found in assets, copying to internal storage...")
                copyModelFromAssets(localModelFile)
                
                if (localModelFile.exists() && localModelFile.length() > 100_000_000) {
                    modelPath = localModelFile.absolutePath
                    _modelState.value = ModelLoadState.Ready
                    Log.d(TAG, "Model copied successfully: ${localModelFile.absolutePath}")
                    return@withContext
                }
            }
            
            _modelState.value = ModelLoadState.Error("Model not found in assets. Add model/$MODEL_FILE_NAME")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model: ${e.message}", e)
            _modelState.value = ModelLoadState.Error(e.message ?: "Unknown error")
        }
    }
    
    private fun isModelInAssets(): Boolean {
        return try {
            context.assets.open(ASSETS_MODEL_PATH).use { true }
        } catch (e: Exception) {
            Log.d(TAG, "Model not in assets: ${e.message}")
            false
        }
    }
    
    private suspend fun copyModelFromAssets(destFile: File) = withContext(Dispatchers.IO) {
        try {
            _modelState.value = ModelLoadState.Copying(0)
            
            context.assets.open(ASSETS_MODEL_PATH).use { inputStream ->
                val totalSize = inputStream.available().toLong()
                var copiedBytes = 0L
                
                FileOutputStream(destFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        copiedBytes += bytesRead
                        
                        if (totalSize > 0) {
                            val progress = ((copiedBytes * 100) / totalSize).toInt()
                            _modelState.value = ModelLoadState.Copying(progress.coerceIn(0, 99))
                        }
                    }
                }
            }
            
            Log.d(TAG, "Model copy completed: ${destFile.length()} bytes")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error copying model: ${e.message}", e)
            destFile.delete()
            throw e
        }
    }
    
    fun getModelPath(): String? = modelPath
}
