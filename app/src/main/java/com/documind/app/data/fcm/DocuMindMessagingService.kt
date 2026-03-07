package com.documind.app.data.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.documind.app.MainActivity
import com.documind.app.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class DocuMindMessagingService : FirebaseMessagingService() {
    
    companion object {
        private const val TAG = "DocuMindFCM"
        private const val CHANNEL_ID = "documind_notifications"
        private const val CHANNEL_NAME = "DocuMind Notifications"
        private const val CHANNEL_DESCRIPTION = "Updates and tips from DocuMind"
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        // Send token to server if needed for targeted notifications
        sendTokenToServer(token)
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d(TAG, "Message received from: ${remoteMessage.from}")
        
        // Handle data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }
        
        // Handle notification payload (when app is in foreground)
        remoteMessage.notification?.let { notification ->
            showNotification(
                title = notification.title ?: getString(R.string.app_name),
                body = notification.body ?: "",
                data = remoteMessage.data
            )
        }
    }
    
    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"]
        val action = data["action"]
        
        when (type) {
            "update" -> handleUpdateNotification(data)
            "tip" -> handleTipNotification(data)
            "promo" -> handlePromoNotification(data)
            else -> {
                // Show generic notification
                val title = data["title"] ?: getString(R.string.app_name)
                val body = data["body"] ?: ""
                if (body.isNotEmpty()) {
                    showNotification(title, body, data)
                }
            }
        }
    }
    
    private fun handleUpdateNotification(data: Map<String, String>) {
        val title = data["title"] ?: "Update Available"
        val body = data["body"] ?: "A new version of DocuMind is available!"
        showNotification(title, body, data)
    }
    
    private fun handleTipNotification(data: Map<String, String>) {
        val title = data["title"] ?: "DocuMind Tip"
        val body = data["body"] ?: "Did you know you can analyze PDFs, Word docs, and web pages?"
        showNotification(title, body, data)
    }
    
    private fun handlePromoNotification(data: Map<String, String>) {
        val title = data["title"] ?: "DocuMind"
        val body = data["body"] ?: ""
        if (body.isNotEmpty()) {
            showNotification(title, body, data)
        }
    }
    
    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        createNotificationChannel()
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Pass any relevant data
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun sendTokenToServer(token: String) {
        // Future: Send token to your backend server for targeted notifications
        // For now, just log it
        Log.d(TAG, "Token should be sent to server: $token")
    }
}
