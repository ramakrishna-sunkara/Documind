package com.documind.app.data.analytics

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.documind.app.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.io.File

object CrashAnalytics {

    private const val TAG = "CrashAnalytics"
    private var isInitialized: Boolean = false

    fun initialize(context: Context) {
        if (isInitialized) {
            return
        }
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.isCrashlyticsCollectionEnabled = true
            crashlytics.setCustomKey("app_version", BuildConfig.VERSION_NAME)
            crashlytics.setCustomKey("version_code", BuildConfig.VERSION_CODE)
            crashlytics.setCustomKey("build_type", if (BuildConfig.DEBUG) "debug" else "release")
            crashlytics.setCustomKey("device_model", Build.MODEL)
            crashlytics.setCustomKey("device_manufacturer", Build.MANUFACTURER)
            crashlytics.setCustomKey("android_sdk", Build.VERSION.SDK_INT)
            recordDeviceMemory(context)
            installUncaughtExceptionHandler()
            isInitialized = true
            log("CrashAnalytics initialized")
        } catch (e: Exception) {
            Log.e(TAG, "CrashAnalytics init failed: ${e.message}")
        }
    }

    fun log(message: String) {
        Log.d(TAG, message)
        try {
            FirebaseCrashlytics.getInstance().log(message)
        } catch (_: Exception) {
        }
    }

    fun setCustomKey(key: String, value: String) {
        try {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        } catch (_: Exception) {
        }
    }

    fun setCustomKey(key: String, value: Int) {
        try {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        } catch (_: Exception) {
        }
    }

    fun setCustomKey(key: String, value: Long) {
        try {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        } catch (_: Exception) {
        }
    }

    fun setCustomKey(key: String, value: Boolean) {
        try {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        } catch (_: Exception) {
        }
    }

    fun recordNonFatal(throwable: Throwable, phase: String, details: Map<String, String> = emptyMap()) {
        Log.e(TAG, "Non-fatal [$phase]: ${throwable.message}", throwable)
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCustomKey("last_error_phase", phase)
            details.forEach { (key, value) ->
                crashlytics.setCustomKey(key, value.take(100))
            }
            crashlytics.recordException(throwable)
        } catch (_: Exception) {
        }
    }

    fun logModelDownloadPhase(
        phase: String,
        status: String,
        progress: Int = -1,
        bytesDownloaded: Long = -1L,
        totalBytes: Long = -1L
    ) {
        setCustomKey("model_phase", phase)
        setCustomKey("model_pack_status", status)
        if (progress >= 0) {
            setCustomKey("model_download_progress", progress)
        }
        if (bytesDownloaded >= 0L) {
            setCustomKey("model_bytes_downloaded", bytesDownloaded)
        }
        if (totalBytes >= 0L) {
            setCustomKey("model_total_bytes", totalBytes)
        }
        log(
            "ModelDownload phase=$phase status=$status progress=$progress " +
                "bytes=$bytesDownloaded/$totalBytes"
        )
    }

    fun logResolvedModelFiles(
        llmPath: String?,
        geckoPath: String?,
        tokenizerPath: String?
    ) {
        setCustomKey("llm_path_present", llmPath != null)
        setCustomKey("gecko_path_present", geckoPath != null)
        setCustomKey("tokenizer_path_present", tokenizerPath != null)
        llmPath?.let { path ->
            setCustomKey("llm_size_mb", fileSizeMb(path))
        }
        geckoPath?.let { path ->
            setCustomKey("gecko_size_mb", fileSizeMb(path))
        }
        tokenizerPath?.let { path ->
            setCustomKey("tokenizer_size_kb", File(path).length() / 1024)
        }
        log(
            "Resolved models llm=${llmPath != null} gecko=${geckoPath != null} " +
                "tokenizer=${tokenizerPath != null}"
        )
    }

    fun logRagPhase(phase: String, detail: String = "") {
        setCustomKey("rag_phase", phase)
        if (detail.isNotBlank()) {
            setCustomKey("rag_detail", detail.take(100))
        }
        log("RagPhase phase=$phase detail=$detail")
    }

    fun reportSessionStart(context: Context) {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCustomKey("app_version", BuildConfig.VERSION_NAME)
            crashlytics.setCustomKey("version_code", BuildConfig.VERSION_CODE)
            crashlytics.log("Session start v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            if (crashlytics.didCrashOnPreviousExecution()) {
                log("Detected crash on previous execution")
                recordNonFatal(
                    throwable = IllegalStateException("App crashed on previous session"),
                    phase = "previous_session_crash",
                    details = mapOf(
                        "version_name" to BuildConfig.VERSION_NAME,
                        "version_code" to BuildConfig.VERSION_CODE.toString()
                    )
                )
            }
            crashlytics.sendUnsentReports()
            AnalyticsManager.logCrashlyticsSession(
                versionName = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE
            )
        } catch (e: Exception) {
            Log.e(TAG, "Session report failed: ${e.message}")
        }
    }

    fun sendVerificationIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val verificationKey = "verified_${BuildConfig.VERSION_CODE}"
        if (prefs.getBoolean(verificationKey, false)) {
            return
        }
        triggerVerificationNonFatal()
        prefs.edit().putBoolean(verificationKey, true).apply()
    }

    private const val PREFS_NAME = "crashlytics_prefs"

    private fun triggerVerificationNonFatal() {
        recordNonFatal(
            throwable = IllegalStateException("Crashlytics verification ping"),
            phase = "verification_ping",
            details = mapOf(
                "version_name" to BuildConfig.VERSION_NAME,
                "version_code" to BuildConfig.VERSION_CODE.toString()
            )
        )
        try {
            FirebaseCrashlytics.getInstance().sendUnsentReports()
        } catch (_: Exception) {
        }
    }

    private fun fileSizeMb(path: String): Long {
        val sizeBytes = File(path).length()
        return if (sizeBytes <= 0L) 0L else sizeBytes / (1024 * 1024)
    }

    private fun installUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            log("Uncaught exception on ${thread.name}: ${throwable.message}")
            try {
                FirebaseCrashlytics.getInstance().recordException(throwable)
            } catch (_: Exception) {
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun recordDeviceMemory(context: Context) {
        recordDeviceMemoryInternal(context)
    }

    private fun recordDeviceMemoryInternal(context: Context?) {
        if (context == null) {
            return
        }
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            setCustomKey("device_total_ram_mb", memoryInfo.totalMem / (1024 * 1024))
            setCustomKey("device_avail_ram_mb", memoryInfo.availMem / (1024 * 1024))
            setCustomKey("device_low_memory", memoryInfo.lowMemory)
            val runtime = Runtime.getRuntime()
            setCustomKey("jvm_max_heap_mb", runtime.maxMemory() / (1024 * 1024))
        } catch (e: Exception) {
            recordNonFatal(e, "record_device_memory")
        }
    }
}
