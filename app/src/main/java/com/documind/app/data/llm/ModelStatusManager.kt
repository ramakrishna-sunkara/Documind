package com.documind.app.data.llm

import android.app.Activity
import android.content.Context
import android.util.Log
import com.documind.app.data.analytics.CrashAnalytics
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
        const val MODEL_PACK_NAME = ModelAssetConstants.MODEL_PACK_NAME
        const val MODEL_FILE_NAME = ModelAssetConstants.LLM_FILE_NAME
        const val GECKO_MODEL_FILE_NAME = ModelAssetConstants.GECKO_MODEL_FILE_NAME
        const val TOKENIZER_FILE_NAME = ModelAssetConstants.TOKENIZER_FILE_NAME
    }

    private var assetPackManager: AssetPackManager? = null
    private val _modelState = MutableStateFlow<ModelState>(ModelState.Idle)
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()
    private var modelPath: String? = null
    private var geckoModelPath: String? = null
    private var tokenizerPath: String? = null
    private var activityRef: Activity? = null

    private val listener = AssetPackStateUpdateListener { state ->
        try {
            if (state.name() == MODEL_PACK_NAME) {
                handleState(state)
            }
        } catch (throwable: Throwable) {
            logError("Asset pack listener failed", throwable)
        }
    }

    init {
        try {
            assetPackManager = AssetPackManagerFactory.getInstance(context)
            assetPackManager?.registerListener(listener)
            CrashAnalytics.log("ModelStatusManager initialized")
        } catch (throwable: Throwable) {
            logError("AssetPackManager init failed", throwable)
        }
    }

    fun setActivity(activity: Activity?) {
        activityRef = activity
    }

    fun checkModelStatus() {
        CrashAnalytics.logModelDownloadPhase("check_status", "started")
        try {
            val manager = assetPackManager
            if (manager == null) {
                setError("AI models are delivered via Play Store. Please install DocuMind from Google Play.")
                return
            }
            val assetsPath = manager.getPackLocation(MODEL_PACK_NAME)?.assetsPath()
            if (assetsPath != null) {
                CrashAnalytics.log("Asset pack path found: $assetsPath")
                resolveModelsFromAssetPack(File(assetsPath))?.let { assets ->
                    applyResolvedModels(assets)
                    return
                }
                CrashAnalytics.log("Asset pack incomplete, requesting download")
            } else {
                CrashAnalytics.log("Asset pack not installed yet")
            }
            startDownload()
        } catch (throwable: Throwable) {
            logError("checkModelStatus failed", throwable)
            setError("Failed to check AI models")
        }
    }

    fun startDownload() {
        _modelState.value = ModelState.Downloading(0)
        CrashAnalytics.logModelDownloadPhase("download_requested", "fetch")
        val manager = assetPackManager
        if (manager == null) {
            setError("AI models are delivered via Play Store. Please install DocuMind from Google Play.")
            return
        }
        manager.fetch(listOf(MODEL_PACK_NAME))
            .addOnSuccessListener {
                CrashAnalytics.log("Play Asset Delivery fetch started")
            }
            .addOnFailureListener { error ->
                logError("Download fetch failed", error)
                setError("Could not download AI models. Install or update DocuMind from Google Play.")
            }
    }

    fun requestCellularDownload(activity: Activity) {
        try {
            assetPackManager?.showCellularDataConfirmation(activity)
        } catch (throwable: Throwable) {
            logError("Cellular request failed", throwable)
        }
    }

    fun getModelPath(): String? = modelPath

    fun getGeckoModelPath(): String? = geckoModelPath

    fun getTokenizerPath(): String? = tokenizerPath

    fun cleanup() {
        try {
            assetPackManager?.unregisterListener(listener)
        } catch (throwable: Throwable) {
            logError("Cleanup failed", throwable)
        }
    }

    private fun applyResolvedModels(assets: ResolvedModelAssets) {
        modelPath = assets.llmPath
        geckoModelPath = assets.geckoPath
        tokenizerPath = assets.tokenizerPath
        CrashAnalytics.logResolvedModelFiles(assets.llmPath, assets.geckoPath, assets.tokenizerPath)
        CrashAnalytics.logModelDownloadPhase("models_ready", "completed", progress = 100)
        _modelState.value = ModelState.Ready
        Log.d(TAG, "All Play Asset Delivery models ready")
    }

    private fun resolveModelsFromAssetPack(directory: File): ResolvedModelAssets? {
        if (!directory.exists() || !directory.isDirectory) {
            CrashAnalytics.log("Asset pack directory missing: ${directory.absolutePath}")
            return null
        }
        val files = try {
            directory.listFiles()?.associateBy { file -> file.name } ?: emptyMap()
        } catch (throwable: Throwable) {
            logError("Failed to list asset pack files", throwable)
            return null
        }
        CrashAnalytics.log("Asset pack files: ${files.keys.sorted()}")
        val llmFile = files[MODEL_FILE_NAME]?.takeIf { file ->
            file.length() >= ModelAssetConstants.MIN_LLM_SIZE_BYTES
        } ?: files.values.firstOrNull { file ->
            file.name.endsWith(".task") && file.length() >= ModelAssetConstants.MIN_LLM_SIZE_BYTES
        }
        val geckoFile = files[GECKO_MODEL_FILE_NAME]?.takeIf { file ->
            file.length() >= ModelAssetConstants.MIN_GECKO_SIZE_BYTES
        } ?: files.values.firstOrNull { file ->
            file.name.endsWith(".tflite") && file.length() >= ModelAssetConstants.MIN_GECKO_SIZE_BYTES
        }
        val tokenizerFile = files[TOKENIZER_FILE_NAME]?.takeIf { file ->
            file.length() > 0L
        }
        CrashAnalytics.logResolvedModelFiles(
            llmPath = llmFile?.absolutePath,
            geckoPath = geckoFile?.absolutePath,
            tokenizerPath = tokenizerFile?.absolutePath
        )
        if (llmFile == null || geckoFile == null || tokenizerFile == null) {
            val missingFiles = buildList {
                if (llmFile == null) add(MODEL_FILE_NAME)
                if (geckoFile == null) add(GECKO_MODEL_FILE_NAME)
                if (tokenizerFile == null) add(TOKENIZER_FILE_NAME)
            }
            logError("Missing model files in asset pack: $missingFiles", null)
            setError("AI model update required. Please update DocuMind from Google Play.")
            return null
        }
        return ResolvedModelAssets(
            llmPath = llmFile.absolutePath,
            geckoPath = geckoFile.absolutePath,
            tokenizerPath = tokenizerFile.absolutePath
        )
    }

    private fun handleState(state: AssetPackState) {
        val status = state.status()
        val bytes = state.bytesDownloaded()
        val total = state.totalBytesToDownload()
        val progress = if (total > 0) ((bytes * 100) / total).toInt() else 0
        CrashAnalytics.recordDeviceMemory(context)
        CrashAnalytics.logModelDownloadPhase(
            phase = "asset_pack_update",
            status = status.toString(),
            progress = progress,
            bytesDownloaded = bytes,
            totalBytes = total
        )
        when (status) {
            AssetPackStatus.COMPLETED -> {
                try {
                    CrashAnalytics.log("Asset pack COMPLETED - resolving models")
                    val assetsPath = assetPackManager?.getPackLocation(MODEL_PACK_NAME)?.assetsPath()
                    if (assetsPath == null) {
                        setError("Download error. Please retry from Google Play.")
                        return
                    }
                    resolveModelsFromAssetPack(File(assetsPath))?.let { assets ->
                        applyResolvedModels(assets)
                    }
                } catch (throwable: Throwable) {
                    logError("COMPLETED handler failed", throwable)
                    setError("Error accessing AI models")
                }
            }
            AssetPackStatus.DOWNLOADING, AssetPackStatus.PENDING -> {
                _modelState.value = ModelState.Downloading(progress)
            }
            AssetPackStatus.TRANSFERRING -> {
                CrashAnalytics.log("Asset pack TRANSFERRING at 99%")
                _modelState.value = ModelState.Downloading(99)
            }
            AssetPackStatus.WAITING_FOR_WIFI -> {
                _modelState.value = ModelState.WaitingForWifi
                activityRef?.let { activity ->
                    try {
                        assetPackManager?.showCellularDataConfirmation(activity)
                    } catch (throwable: Throwable) {
                        logError("Cellular dialog failed", throwable)
                    }
                }
            }
            AssetPackStatus.FAILED -> {
                logError("Asset pack FAILED code=${state.errorCode()}", null)
                setError("Download failed. Tap to retry.")
            }
            AssetPackStatus.NOT_INSTALLED -> {
                startDownload()
            }
            AssetPackStatus.CANCELED -> {
                setError("Download canceled. Tap to retry.")
            }
            else -> {
                CrashAnalytics.log("Unknown asset pack status: $status")
            }
        }
    }

    private fun setError(message: String) {
        _modelState.value = ModelState.Error(message)
        CrashAnalytics.recordNonFatal(
            throwable = IllegalStateException(message),
            phase = "model_status_error"
        )
    }

    private fun logError(message: String, throwable: Throwable?) {
        Log.e(TAG, message, throwable)
        CrashAnalytics.log("ModelStatusManager error: $message")
        if (throwable != null) {
            CrashAnalytics.recordNonFatal(throwable, "model_status_manager")
        } else {
            CrashAnalytics.recordNonFatal(IllegalStateException(message), "model_status_manager")
        }
    }

    private data class ResolvedModelAssets(
        val llmPath: String,
        val geckoPath: String,
        val tokenizerPath: String
    )
}
