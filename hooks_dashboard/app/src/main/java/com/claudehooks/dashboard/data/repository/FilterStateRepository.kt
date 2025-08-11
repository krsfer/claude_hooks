package com.claudehooks.dashboard.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.claudehooks.dashboard.data.model.FilterState
import com.claudehooks.dashboard.data.model.HookType
import com.claudehooks.dashboard.data.model.Severity
import com.claudehooks.dashboard.presentation.components.FilterPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Repository for persisting and managing filter state
 */
class FilterStateRepository(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "filter_state_prefs"
        private const val KEY_SEARCH_QUERY = "search_query"
        private const val KEY_SELECTED_TYPES = "selected_types"
        private const val KEY_SELECTED_SEVERITIES = "selected_severities"
        private const val KEY_SELECTED_SESSIONS = "selected_sessions"
        private const val KEY_ACTIVE_PRESET = "active_preset"
    }
    
    private val sharedPrefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _filterState = MutableStateFlow(loadFilterState())
    val filterState: Flow<FilterState> = _filterState.asStateFlow()
    
    /**
     * Update and persist filter state
     */
    fun updateFilterState(newState: FilterState) {
        _filterState.value = newState
        saveFilterState(newState)
    }
    
    /**
     * Get current filter state
     */
    fun getCurrentFilterState(): FilterState = _filterState.value
    
    /**
     * Clear all filters and persistence
     */
    fun clearAll() {
        val clearedState = FilterState()
        _filterState.value = clearedState
        saveFilterState(clearedState)
    }
    
    /**
     * Load filter state from SharedPreferences
     */
    private fun loadFilterState(): FilterState {
        return try {
            FilterState(
                searchQuery = sharedPrefs.getString(KEY_SEARCH_QUERY, "") ?: "",
                selectedTypes = loadStringSet(KEY_SELECTED_TYPES).mapNotNull { 
                    try { HookType.valueOf(it) } catch (e: Exception) { null }
                }.toSet(),
                selectedSeverities = loadStringSet(KEY_SELECTED_SEVERITIES).mapNotNull {
                    try { Severity.valueOf(it) } catch (e: Exception) { null }
                }.toSet(),
                selectedSessions = loadStringSet(KEY_SELECTED_SESSIONS),
                activePreset = loadActivePreset()
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to load filter state, using defaults")
            FilterState()
        }
    }
    
    /**
     * Save filter state to SharedPreferences
     */
    private fun saveFilterState(state: FilterState) {
        try {
            sharedPrefs.edit()
                .putString(KEY_SEARCH_QUERY, state.searchQuery)
                .putStringSet(KEY_SELECTED_TYPES, state.selectedTypes.map { it.name }.toSet())
                .putStringSet(KEY_SELECTED_SEVERITIES, state.selectedSeverities.map { it.name }.toSet())
                .putStringSet(KEY_SELECTED_SESSIONS, state.selectedSessions)
                .putString(KEY_ACTIVE_PRESET, state.activePreset.javaClass.simpleName)
                .apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save filter state")
        }
    }
    
    /**
     * Load string set from SharedPreferences
     */
    private fun loadStringSet(key: String): Set<String> {
        return sharedPrefs.getStringSet(key, emptySet()) ?: emptySet()
    }
    
    /**
     * Load active preset from SharedPreferences
     */
    private fun loadActivePreset(): FilterPreset {
        val presetName = sharedPrefs.getString(KEY_ACTIVE_PRESET, "None") ?: "None"
        return when (presetName) {
            "CriticalIssues" -> FilterPreset.CriticalIssues
            "RecentActivity" -> FilterPreset.RecentActivity
            "ToolUsage" -> FilterPreset.ToolUsage
            "SessionFlow" -> FilterPreset.SessionFlow
            "Notifications" -> FilterPreset.Notifications
            else -> FilterPreset.None
        }
    }
}

/**
 * Filter preset names for SharedPreferences
 */
fun FilterPreset.getPresetName(): String = when (this) {
    FilterPreset.None -> "None"
    FilterPreset.CriticalIssues -> "CriticalIssues"
    FilterPreset.RecentActivity -> "RecentActivity"
    FilterPreset.ToolUsage -> "ToolUsage"
    FilterPreset.SessionFlow -> "SessionFlow"
    FilterPreset.Notifications -> "Notifications"
}