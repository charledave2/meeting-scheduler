package com.example.meetingscheduler

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal object MeetingPushNotifications {
    const val CHANNEL_ID = "meeting_assignments"
    private const val TOKEN_PREFS = "meeting_push_registration"
    private const val TOKEN_KEY = "fcm_token"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Meeting assignments",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications about newly saved meeting assignments"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun savedToken(context: Context): String? =
        context.getSharedPreferences(TOKEN_PREFS, Context.MODE_PRIVATE).getString(TOKEN_KEY, null)

    private fun saveToken(context: Context, token: String) {
        context.getSharedPreferences(TOKEN_PREFS, Context.MODE_PRIVATE)
            .edit().putString(TOKEN_KEY, token).apply()
    }

    fun syncToken(context: Context, api: SchedulerApi) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            saveToken(context, token)
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                runCatching { api.registerPushToken(token) }
            }
        }
    }

    fun saveAndSyncRefreshToken(context: Context, token: String) {
        saveToken(context, token)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { SchedulerApi(context).registerPushToken(token) }
        }
    }

    fun show(context: Context, title: String, body: String, scheduleId: Int? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        createChannel(context)
        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (scheduleId != null && scheduleId > 0) putExtra("scheduleId", scheduleId.toString())
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            scheduleId?.takeIf { it > 0 } ?: 4201,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            Notification.Builder(context)
        }
        val notification = builder
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }
}

class MeetingSchedulerMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        MeetingPushNotifications.saveAndSyncRefreshToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "May bagong bahagi ka sa iskedyul"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: message.data["weekLabel"]
            ?: "Buksan ang JW Meeting Scheduler para tingnan ang iskedyul."
        MeetingPushNotifications.show(applicationContext, title, body, message.data["scheduleId"]?.toIntOrNull())
    }
}
