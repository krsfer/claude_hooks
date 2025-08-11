package com.claudehooks.dashboard.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
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
        // Show notifications for NOTIFICATION type events, SESSION_START events, and USER_PROMPT_SUBMIT events
        if (event.type != HookType.NOTIFICATION && 
            event.type != HookType.SESSION_START && 
            event.type != HookType.USER_PROMPT_SUBMIT) {
            return
        }
        
        // Check if this is a system notification
        val isSystemNotification = isSystemNotification(event)
        
        try {
            // For system notifications, use the claudehook channel and show toast
            val channelId = if (isSystemNotification) {
                NotificationChannels.CLAUDEHOOK_CHANNEL_ID
            } else {
                NotificationChannels.getChannelIdForSeverity(event.severity)
            }
            
            // Show toast for system notifications
            if (isSystemNotification) {
                showToastForSystemNotification(event)
            }
            
            // Check if notifications are enabled for this channel
            if (!NotificationChannels.isNotificationChannelEnabled(context, channelId)) {
                Timber.d("Notifications disabled for channel: $channelId")
                return
            }
            
            val notification = createNotification(event, channelId, isSystemNotification)
            val notificationId = generateNotificationId(event)
            
            // Check if we have notification permission (API 33+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (androidx.core.app.ActivityCompat.checkSelfPermission(
                        context, 
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    Timber.w("POST_NOTIFICATIONS permission not granted")
                    return
                }
            }
            
            notificationManager.notify(notificationId, notification)
            
            val notificationType = if (isSystemNotification) "system" else "regular"
            Timber.d("$notificationType notification shown for ${event.type}: ${event.title}")
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to show notification for event: ${event.id}")
        }
    }
    
    private fun isSystemNotification(event: HookEvent): Boolean {
        // Session start and user prompt submit events are always considered system notifications
        if (event.type == HookType.SESSION_START || event.type == HookType.USER_PROMPT_SUBMIT) {
            return true
        }
        
        // Detect system notifications based on title or message content
        val systemKeywords = listOf(
            "system notification",
            "system alert", 
            "system event",
            "claude code",
            "hook system",
            "session started"
        )
        
        val titleLower = event.title.lowercase()
        val messageLower = event.message.lowercase()
        
        return systemKeywords.any { keyword ->
            titleLower.contains(keyword) || messageLower.contains(keyword)
        } || event.source.lowercase().contains("system")
    }
    
    private fun showToastForSystemNotification(event: HookEvent) {
        try {
            val toastMessage = "System: ${event.title}"
            
            // Ensure toast is shown on the main thread
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()
            }
            
            Timber.d("Toast shown for system notification: ${event.title}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to show toast for system notification")
        }
    }
    
    private fun createNotification(event: HookEvent, channelId: String, isSystemNotification: Boolean = false): android.app.Notification {
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
        val title = if (isSystemNotification) "🔔 ${event.title}" else event.title
        val priority = if (isSystemNotification) NotificationCompat.PRIORITY_MAX else getNotificationPriority(event.severity)
        
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(getNotificationIcon(event.severity))
            .setContentTitle(title)
            .setContentText(event.message)
            .setSubText("$timeString • ${event.source}")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(priority)
            .setCategory(if (isSystemNotification) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            
        // Add vibration and wearable support for system notifications
        if (isSystemNotification) {
            // Custom vibration pattern for immediate attention
            // Pattern: wait 0ms, vibrate 800ms, pause 250ms, vibrate 300ms, pause 250ms, vibrate 300ms
            val vibrationPattern = longArrayOf(0, 800, 250, 300, 250, 300)
            builder.setVibrate(vibrationPattern)
            
            // Ensure notification mirrors to wearables
            builder.setLocalOnly(false)
            
            // Use ALL defaults to ensure maximum compatibility
            builder.setDefaults(NotificationCompat.DEFAULT_ALL)
            
            // Set as alert-type notification for wearables
            builder.setOnlyAlertOnce(false)
            
            // Add group to ensure it appears on wearables
            builder.setGroup("claude_system_alerts")
            
            // Set timeout so notification doesn't persist forever
            builder.setTimeoutAfter(30000) // 30 seconds
        }
        
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
        
        // Add color based on severity or system notification
        val color = if (isSystemNotification) 0xFF4CAF50.toInt() else getNotificationColor(event.severity) // Green for system notifications
        builder.color = color
        
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