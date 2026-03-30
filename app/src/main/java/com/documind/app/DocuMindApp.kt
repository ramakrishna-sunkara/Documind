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
        
        // Initialize in order with error handling - crash-safe startup
        safeInit("Firebase") { initializeFirebase() }
        safeInit("Crashlytics") { initializeCrashlytics() }
        safeInit("PDFBox") { initializePdfBox() }
        safeInit("Analytics") { initializeAnalytics() }
        safeInit("FCM") { subscribeFcmTopics() }
    }
    
    private inline fun safeInit(name: String, block: () -> Unit) {
        try {
            block()
            Log.d(TAG, "$name initialized")
        } catch (e: Exception) {
            Log.e(TAG, "$name initialization failed: ${e.message}")
        }
    }
    
    private fun initializeFirebase() {
        FirebaseApp.initializeApp(this)
    }
    
    private fun initializeCrashlytics() {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
            crashlytics.setCustomKey("app_version", BuildConfig.VERSION_NAME)
            crashlytics.setCustomKey("build_type", if (BuildConfig.DEBUG) "debug" else "release")
        } catch (e: Exception) {
            Log.e(TAG, "Crashlytics setup error: ${e.message}")
        }
    }
    
    private fun initializePdfBox() {
        PDFBoxResourceLoader.init(this)
    }
    
    private fun initializeAnalytics() {
        AnalyticsManager.initialize(this)
        AnalyticsManager.logAppOpen()
    }
    
    private fun subscribeFcmTopics() {
        FirebaseMessaging.getInstance().subscribeToTopic("all_users")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Subscribed to 'all_users' topic")
                }
            }
        
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "FCM Token obtained")
            }
        }
    }
}
