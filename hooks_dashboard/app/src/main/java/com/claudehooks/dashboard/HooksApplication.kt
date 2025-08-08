package com.claudehooks.dashboard

import android.app.Application
import android.util.Log
import com.claudehooks.dashboard.notification.NotificationChannels
import timber.log.Timber

class HooksApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        Timber.plant(Timber.DebugTree())
        
        // Create notification channels
        NotificationChannels.createNotificationChannels(this)
        
        Timber.d("HooksApplication started with notification channels")
    }
}