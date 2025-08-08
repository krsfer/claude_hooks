package com.claudehooks.dashboard.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.claudehooks.dashboard.data.model.Severity

object NotificationChannels {
    
    const val SYSTEM_CHANNEL_ID = "system_notifications"
    const val CRITICAL_CHANNEL_ID = "critical_notifications"
    const val TOOL_CHANNEL_ID = "tool_notifications"
    const val GENERAL_CHANNEL_ID = "general_notifications"
    
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = NotificationManagerCompat.from(context)
            
            // System notifications channel (default priority)
            val systemChannel = NotificationChannel(
                SYSTEM_CHANNEL_ID,
                "System Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Claude Code system notifications and status updates"
                enableLights(true)
                enableVibration(true)
            }
            
            // Critical notifications channel (high priority)
            val criticalChannel = NotificationChannel(
                CRITICAL_CHANNEL_ID,
                "Critical Events",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical events requiring immediate attention"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            
            // Tool notifications channel (low priority)
            val toolChannel = NotificationChannel(
                TOOL_CHANNEL_ID,
                "Tool Events",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tool execution and completion notifications"
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
            }
            
            // General notifications channel (default priority)
            val generalChannel = NotificationChannel(
                GENERAL_CHANNEL_ID,
                "General Events",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General Claude Code hook events"
                enableLights(true)
                enableVibration(false)
            }
            
            // Create all channels
            notificationManager.createNotificationChannels(listOf(
                systemChannel,
                criticalChannel,
                toolChannel,
                generalChannel
            ))
        }
    }
    
    fun getChannelIdForSeverity(severity: Severity): String {
        return when (severity) {
            Severity.CRITICAL -> CRITICAL_CHANNEL_ID
            Severity.ERROR -> CRITICAL_CHANNEL_ID
            Severity.WARNING -> SYSTEM_CHANNEL_ID
            Severity.INFO -> GENERAL_CHANNEL_ID
        }
    }
    
    fun isNotificationChannelEnabled(context: Context, channelId: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = NotificationManagerCompat.from(context)
            val channel = notificationManager.getNotificationChannel(channelId)
            return channel?.importance != NotificationManager.IMPORTANCE_NONE
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}