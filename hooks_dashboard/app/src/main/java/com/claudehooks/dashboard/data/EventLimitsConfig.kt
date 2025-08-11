package com.claudehooks.dashboard.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Configuration for event storage and display limits
 */
class EventLimitsConfig(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "event_limits_prefs",
        Context.MODE_PRIVATE
    )
    
    companion object {
        // Environment variable names
        const val ENV_MAX_MEMORY_EVENTS = "MAX_MEMORY_EVENTS"
        const val ENV_MAX_DISPLAY_EVENTS = "MAX_DISPLAY_EVENTS"
        
        // SharedPreferences keys
        private const val KEY_MAX_MEMORY_EVENTS = "max_memory_events"
        private const val KEY_MAX_DISPLAY_EVENTS = "max_display_events"
        
        // Default values
        const val DEFAULT_MAX_MEMORY_EVENTS = 1000
        const val DEFAULT_MAX_DISPLAY_EVENTS = 100
        
        // Limits
        const val MIN_MEMORY_EVENTS = 50
        const val MAX_MEMORY_EVENTS = 5000
        const val MIN_DISPLAY_EVENTS = 25
        const val MAX_DISPLAY_EVENTS = 500
    }
    
    /**
     * Get maximum number of events to keep in memory
     * Priority: Environment variable > SharedPreferences > Default
     */
    fun getMaxMemoryEvents(): Int {
        // First check environment variable
        System.getenv(ENV_MAX_MEMORY_EVENTS)?.toIntOrNull()?.let { envValue ->
            return envValue.coerceIn(MIN_MEMORY_EVENTS, MAX_MEMORY_EVENTS)
        }
        
        // Then check SharedPreferences
        if (prefs.contains(KEY_MAX_MEMORY_EVENTS)) {
            return prefs.getInt(KEY_MAX_MEMORY_EVENTS, DEFAULT_MAX_MEMORY_EVENTS)
                .coerceIn(MIN_MEMORY_EVENTS, MAX_MEMORY_EVENTS)
        }
        
        // Return default
        return DEFAULT_MAX_MEMORY_EVENTS
    }
    
    /**
     * Get maximum number of events to display in UI
     * Priority: Environment variable > SharedPreferences > Default
     */
    fun getMaxDisplayEvents(): Int {
        // First check environment variable
        System.getenv(ENV_MAX_DISPLAY_EVENTS)?.toIntOrNull()?.let { envValue ->
            return envValue.coerceIn(MIN_DISPLAY_EVENTS, MAX_DISPLAY_EVENTS)
        }
        
        // Then check SharedPreferences
        if (prefs.contains(KEY_MAX_DISPLAY_EVENTS)) {
            return prefs.getInt(KEY_MAX_DISPLAY_EVENTS, DEFAULT_MAX_DISPLAY_EVENTS)
                .coerceIn(MIN_DISPLAY_EVENTS, MAX_DISPLAY_EVENTS)
        }
        
        // Return default
        return DEFAULT_MAX_DISPLAY_EVENTS
    }
    
    /**
     * Save max memory events to SharedPreferences
     */
    fun setMaxMemoryEvents(value: Int) {
        val coercedValue = value.coerceIn(MIN_MEMORY_EVENTS, MAX_MEMORY_EVENTS)
        prefs.edit().putInt(KEY_MAX_MEMORY_EVENTS, coercedValue).apply()
    }
    
    /**
     * Save max display events to SharedPreferences
     */
    fun setMaxDisplayEvents(value: Int) {
        val coercedValue = value.coerceIn(MIN_DISPLAY_EVENTS, MAX_DISPLAY_EVENTS)
        prefs.edit().putInt(KEY_MAX_DISPLAY_EVENTS, coercedValue).apply()
    }
    
    /**
     * Get suggested memory limit based on available RAM
     */
    fun getSuggestedMemoryLimit(context: Context): Int {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory() / (1024 * 1024) // Convert to MB
        
        return when {
            maxMemory < 128 -> 500  // Low memory device
            maxMemory < 256 -> 1000 // Standard memory (default)
            maxMemory < 512 -> 2000 // High memory
            else -> 3000 // Very high memory
        }.coerceIn(MIN_MEMORY_EVENTS, MAX_MEMORY_EVENTS)
    }
    
    /**
     * Reset to default values
     */
    fun resetToDefaults() {
        prefs.edit()
            .remove(KEY_MAX_MEMORY_EVENTS)
            .remove(KEY_MAX_DISPLAY_EVENTS)
            .apply()
    }
}