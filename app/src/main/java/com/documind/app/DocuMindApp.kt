package com.documind.app

import android.app.Application
import android.util.Log
import com.documind.app.data.analytics.AnalyticsManager
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class DocuMindApp : Application() {
    
    companion object {
        private const val TAG = "DocuMindApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        initializeFirebase()
        initializeCrashlytics()
        initializePdfBox()
        initializeAnalytics()
        subscribeFcmTopics()
    }
    
    private fun initializeFirebase() {
        FirebaseApp.initializeApp(this)
        Log.d(TAG, "Firebase initialized")
    }
    
    private fun initializeCrashlytics() {
        val crashlytics = FirebaseCrashlytics.getInstance()
        
        // Enable/disable based on build type
        crashlytics.isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
        
        // Set custom keys for better crash context
        crashlytics.setCustomKey("app_version", BuildConfig.VERSION_NAME)
        crashlytics.setCustomKey("build_type", if (BuildConfig.DEBUG) "debug" else "release")
        
        Log.d(TAG, "Crashlytics initialized (enabled: ${!BuildConfig.DEBUG})")
    }
    
    private fun initializePdfBox() {
        PDFBoxResourceLoader.init(this)
        Log.d(TAG, "PDFBox initialized")
    }
    
    private fun initializeAnalytics() {
        AnalyticsManager.initialize(this)
        AnalyticsManager.logAppOpen()
        Log.d(TAG, "Analytics initialized")
    }
    
    private fun subscribeFcmTopics() {
        // Subscribe to general topic for all users
        FirebaseMessaging.getInstance().subscribeToTopic("all_users")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Subscribed to 'all_users' topic")
                } else {
                    Log.w(TAG, "Failed to subscribe to topic", task.exception)
                }
            }
        
        // Get FCM token for debugging
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "FCM Token: ${task.result}")
            }
        }
    }
}
