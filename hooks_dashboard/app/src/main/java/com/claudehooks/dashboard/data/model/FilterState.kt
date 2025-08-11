package com.claudehooks.dashboard.data.model

import com.claudehooks.dashboard.presentation.components.FilterPreset

/**
 * Complete filter state for the dashboard
 */
data class FilterState(
    val searchQuery: String = "",
    val selectedTypes: Set<HookType> = emptySet(),
    val selectedSeverities: Set<Severity> = emptySet(),
    val selectedSessions: Set<String> = emptySet(),
    val activePreset: FilterPreset = FilterPreset.None
) {
    /**
     * Check if any filters are active
     */
    fun hasActiveFilters(): Boolean {
        return searchQuery.isNotEmpty() || 
               selectedTypes.isNotEmpty() || 
               selectedSeverities.isNotEmpty() || 
               selectedSessions.isNotEmpty() ||
               activePreset != FilterPreset.None
    }
    
    /**
     * Clear all filters
     */
    fun clearAll(): FilterState {
        return FilterState()
    }
    
    /**
     * Apply a filter preset
     */
    fun applyPreset(preset: FilterPreset): FilterState {
        return when (preset) {
            FilterPreset.None -> clearAll()
            else -> copy(
                activePreset = preset,
                selectedTypes = preset.types,
                selectedSeverities = preset.severities,
                searchQuery = "", // Clear search when applying preset
                selectedSessions = emptySet() // Clear sessions when applying preset
            )
        }
    }
    
    /**
     * Toggle a hook type filter
     */
    fun toggleType(type: HookType): FilterState {
        return copy(
            selectedTypes = if (type in selectedTypes) {
                selectedTypes - type
            } else {
                selectedTypes + type
            },
            activePreset = FilterPreset.None // Clear preset when manually changing filters
        )
    }
    
    /**
     * Toggle a severity filter
     */
    fun toggleSeverity(severity: Severity): FilterState {
        return copy(
            selectedSeverities = if (severity in selectedSeverities) {
                selectedSeverities - severity
            } else {
                selectedSeverities + severity
            },
            activePreset = FilterPreset.None // Clear preset when manually changing filters
        )
    }
    
    /**
     * Toggle a session filter
     */
    fun toggleSession(sessionId: String): FilterState {
        return copy(
            selectedSessions = if (sessionId in selectedSessions) {
                selectedSessions - sessionId
            } else {
                selectedSessions + sessionId
            },
            activePreset = FilterPreset.None // Clear preset when manually changing filters
        )
    }
    
    /**
     * Update search query
     */
    fun updateSearchQuery(query: String): FilterState {
        return copy(
            searchQuery = query,
            activePreset = if (query.isNotEmpty()) FilterPreset.None else activePreset
        )
    }
}