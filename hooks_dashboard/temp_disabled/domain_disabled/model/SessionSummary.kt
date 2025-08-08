package com.claudehooks.dashboard.domain.model

import kotlinx.datetime.Instant

data class SessionSummary(
    val sessionId: String,
    val startTime: Instant?,
    val endTime: Instant?,
    val totalHooks: Int,
    val hookTypeCounts: Map<HookType, Int>,
    val statusCounts: Map<HookStatus, Int>,
    val averageExecutionTime: Long?,
    val platform: Platform?,
    val projectType: ProjectType?,
    val gitBranch: String?,
    val isActive: Boolean
) {
    val duration: Long?
        get() = if (startTime != null && endTime != null) {
            endTime.epochSeconds - startTime.epochSeconds
        } else null
        
    val successRate: Float
        get() = if (totalHooks > 0) {
            (statusCounts[HookStatus.success] ?: 0).toFloat() / totalHooks
        } else 0f
}