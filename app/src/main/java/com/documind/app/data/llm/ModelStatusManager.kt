package com.documind.app.data.llm

import android.content.Context
import android.util.Log
import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

sealed class ModelState {
    data object Idle : ModelState()
    data class Downloading(val progress: Int) : ModelState()
    data object Ready : ModelState()
    data class Error(val message: String) : ModelState()
}

class ModelStatusManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ModelStatusManager"
        const val MODEL_PACK_NAME = "model_pack"
        const val MODEL_FILE_NAME = "gemma3-1b.task"
    }
    
    private val assetPackManager: AssetPackManager = AssetPackManagerFactory.getInstance(context)
    
    private val _modelState = MutableStateFlow<ModelState>(ModelState.Idle)
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()
    
    private var modelPath: String? = null
    
    private val stateUpdateListener = AssetPackStateUpdateListener { state ->
        handlePackState(state)
    }
    
    init {
        assetPackManager.registerListener(stateUpdateListener)
    }
    
    fun checkModelStatus() {
        // First check if model exists in local assets (for development/testing)
        val localModelPath = checkLocalModel()
        if (localModelPath != null) {
            modelPath = localModelPath
            _modelState.value = ModelState.Ready
            Log.d(TAG, "Model found at local path: $localModelPath")
            return
        }
        
        // Then check Play Asset Delivery
        val packLocation = assetPackManager.getPackLocation(MODEL_PACK_NAME)
        if (packLocation != null) {
            modelPath = "${packLocation.assetsPath()}/$MODEL_FILE_NAME"
            _modelState.value = ModelState.Ready
            Log.d(TAG, "Model found in asset pack: $modelPath")
        } else {
            requestModelDownload()
        }
    }
    
    private fun checkLocalModel(): String? {
        // Check in app's files directory (for manually copied models during development)
        val filesDir = File(context.filesDir, MODEL_FILE_NAME)
        if (filesDir.exists()) {
            Log.d(TAG, "Found model in files dir: ${filesDir.absolutePath}")
            return filesDir.absolutePath
        }
        
        // Check in external files directory
        val externalDir = context.getExternalFilesDir(null)
        if (externalDir != null) {
            val externalModel = File(externalDir, MODEL_FILE_NAME)
            if (externalModel.exists()) {
                Log.d(TAG, "Found model in external files dir: ${externalModel.absolutePath}")
                return externalModel.absolutePath
            }
        }
        
        // For local development: Try to copy from app assets to files dir
        try {
            val assetFiles = context.assets.list("") ?: emptyArray()
            if (assetFiles.contains(MODEL_FILE_NAME)) {
                Log.d(TAG, "Found model in app assets, copying to files dir...")
                val destFile = File(context.filesDir, MODEL_FILE_NAME)
                context.assets.open(MODEL_FILE_NAME).use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "Model copied to: ${destFile.absolutePath}")
                return destFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy model from assets", e)
        }
        
        return null
    }
    
    private fun requestModelDownload() {
        _modelState.value = ModelState.Downloading(0)
        try {
            assetPackManager.fetch(listOf(MODEL_PACK_NAME))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch asset pack", e)
            handleAssetPackUnavailable()
        }
    }
    
    private fun handleAssetPackUnavailable() {
        _modelState.value = ModelState.Error(
            "Model not available. For local testing:\n" +
            "1. Download gemma3-1b.task model file\n" +
            "2. Copy to: ${context.filesDir.absolutePath}/$MODEL_FILE_NAME\n" +
            "Or deploy via Play Store for asset pack delivery."
        )
    }
    
    private fun handlePackState(packState: AssetPackState) {
        if (packState.name() != MODEL_PACK_NAME) return
        
        when (packState.status()) {
            AssetPackStatus.PENDING,
            AssetPackStatus.DOWNLOADING -> {
                val totalBytes = packState.totalBytesToDownload()
                val downloadedBytes = packState.bytesDownloaded()
                val progress = if (totalBytes > 0) {
                    ((downloadedBytes * 100) / totalBytes).toInt()
                } else {
                    0
                }
                _modelState.value = ModelState.Downloading(progress)
            }
            AssetPackStatus.TRANSFERRING -> {
                _modelState.value = ModelState.Downloading(99)
            }
            AssetPackStatus.COMPLETED -> {
                val packLocation = assetPackManager.getPackLocation(MODEL_PACK_NAME)
                if (packLocation != null) {
                    modelPath = "${packLocation.assetsPath()}/$MODEL_FILE_NAME"
                    _modelState.value = ModelState.Ready
                } else {
                    _modelState.value = ModelState.Error("Failed to locate model after download")
                }
            }
            AssetPackStatus.FAILED -> {
                val errorCode = packState.errorCode()
                if (errorCode == -5) {
                    handleAssetPackUnavailable()
                } else {
                    _modelState.value = ModelState.Error("Model download failed: $errorCode")
                }
            }
            AssetPackStatus.CANCELED -> {
                _modelState.value = ModelState.Error("Model download was canceled")
            }
            AssetPackStatus.NOT_INSTALLED -> {
                requestModelDownload()
            }
            else -> {
                // Unknown status - check if API unavailable
                handleAssetPackUnavailable()
            }
        }
    }
    
    fun getModelPath(): String? = modelPath
    
    fun cleanup() {
        assetPackManager.unregisterListener(stateUpdateListener)
    }
}
