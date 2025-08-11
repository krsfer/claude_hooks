package com.claudehooks.dashboard.presentation.util

import com.claudehooks.dashboard.data.model.FilterState
import com.claudehooks.dashboard.data.model.HookEvent
import com.claudehooks.dashboard.presentation.components.matchesSearchQuery
import com.claudehooks.dashboard.presentation.components.calculateSearchRelevance

/**
 * Filter events based on filter state
 */
fun List<HookEvent>.applyFilters(filterState: FilterState): List<HookEvent> {
    var filtered = this
    
    // Apply type filter
    if (filterState.selectedTypes.isNotEmpty()) {
        filtered = filtered.filter { it.type in filterState.selectedTypes }
    }
    
    // Apply severity filter
    if (filterState.selectedSeverities.isNotEmpty()) {
        filtered = filtered.filter { it.severity in filterState.selectedSeverities }
    }
    
    // Apply session filter
    if (filterState.selectedSessions.isNotEmpty()) {
        filtered = filtered.filter { event ->
            val eventSession = event.metadata["session_id"]
            eventSession != null && eventSession in filterState.selectedSessions
        }
    }
    
    // Apply search query filter
    if (filterState.searchQuery.isNotEmpty()) {
        filtered = filtered.filter { event ->
            matchesSearchQuery(event.title, filterState.searchQuery) ||
            matchesSearchQuery(event.message, filterState.searchQuery) ||
            matchesSearchQuery(event.source, filterState.searchQuery) ||
            event.metadata.values.any { matchesSearchQuery(it, filterState.searchQuery) }
        }
        
        // Sort by search relevance when searching
        filtered = filtered.sortedByDescending { event ->
            val titleRelevance = calculateSearchRelevance(event.title, filterState.searchQuery) * 2f
            val messageRelevance = calculateSearchRelevance(event.message, filterState.searchQuery) * 1.5f
            val sourceRelevance = calculateSearchRelevance(event.source, filterState.searchQuery)
            val metadataRelevance = event.metadata.values.maxOfOrNull { 
                calculateSearchRelevance(it, filterState.searchQuery) 
            } ?: 0f
            
            titleRelevance + messageRelevance + sourceRelevance + metadataRelevance
        }
    }
    
    return filtered
}

/**
 * Extract available sessions from events
 */
fun List<HookEvent>.extractAvailableSessions(): List<String> {
    return mapNotNull { it.metadata["session_id"] }
        .distinct()
        .sortedByDescending { sessionId ->
            // Sort by most recent activity in each session
            filter { it.metadata["session_id"] == sessionId }
                .maxOfOrNull { it.timestamp }
        }
        .take(10) // Limit to 10 most recent sessions
}

/**
 * Get filter statistics
 */
data class FilterStats(
    val totalEvents: Int,
    val filteredEvents: Int,
    val criticalCount: Int,
    val errorCount: Int,
    val warningCount: Int,
    val infoCount: Int,
    val uniqueSessions: Int,
    val searchMatches: Int
)

fun List<HookEvent>.getFilterStats(filterState: FilterState): FilterStats {
    val filtered = applyFilters(filterState)
    
    return FilterStats(
        totalEvents = size,
        filteredEvents = filtered.size,
        criticalCount = filtered.count { it.severity == com.claudehooks.dashboard.data.model.Severity.CRITICAL },
        errorCount = filtered.count { it.severity == com.claudehooks.dashboard.data.model.Severity.ERROR },
        warningCount = filtered.count { it.severity == com.claudehooks.dashboard.data.model.Severity.WARNING },
        infoCount = filtered.count { it.severity == com.claudehooks.dashboard.data.model.Severity.INFO },
        uniqueSessions = filtered.mapNotNull { it.metadata["session_id"] }.distinct().size,
        searchMatches = if (filterState.searchQuery.isNotEmpty()) filtered.size else 0
    )
}