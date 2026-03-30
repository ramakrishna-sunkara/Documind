package com.documind.app.data.llm

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
        // Actual filename from Kaggle: gemma3-1b-it-int4.task (555MB, INT4 quantized)
        const val MODEL_FILE_NAME = "gemma3-1b-it-int4.task"
    }
    
    private var assetPackManager: AssetPackManager? = null
    
    private val _modelState = MutableStateFlow<ModelState>(ModelState.Idle)
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()
    
    private var modelPath: String? = null
    private var activityRef: Activity? = null
    
    private fun log(message: String) {
        Log.d(TAG, message)
    }
    
    private fun logError(message: String, e: Exception? = null) {
        Log.e(TAG, message, e)
        // Send to Crashlytics for debugging
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.log("$TAG: $message")
            if (e != null) {
                crashlytics.recordException(e)
            } else {
                crashlytics.recordException(Exception("ModelError: $message"))
            }
        } catch (_: Exception) {}
    }
    
    private val listener = AssetPackStateUpdateListener { state ->
        try {
            if (state.name() == MODEL_PACK_NAME) {
                handleState(state)
            }
        } catch (e: Exception) {
            logError("Listener error", e)
        }
    }
    
    init {
        try {
            assetPackManager = AssetPackManagerFactory.getInstance(context)
            assetPackManager?.registerListener(listener)
        } catch (e: Exception) {
            logError("AssetPackManager init failed", e)
        }
    }
    
    fun setActivity(activity: Activity?) {
        activityRef = activity
    }
    
    fun checkModelStatus() {
        log("Checking model status...")
        
        try {
            // 1. Check asset pack location (already downloaded)
            val manager = assetPackManager
            if (manager != null) {
                val location = manager.getPackLocation(MODEL_PACK_NAME)
                if (location != null) {
                    val assetsPath = location.assetsPath()
                    
                    // Try exact filename
                    val exactPath = "$assetsPath/$MODEL_FILE_NAME"
                    if (File(exactPath).exists()) {
                        modelPath = exactPath
                        _modelState.value = ModelState.Ready
                        log("Model ready: $exactPath")
                        return
                    }
                    
                    // Search for .task file in assets folder
                    val assetsDir = File(assetsPath)
                    if (assetsDir.exists()) {
                        val taskFile = assetsDir.listFiles()?.find { 
                            it.name.endsWith(".task") && it.length() > 100_000_000 
                        }
                        if (taskFile != null) {
                            modelPath = taskFile.absolutePath
                            _modelState.value = ModelState.Ready
                            log("Model ready: ${taskFile.absolutePath}")
                            return
                        }
                    }
                }
            }
            
            // 2. Check local files
            checkLocalFiles()?.let {
                modelPath = it
                _modelState.value = ModelState.Ready
                log("Model ready from local: $it")
                return
            }
            
            // 3. Auto-start download
            log("Model not found, starting download...")
            startDownload()
            
        } catch (e: Exception) {
            logError("checkModelStatus failed", e)
            _modelState.value = ModelState.Error("Failed to check model")
        }
    }
    
    private fun checkLocalFiles(): String? {
        try {
            val locations = listOf(
                File(context.filesDir, MODEL_FILE_NAME),
                context.getExternalFilesDir(null)?.let { File(it, MODEL_FILE_NAME) },
                File("/sdcard/Download", MODEL_FILE_NAME)
            )
            
            for (file in locations) {
                if (file?.exists() == true && file.length() > 100_000_000) {
                    return file.absolutePath
                }
            }
        } catch (e: Exception) {
            logError("checkLocalFiles failed", e)
        }
        return null
    }
    
    fun startDownload() {
        _modelState.value = ModelState.Downloading(0)
        
        val manager = assetPackManager
        if (manager == null) {
            logError("AssetPackManager is null", null)
            _modelState.value = ModelState.Error("Please install from Play Store")
            return
        }
        
        manager.fetch(listOf(MODEL_PACK_NAME))
            .addOnSuccessListener { 
                log("Download started")
            }
            .addOnFailureListener { e -> 
                logError("Download failed: ${e.message}", e)
                _modelState.value = ModelState.Error("Download failed. Check connection.")
            }
    }
    
    fun requestCellularDownload(activity: Activity) {
        try {
            assetPackManager?.showCellularDataConfirmation(activity)
        } catch (e: Exception) {
            Log.e(TAG, "Cellular request failed", e)
        }
    }
    
    private fun handleState(state: AssetPackState) {
        val status = state.status()
        val bytes = state.bytesDownloaded()
        val total = state.totalBytesToDownload()
        
        when (status) {
            AssetPackStatus.COMPLETED -> {
                try {
                    val loc = assetPackManager?.getPackLocation(MODEL_PACK_NAME)
                    if (loc == null) {
                        logError("Pack location is null after COMPLETED", null)
                        _modelState.value = ModelState.Error("Download error. Please retry.")
                        return
                    }
                    
                    val assetsPath = loc.assetsPath()
                    log("Assets path: $assetsPath")
                    
                    // Try direct path first
                    val directPath = "$assetsPath/$MODEL_FILE_NAME"
                    if (File(directPath).exists()) {
                        modelPath = directPath
                        _modelState.value = ModelState.Ready
                        log("Model ready at: $directPath")
                        return
                    }
                    
                    // List files in assets folder to find the model
                    val assetsDir = File(assetsPath)
                    if (assetsDir.exists() && assetsDir.isDirectory) {
                        val files = assetsDir.listFiles()
                        log("Files in assets: ${files?.map { it.name }}")
                        
                        // Find any .task file
                        val taskFile = files?.find { it.name.endsWith(".task") }
                        if (taskFile != null && taskFile.length() > 100_000_000) {
                            modelPath = taskFile.absolutePath
                            _modelState.value = ModelState.Ready
                            log("Model found: ${taskFile.absolutePath}")
                            return
                        }
                    }
                    
                    // Log detailed error for debugging
                    logError("Model not found. assetsPath=$assetsPath, exists=${assetsDir.exists()}, files=${assetsDir.listFiles()?.size ?: 0}", null)
                    _modelState.value = ModelState.Error("Model file not found. Please retry.")
                    
                } catch (e: Exception) {
                    logError("Error accessing model: ${e.message}", e)
                    _modelState.value = ModelState.Error("Error accessing model")
                }
            }
            AssetPackStatus.DOWNLOADING, AssetPackStatus.PENDING -> {
                val progress = if (total > 0) ((bytes * 100) / total).toInt() else 0
                _modelState.value = ModelState.Downloading(progress)
            }
            AssetPackStatus.TRANSFERRING -> {
                _modelState.value = ModelState.Downloading(99)
            }
            AssetPackStatus.WAITING_FOR_WIFI -> {
                _modelState.value = ModelState.WaitingForWifi
                // Auto-show cellular confirmation dialog
                activityRef?.let { activity ->
                    try {
                        assetPackManager?.showCellularDataConfirmation(activity)
                    } catch (e: Exception) {
                        log("Could not show cellular dialog: ${e.message}")
                    }
                }
            }
            AssetPackStatus.FAILED -> {
                val errorCode = state.errorCode()
                logError("Download failed: code=$errorCode", null)
                _modelState.value = ModelState.Error("Download failed. Tap to retry.")
            }
            AssetPackStatus.NOT_INSTALLED -> {
                startDownload()
            }
            AssetPackStatus.CANCELED -> {
                _modelState.value = ModelState.Error("Download canceled. Tap to retry.")
            }
            else -> {
                log("Unknown status: $status")
            }
        }
    }
    
    fun getModelPath(): String? = modelPath
    
    fun cleanup() {
        try {
            assetPackManager?.unregisterListener(listener)
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup error", e)
        }
    }
}
