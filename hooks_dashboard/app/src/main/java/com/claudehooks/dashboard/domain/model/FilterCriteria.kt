package com.claudehooks.dashboard.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.datetime.Instant

@Parcelize
data class FilterCriteria(
    val sessionId: String? = null,
    val hookTypes: Set<HookType> = emptySet(),
    val statuses: Set<HookStatus> = emptySet(),
    val platforms: Set<Platform> = emptySet(),
    val projectTypes: Set<ProjectType> = emptySet(),
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val searchQuery: String? = null,
    val toolNames: Set<String> = emptySet()
) : Parcelable {
    
    fun isEmpty(): Boolean {
        return sessionId.isNullOrBlank() &&
                hookTypes.isEmpty() &&
                statuses.isEmpty() &&
                platforms.isEmpty() &&
                projectTypes.isEmpty() &&
                startTime == null &&
                endTime == null &&
                searchQuery.isNullOrBlank() &&
                toolNames.isEmpty()
    }
    
    fun getActiveFiltersCount(): Int {
        var count = 0
        if (!sessionId.isNullOrBlank()) count++
        if (hookTypes.isNotEmpty()) count++
        if (statuses.isNotEmpty()) count++
        if (platforms.isNotEmpty()) count++
        if (projectTypes.isNotEmpty()) count++
        if (startTime != null || endTime != null) count++
        if (!searchQuery.isNullOrBlank()) count++
        if (toolNames.isNotEmpty()) count++
        return count
    }
}