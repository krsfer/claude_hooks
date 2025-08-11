package com.claudehooks.dashboard

import android.app.Application
import com.claudehooks.dashboard.data.local.HookDatabase
import timber.log.Timber

class ClaudeHooksApplication : Application() {
    
    // Database instance - initialized lazily
    val database by lazy { HookDatabase.getDatabase(this) }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        Timber.plant(Timber.DebugTree())
        
        Timber.d("ClaudeHooksApplication initialized")
        
        // Pre-warm database connection
        initializeDatabase()
    }
    
    private fun initializeDatabase() {
        // Access the database to ensure it's created
        database.hookEventDao()
        Timber.d("Database initialized")
    }
}