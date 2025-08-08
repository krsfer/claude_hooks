package com.claudehooks.dashboard.domain.model

data class DashboardStats(
    val totalHooks: Int,
    val activeSessions: Int,
    val totalSessions: Int,
    val hookTypeCounts: Map<HookType, Int>,
    val statusCounts: Map<HookStatus, Int>,
    val platformCounts: Map<Platform, Int>,
    val projectTypeCounts: Map<ProjectType, Int>,
    val averageExecutionTime: Long?,
    val hooksPerHour: Float,
    val successRate: Float,
    val mostActiveSession: String?,
    val topToolNames: List<Pair<String, Int>>
) {
    companion object {
        fun empty() = DashboardStats(
            totalHooks = 0,
            activeSessions = 0,
            totalSessions = 0,
            hookTypeCounts = emptyMap(),
            statusCounts = emptyMap(),
            platformCounts = emptyMap(),
            projectTypeCounts = emptyMap(),
            averageExecutionTime = null,
            hooksPerHour = 0f,
            successRate = 0f,
            mostActiveSession = null,
            topToolNames = emptyList()
        )
    }
}