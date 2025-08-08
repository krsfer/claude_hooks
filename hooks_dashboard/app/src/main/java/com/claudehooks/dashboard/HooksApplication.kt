package com.claudehooks.dashboard

import android.app.Application
import android.util.Log
import timber.log.Timber

class HooksApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        Timber.plant(Timber.DebugTree())
        
        Timber.d("HooksApplication started")
    }
}