package com.cs250.kratos.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.cs250.kratos.MainActivity
import com.cs250.kratos.R
import com.cs250.kratos.data.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val authRepo = AuthRepository()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 1. Check for Notification Payload (What you currently have)
        remoteMessage.notification?.let {
            sendNotification(it.title ?: "New Message", it.body ?: "")
        }

        // 2. CRITICAL ADDITION: Check for Data Payload
        // Cloud Functions often send data here to bypass background restrictions
        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"] ?: "New Message"
            val body = remoteMessage.data["body"] ?: remoteMessage.data["message"] ?: "You have a new message"

            // Only send notification if we haven't already sent one from the block above
            if (remoteMessage.notification == null) {
                sendNotification(title, body)
            }
        }
    }


    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        // This is required for newer Android versions
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "kratos_chat_channel_v2"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Use a built-in system icon for testing
            // Make sure you have an icon
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the NotificationChannel, but only on API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            GlobalScope.launch {
                try {
                    authRepo.updateFcmToken(currentUser.uid, token)
                } catch (e: Exception) {
                    // Handle exception
                }
            }
        }
    }
}
