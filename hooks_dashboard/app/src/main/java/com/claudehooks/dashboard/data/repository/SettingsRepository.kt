package com.claudehooks.dashboard.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

class SettingsRepository(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "claude_hooks_settings"
        private const val KEY_PERSISTENCE_ENABLED = "persistence_enabled"
        private const val KEY_AUTO_CLEANUP_ENABLED = "auto_cleanup_enabled"
        private const val KEY_CLEANUP_HOURS = "cleanup_hours"
        private const val KEY_MAX_STORED_EVENTS = "max_stored_events"
        
        const val DEFAULT_CLEANUP_HOURS = 24
        const val DEFAULT_MAX_STORED_EVENTS = 5000
    }
    
    private val sharedPrefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Settings state flows
    private val _persistenceEnabled = MutableStateFlow(getPersistenceEnabled())
    val persistenceEnabled: Flow<Boolean> = _persistenceEnabled.asStateFlow()
    
    private val _autoCleanupEnabled = MutableStateFlow(getAutoCleanupEnabled())
    val autoCleanupEnabled: Flow<Boolean> = _autoCleanupEnabled.asStateFlow()
    
    private val _cleanupHours = MutableStateFlow(getCleanupHours())
    val cleanupHours: Flow<Int> = _cleanupHours.asStateFlow()
    
    private val _maxStoredEvents = MutableStateFlow(getMaxStoredEvents())
    val maxStoredEvents: Flow<Int> = _maxStoredEvents.asStateFlow()
    
    // Getters
    fun getPersistenceEnabled(): Boolean {
        return sharedPrefs.getBoolean(KEY_PERSISTENCE_ENABLED, true)
    }
    
    fun getAutoCleanupEnabled(): Boolean {
        return sharedPrefs.getBoolean(KEY_AUTO_CLEANUP_ENABLED, true)
    }
    
    fun getCleanupHours(): Int {
        return sharedPrefs.getInt(KEY_CLEANUP_HOURS, DEFAULT_CLEANUP_HOURS)
    }
    
    fun getMaxStoredEvents(): Int {
        return sharedPrefs.getInt(KEY_MAX_STORED_EVENTS, DEFAULT_MAX_STORED_EVENTS)
    }
    
    // Setters
    fun setPersistenceEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_PERSISTENCE_ENABLED, enabled).apply()
        _persistenceEnabled.value = enabled
        Timber.d("Persistence enabled: $enabled")
    }
    
    fun setAutoCleanupEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_AUTO_CLEANUP_ENABLED, enabled).apply()
        _autoCleanupEnabled.value = enabled
        Timber.d("Auto cleanup enabled: $enabled")
    }
    
    fun setCleanupHours(hours: Int) {
        val validHours = hours.coerceIn(1, 168) // 1 hour to 7 days
        sharedPrefs.edit().putInt(KEY_CLEANUP_HOURS, validHours).apply()
        _cleanupHours.value = validHours
        Timber.d("Cleanup hours: $validHours")
    }
    
    fun setMaxStoredEvents(maxEvents: Int) {
        val validMax = maxEvents.coerceIn(100, 50000) // 100 to 50k events
        sharedPrefs.edit().putInt(KEY_MAX_STORED_EVENTS, validMax).apply()
        _maxStoredEvents.value = validMax
        Timber.d("Max stored events: $validMax")
    }
    
    /**
     * Reset all settings to defaults
     */
    fun resetToDefaults() {
        sharedPrefs.edit().clear().apply()
        _persistenceEnabled.value = getPersistenceEnabled()
        _autoCleanupEnabled.value = getAutoCleanupEnabled()
        _cleanupHours.value = getCleanupHours()
        _maxStoredEvents.value = getMaxStoredEvents()
        Timber.d("Settings reset to defaults")
    }
}