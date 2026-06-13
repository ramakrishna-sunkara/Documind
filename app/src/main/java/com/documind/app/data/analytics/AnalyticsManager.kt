package com.documind.app.data.analytics

import android.content.Context
import android.os.Bundle
import com.documind.app.BuildConfig
import com.documind.app.data.extractor.SourceType
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

object AnalyticsManager {
    
    private var firebaseAnalytics: FirebaseAnalytics? = null
    private var isEnabled = BuildConfig.ENABLE_ANALYTICS
    
    fun initialize(context: Context) {
        if (isEnabled) {
            firebaseAnalytics = Firebase.analytics
            firebaseAnalytics?.setAnalyticsCollectionEnabled(true)
        }
    }
    
    fun setAnalyticsEnabled(enabled: Boolean) {
        isEnabled = enabled
        firebaseAnalytics?.setAnalyticsCollectionEnabled(enabled)
    }
    
    // App Lifecycle Events
    fun logAppOpen() {
        logEvent(Events.APP_OPEN, Bundle().apply {
            putString(Params.APP_VERSION, BuildConfig.VERSION_NAME)
            putInt(Params.VERSION_CODE, BuildConfig.VERSION_CODE)
        })
    }

    fun logCrashlyticsSession(versionName: String, versionCode: Int) {
        setUserProperty(UserProperties.APP_VERSION, versionName)
        logEvent(Events.CRASHLYTICS_SESSION, Bundle().apply {
            putString(Params.APP_VERSION, versionName)
            putInt(Params.VERSION_CODE, versionCode)
        })
    }
    
    fun logAppBackground() {
        logEvent(Events.APP_BACKGROUND)
    }
    
    // Document Events
    fun logDocumentLoaded(sourceType: SourceType, wordCount: Int, isLarge: Boolean) {
        logEvent(Events.DOCUMENT_LOADED, Bundle().apply {
            putString(Params.SOURCE_TYPE, sourceType.name)
            putInt(Params.WORD_COUNT, wordCount)
            putBoolean(Params.IS_LARGE_DOCUMENT, isLarge)
        })
    }
    
    fun logDocumentExtractionFailed(sourceType: SourceType, errorMessage: String) {
        logEvent(Events.DOCUMENT_EXTRACTION_FAILED, Bundle().apply {
            putString(Params.SOURCE_TYPE, sourceType.name)
            putString(Params.ERROR_MESSAGE, errorMessage.take(100))
        })
    }
    
    fun logDocumentCleared() {
        logEvent(Events.DOCUMENT_CLEARED)
    }
    
    // Chat Events
    fun logQuerySent(queryLength: Int) {
        logEvent(Events.QUERY_SENT, Bundle().apply {
            putInt(Params.QUERY_LENGTH, queryLength)
        })
    }
    
    fun logQueryResponseReceived(responseLength: Int, durationMs: Long) {
        logEvent(Events.QUERY_RESPONSE_RECEIVED, Bundle().apply {
            putInt(Params.RESPONSE_LENGTH, responseLength)
            putLong(Params.DURATION_MS, durationMs)
        })
    }
    
    fun logQueryFailed(errorMessage: String) {
        logEvent(Events.QUERY_FAILED, Bundle().apply {
            putString(Params.ERROR_MESSAGE, errorMessage.take(100))
        })
    }
    
    // Model Events
    fun logModelInitialized(durationMs: Long) {
        logEvent(Events.MODEL_INITIALIZED, Bundle().apply {
            putLong(Params.DURATION_MS, durationMs)
        })
    }
    
    fun logModelInitFailed(errorMessage: String) {
        logEvent(Events.MODEL_INIT_FAILED, Bundle().apply {
            putString(Params.ERROR_MESSAGE, errorMessage.take(100))
        })
    }
    
    fun logModelSkipped() {
        logEvent(Events.MODEL_SKIPPED)
    }
    
    // Navigation Events
    fun logScreenView(screenName: String) {
        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        })
    }
    
    // Support Events
    fun logSupportOpened() {
        logEvent(Events.SUPPORT_OPENED)
    }
    
    fun logBuyMeCoffeeClicked() {
        logEvent(Events.BUY_ME_COFFEE_CLICKED)
    }
    
    fun logRateAppClicked() {
        logEvent(Events.RATE_APP_CLICKED)
    }
    
    // Share Events
    fun logShareClicked() {
        logEvent(Events.SHARE_CLICKED)
    }
    
    // User Properties
    fun setUserProperty(name: String, value: String) {
        if (isEnabled) {
            firebaseAnalytics?.setUserProperty(name, value)
        }
    }
    
    // Generic event logging with Map params
    fun logEvent(eventName: String, params: Map<String, String>?) {
        if (isEnabled) {
            val bundle = params?.let {
                Bundle().apply {
                    it.forEach { (key, value) -> putString(key, value) }
                }
            }
            firebaseAnalytics?.logEvent(eventName, bundle)
        }
    }
    
    private fun logEvent(eventName: String, params: Bundle? = null) {
        if (isEnabled) {
            firebaseAnalytics?.logEvent(eventName, params)
        }
    }
    
    // Event Names
    object Events {
        const val APP_OPEN = "app_open"
        const val APP_BACKGROUND = "app_background"
        const val CRASHLYTICS_SESSION = "crashlytics_session"
        
        const val DOCUMENT_LOADED = "document_loaded"
        const val DOCUMENT_EXTRACTION_FAILED = "document_extraction_failed"
        const val DOCUMENT_CLEARED = "document_cleared"
        
        const val QUERY_SENT = "query_sent"
        const val QUERY_RESPONSE_RECEIVED = "query_response_received"
        const val QUERY_FAILED = "query_failed"
        
        const val MODEL_INITIALIZED = "model_initialized"
        const val MODEL_INIT_FAILED = "model_init_failed"
        const val MODEL_SKIPPED = "model_skipped"
        
        const val SUPPORT_OPENED = "support_opened"
        const val BUY_ME_COFFEE_CLICKED = "buy_me_coffee_clicked"
        const val RATE_APP_CLICKED = "rate_app_clicked"
        const val SHARE_CLICKED = "share_clicked"
    }
    
    // Parameter Names
    object Params {
        const val SOURCE_TYPE = "source_type"
        const val WORD_COUNT = "word_count"
        const val IS_LARGE_DOCUMENT = "is_large_document"
        const val QUERY_LENGTH = "query_length"
        const val RESPONSE_LENGTH = "response_length"
        const val DURATION_MS = "duration_ms"
        const val ERROR_MESSAGE = "error_message"
        const val APP_VERSION = "app_version"
        const val VERSION_CODE = "version_code"
    }
    
    // User Properties
    object UserProperties {
        const val DOCUMENTS_PROCESSED = "documents_processed"
        const val QUERIES_SENT = "queries_sent"
        const val PREFERRED_SOURCE = "preferred_source"
        const val APP_VERSION = "app_version"
    }
}
