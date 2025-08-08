package com.claudehooks.dashboard.data.model

import java.time.Instant
import java.util.UUID

data class HookEvent(
    val id: String = UUID.randomUUID().toString(),
    val type: HookType,
    val title: String,
    val message: String,
    val timestamp: Instant = Instant.now(),
    val source: String,
    val severity: Severity = Severity.INFO,
    val metadata: Map<String, String> = emptyMap()
)

enum class HookType {
    API_CALL,
    DATABASE,
    FILE_SYSTEM,
    NETWORK,
    SECURITY,
    PERFORMANCE,
    ERROR,
    CUSTOM
}

enum class Severity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

data class DashboardStats(
    val totalEvents: Int = 0,
    val criticalCount: Int = 0,
    val warningCount: Int = 0,
    val successRate: Float = 0f,
    val activeHooks: Int = 0
)