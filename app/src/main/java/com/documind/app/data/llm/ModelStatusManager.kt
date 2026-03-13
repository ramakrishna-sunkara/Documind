package com.documind.app.data.llm

import android.app.Activity
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
    data object WaitingForWifi : ModelState()
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
    private var activityRef: Activity? = null
    
    private val listener = AssetPackStateUpdateListener { state ->
        if (state.name() == MODEL_PACK_NAME) {
            handleState(state)
        }
    }
    
    init {
        assetPackManager.registerListener(listener)
    }
    
    fun setActivity(activity: Activity?) {
        activityRef = activity
    }
    
    fun checkModelStatus() {
        Log.d(TAG, "Checking model...")
        
        // 1. Check if already downloaded via Play Asset Delivery
        val location = assetPackManager.getPackLocation(MODEL_PACK_NAME)
        if (location != null) {
            val path = "${location.assetsPath()}/$MODEL_FILE_NAME"
            if (File(path).exists()) {
                modelPath = path
                _modelState.value = ModelState.Ready
                Log.d(TAG, "Model ready: $path")
                return
            }
        }
        
        // 2. Check local files (for development)
        checkLocalFiles()?.let {
            modelPath = it
            _modelState.value = ModelState.Ready
            Log.d(TAG, "Local model: $it")
            return
        }
        
        // 3. Start download
        startDownload()
    }
    
    private fun checkLocalFiles(): String? {
        listOf(
            File(context.filesDir, MODEL_FILE_NAME),
            context.getExternalFilesDir(null)?.let { File(it, MODEL_FILE_NAME) }
        ).forEach { file ->
            if (file?.exists() == true && file.length() > 0) {
                return file.absolutePath
            }
        }
        return null
    }
    
    fun startDownload() {
        Log.d(TAG, "Starting download...")
        _modelState.value = ModelState.Downloading(0)
        
        try {
            assetPackManager.fetch(listOf(MODEL_PACK_NAME))
                .addOnSuccessListener { Log.d(TAG, "Fetch started") }
                .addOnFailureListener { e -> 
                    Log.e(TAG, "Fetch failed", e)
                    _modelState.value = ModelState.Error("Download failed")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch error", e)
            _modelState.value = ModelState.Error("Download error")
        }
    }
    
    fun requestCellularDownload(activity: Activity) {
        try {
            assetPackManager.showCellularDataConfirmation(activity)
        } catch (e: Exception) {
            Log.e(TAG, "Cellular request failed", e)
        }
    }
    
    private fun handleState(state: AssetPackState) {
        val status = state.status()
        Log.d(TAG, "Status: $status")
        
        when (status) {
            AssetPackStatus.COMPLETED -> {
                val loc = assetPackManager.getPackLocation(MODEL_PACK_NAME)
                if (loc != null) {
                    modelPath = "${loc.assetsPath()}/$MODEL_FILE_NAME"
                    _modelState.value = ModelState.Ready
                    Log.d(TAG, "Download complete: $modelPath")
                }
            }
            AssetPackStatus.DOWNLOADING, AssetPackStatus.PENDING -> {
                val total = state.totalBytesToDownload()
                val done = state.bytesDownloaded()
                val progress = if (total > 0) ((done * 100) / total).toInt() else 0
                _modelState.value = ModelState.Downloading(progress)
            }
            AssetPackStatus.TRANSFERRING -> {
                _modelState.value = ModelState.Downloading(99)
            }
            AssetPackStatus.WAITING_FOR_WIFI -> {
                _modelState.value = ModelState.WaitingForWifi
                activityRef?.let { requestCellularDownload(it) }
            }
            AssetPackStatus.FAILED -> {
                _modelState.value = ModelState.Error("Download failed: ${state.errorCode()}")
            }
            AssetPackStatus.NOT_INSTALLED -> {
                startDownload()
            }
            else -> {}
        }
    }
    
    fun getModelPath(): String? = modelPath
    
    fun cleanup() {
        assetPackManager.unregisterListener(listener)
    }
}
