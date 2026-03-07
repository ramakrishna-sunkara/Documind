package com.documind.app.data.update

import android.app.Activity
import android.content.IntentSender
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.documind.app.data.analytics.AnalyticsManager
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InAppUpdateManager(private val activity: Activity) {
    
    companion object {
        private const val TAG = "InAppUpdateManager"
        
        // Days before prompting for flexible update
        private const val DAYS_FOR_FLEXIBLE_UPDATE = 3
        
        // Priority threshold for immediate update (1-5, 5 = critical)
        private const val IMMEDIATE_UPDATE_PRIORITY = 4
    }
    
    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)
    
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()
    
    private var updateLauncher: ActivityResultLauncher<IntentSenderRequest>? = null
    
    private val installStateListener = InstallStateUpdatedListener { state ->
        handleInstallState(state)
    }
    
    fun setUpdateLauncher(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        updateLauncher = launcher
    }
    
    fun checkForUpdate() {
        Log.d(TAG, "Checking for updates...")
        _updateState.value = UpdateState.Checking
        
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { updateInfo ->
                handleUpdateInfo(updateInfo)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Update check failed", exception)
                _updateState.value = UpdateState.Error(exception.message ?: "Update check failed")
                AnalyticsManager.logEvent("update_check_failed", mapOf(
                    "error" to (exception.message ?: "Unknown")
                ))
            }
    }
    
    private fun handleUpdateInfo(updateInfo: AppUpdateInfo) {
        val availability = updateInfo.updateAvailability()
        val isImmediateAllowed = updateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
        val isFlexibleAllowed = updateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
        
        Log.d(TAG, "Update availability: $availability")
        Log.d(TAG, "Available version: ${updateInfo.availableVersionCode()}")
        Log.d(TAG, "Update priority: ${updateInfo.updatePriority()}")
        Log.d(TAG, "Stale days: ${updateInfo.clientVersionStalenessDays()}")
        
        when (availability) {
            UpdateAvailability.UPDATE_AVAILABLE -> {
                val updatePriority = updateInfo.updatePriority()
                val staleDays = updateInfo.clientVersionStalenessDays() ?: 0
                
                // Determine update type based on priority and staleness
                val updateType = when {
                    // High priority = immediate update
                    updatePriority >= IMMEDIATE_UPDATE_PRIORITY && isImmediateAllowed -> {
                        AppUpdateType.IMMEDIATE
                    }
                    // Stale version = immediate update
                    staleDays >= 7 && isImmediateAllowed -> {
                        AppUpdateType.IMMEDIATE
                    }
                    // Otherwise flexible update
                    isFlexibleAllowed -> {
                        AppUpdateType.FLEXIBLE
                    }
                    // Fallback to immediate if flexible not allowed
                    isImmediateAllowed -> {
                        AppUpdateType.IMMEDIATE
                    }
                    else -> null
                }
                
                if (updateType != null) {
                    _updateState.value = UpdateState.Available(
                        versionCode = updateInfo.availableVersionCode(),
                        isImmediate = updateType == AppUpdateType.IMMEDIATE
                    )
                    
                    AnalyticsManager.logEvent("update_available", mapOf(
                        "version_code" to updateInfo.availableVersionCode().toString(),
                        "update_type" to if (updateType == AppUpdateType.IMMEDIATE) "immediate" else "flexible",
                        "priority" to updatePriority.toString(),
                        "stale_days" to staleDays.toString()
                    ))
                    
                    startUpdate(updateInfo, updateType)
                } else {
                    _updateState.value = UpdateState.NotAvailable
                }
            }
            
            UpdateAvailability.UPDATE_NOT_AVAILABLE -> {
                Log.d(TAG, "No update available")
                _updateState.value = UpdateState.NotAvailable
            }
            
            UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                Log.d(TAG, "Update already in progress")
                _updateState.value = UpdateState.Downloading(0)
                
                // Resume the update
                if (isImmediateAllowed) {
                    startUpdate(updateInfo, AppUpdateType.IMMEDIATE)
                }
            }
            
            else -> {
                _updateState.value = UpdateState.NotAvailable
            }
        }
    }
    
    private fun startUpdate(updateInfo: AppUpdateInfo, updateType: Int) {
        Log.d(TAG, "Starting update, type: ${if (updateType == AppUpdateType.IMMEDIATE) "IMMEDIATE" else "FLEXIBLE"}")
        
        try {
            if (updateType == AppUpdateType.FLEXIBLE) {
                appUpdateManager.registerListener(installStateListener)
            }
            
            val launcher = updateLauncher
            if (launcher != null) {
                appUpdateManager.startUpdateFlowForResult(
                    updateInfo,
                    launcher,
                    AppUpdateOptions.newBuilder(updateType).build()
                )
                
                AnalyticsManager.logEvent("update_started", mapOf(
                    "update_type" to if (updateType == AppUpdateType.IMMEDIATE) "immediate" else "flexible"
                ))
            } else {
                Log.e(TAG, "Update launcher not set")
                _updateState.value = UpdateState.Error("Update launcher not configured")
            }
        } catch (e: IntentSender.SendIntentException) {
            Log.e(TAG, "Failed to start update flow", e)
            _updateState.value = UpdateState.Error("Failed to start update")
        }
    }
    
    private fun handleInstallState(state: InstallState) {
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val bytesDownloaded = state.bytesDownloaded()
                val totalBytes = state.totalBytesToDownload()
                val progress = if (totalBytes > 0) {
                    ((bytesDownloaded * 100) / totalBytes).toInt()
                } else 0
                
                Log.d(TAG, "Downloading: $progress%")
                _updateState.value = UpdateState.Downloading(progress)
            }
            
            InstallStatus.DOWNLOADED -> {
                Log.d(TAG, "Update downloaded, ready to install")
                _updateState.value = UpdateState.ReadyToInstall
                
                AnalyticsManager.logEvent("update_downloaded", null)
            }
            
            InstallStatus.INSTALLING -> {
                Log.d(TAG, "Installing update...")
                _updateState.value = UpdateState.Installing
            }
            
            InstallStatus.INSTALLED -> {
                Log.d(TAG, "Update installed")
                _updateState.value = UpdateState.Installed
                appUpdateManager.unregisterListener(installStateListener)
                
                AnalyticsManager.logEvent("update_installed", null)
            }
            
            InstallStatus.FAILED -> {
                Log.e(TAG, "Update failed")
                _updateState.value = UpdateState.Error("Update installation failed")
                appUpdateManager.unregisterListener(installStateListener)
                
                AnalyticsManager.logEvent("update_failed", null)
            }
            
            InstallStatus.CANCELED -> {
                Log.d(TAG, "Update canceled by user")
                _updateState.value = UpdateState.Canceled
                appUpdateManager.unregisterListener(installStateListener)
                
                AnalyticsManager.logEvent("update_canceled", null)
            }
            
            else -> {
                // Pending, Unknown, etc.
            }
        }
    }
    
    fun completeUpdate() {
        Log.d(TAG, "Completing update...")
        appUpdateManager.completeUpdate()
    }
    
    fun handleUpdateResult(resultCode: Int) {
        when (resultCode) {
            Activity.RESULT_OK -> {
                Log.d(TAG, "Update flow accepted")
            }
            Activity.RESULT_CANCELED -> {
                Log.d(TAG, "Update flow canceled")
                _updateState.value = UpdateState.Canceled
                AnalyticsManager.logEvent("update_flow_canceled", null)
            }
            else -> {
                Log.e(TAG, "Update flow failed with code: $resultCode")
                _updateState.value = UpdateState.Error("Update failed")
            }
        }
    }
    
    fun resumeUpdate() {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { updateInfo ->
                if (updateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    _updateState.value = UpdateState.ReadyToInstall
                } else if (updateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    startUpdate(updateInfo, AppUpdateType.IMMEDIATE)
                }
            }
    }
    
    fun cleanup() {
        appUpdateManager.unregisterListener(installStateListener)
    }
}

sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data object NotAvailable : UpdateState()
    data class Available(val versionCode: Int, val isImmediate: Boolean) : UpdateState()
    data class Downloading(val progress: Int) : UpdateState()
    data object ReadyToInstall : UpdateState()
    data object Installing : UpdateState()
    data object Installed : UpdateState()
    data object Canceled : UpdateState()
    data class Error(val message: String) : UpdateState()
}
