package com.claudehooks.dashboard

import android.app.Application
import android.util.Log

class HooksApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize basic logging
        Log.d("HooksApp", "HooksApplication started")
    }
}