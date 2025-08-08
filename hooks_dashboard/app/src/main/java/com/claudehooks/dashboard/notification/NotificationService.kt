package com.claudehooks.dashboard.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.claudehooks.dashboard.R
import com.claudehooks.dashboard.data.model.HookEvent
import com.claudehooks.dashboard.data.model.HookType
import com.claudehooks.dashboard.data.model.Severity
import com.claudehooks.dashboard.presentation.MainActivity
import timber.log.Timber
import java.time.format.DateTimeFormatter

class NotificationService(private val context: Context) {
    
    companion object {
        private const val NOTIFICATION_ID_BASE = 1000
        private var notificationIdCounter = NOTIFICATION_ID_BASE
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    
    init {
        // Ensure notification channels are created
        NotificationChannels.createNotificationChannels(context)
    }
    
    fun showNotificationForHookEvent(event: HookEvent) {
        // Only show notifications for NOTIFICATION type events as requested
        if (event.type != HookType.NOTIFICATION) {
            return
        }
        
        try {
            val channelId = NotificationChannels.getChannelIdForSeverity(event.severity)
            
            // Check if notifications are enabled for this channel
            if (!NotificationChannels.isNotificationChannelEnabled(context, channelId)) {
                Timber.d("Notifications disabled for channel: $channelId")
                return
            }
            
            val notification = createNotification(event, channelId)
            val notificationId = generateNotificationId(event)
            
            notificationManager.notify(notificationId, notification)
            
            Timber.d("Notification shown for ${event.type}: ${event.title}")
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to show notification for event: ${event.id}")
        }
    }
    
    private fun createNotification(event: HookEvent, channelId: String): android.app.Notification {
        // Create intent to open the app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("event_id", event.id)
            putExtra("session_id", event.metadata["session_id"])
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            event.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Format timestamp for display
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        val timeString = event.timestamp.atZone(java.time.ZoneId.systemDefault()).format(timeFormatter)
        
        // Create notification content
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(getNotificationIcon(event.severity))
            .setContentTitle(event.title)
            .setContentText(event.message)
            .setSubText("$timeString • ${event.source}")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(getNotificationPriority(event.severity))
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        
        // Add expanded style for longer messages
        if (event.message.length > 50) {
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(event.message)
                    .setSummaryText(event.source)
            )
        }
        
        // Add session info if available
        event.metadata["session_id"]?.let { sessionId ->
            builder.addAction(
                android.R.drawable.ic_menu_info_details,
                "Session",
                createSessionPendingIntent(sessionId)
            )
        }
        
        // Add color based on severity
        builder.color = getNotificationColor(event.severity)
        
        return builder.build()
    }
    
    private fun createSessionPendingIntent(sessionId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("filter_session_id", sessionId)
        }
        
        return PendingIntent.getActivity(
            context,
            sessionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    private fun getNotificationIcon(severity: Severity): Int {
        return when (severity) {
            Severity.CRITICAL, Severity.ERROR -> android.R.drawable.ic_dialog_alert
            Severity.WARNING -> android.R.drawable.ic_dialog_info
            Severity.INFO -> android.R.drawable.ic_dialog_info
        }
    }
    
    private fun getNotificationPriority(severity: Severity): Int {
        return when (severity) {
            Severity.CRITICAL, Severity.ERROR -> NotificationCompat.PRIORITY_HIGH
            Severity.WARNING -> NotificationCompat.PRIORITY_DEFAULT
            Severity.INFO -> NotificationCompat.PRIORITY_LOW
        }
    }
    
    private fun getNotificationColor(severity: Severity): Int {
        return when (severity) {
            Severity.CRITICAL -> 0xFFD32F2F.toInt() // Red
            Severity.ERROR -> 0xFFD32F2F.toInt()    // Red
            Severity.WARNING -> 0xFFFF9800.toInt()  // Orange
            Severity.INFO -> 0xFF1976D2.toInt()     // Blue
        }
    }
    
    private fun generateNotificationId(event: HookEvent): Int {
        // Use a combination of event type and timestamp to create unique IDs
        // This allows multiple notifications to coexist
        return (event.type.name.hashCode() + event.timestamp.epochSecond).toInt().and(0x7fffffff)
    }
    
    fun clearNotification(notificationId: Int) {
        try {
            notificationManager.cancel(notificationId)
            Timber.d("Cleared notification: $notificationId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear notification: $notificationId")
        }
    }
    
    fun clearAllNotifications() {
        try {
            notificationManager.cancelAll()
            Timber.d("Cleared all notifications")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear all notifications")
        }
    }
    
    fun areNotificationsEnabled(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }
}