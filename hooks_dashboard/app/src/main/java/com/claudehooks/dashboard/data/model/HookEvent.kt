package com.claudehooks.dashboard.data.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

// Display model for the dashboard UI
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

// Redis data model matching the claude_hook_redis.sh output
@Serializable
data class RedisHookData(
    val id: String,
    val hook_type: String,
    val timestamp: String,
    val session_id: String,
    val sequence: Long,
    val core: CoreData,
    val payload: PayloadData,
    val context: ContextData,
    val metrics: MetricsData? = null
)

@Serializable
data class CoreData(
    val status: String,
    val execution_time_ms: Long
)

@Serializable
data class PayloadData(
    val prompt: String? = null,
    val tool_name: String? = null,
    val tool_input: String? = null,
    val tool_input_preview: String? = null,
    val tool_response: String? = null,
    val message: String? = null,
    val notification_type: String? = null,
    val compact_reason: String? = null,
    // Additional potential field names for tool information
    val name: String? = null,
    val tool: String? = null,
    val command: String? = null,
    val action: String? = null,
    // Tool-specific parameters that can help identify the tool type
    val file_path: String? = null,
    val content: String? = null,
    val old_string: String? = null,
    val pattern: String? = null,
    val path: String? = null,
    val url: String? = null,
    val query: String? = null,
    // Additional fields from logs
    val prompt_preview: String? = null,
    val prompt_length: Int? = null,
    val success: Boolean? = null,
    val execution_time_ms: Long? = null,
    val output_length: Int? = null,
    val session_ended: Boolean? = null,
    val total_duration_ms: Long? = null,
    val total_tokens: Int? = null,
    val tools_used: Int? = null,
    val reason: String? = null
)

@Serializable
data class ContextData(
    val platform: String,
    val cwd: String? = null,
    val git_branch: String? = null,
    val git_status: String? = null,
    val user_agent: String? = null
)

@Serializable
data class MetricsData(
    val script_version: String? = null
)

enum class HookType {
    SESSION_START,
    USER_PROMPT_SUBMIT,
    PRE_TOOL_USE,
    POST_TOOL_USE,
    NOTIFICATION,
    STOP_HOOK,
    SUB_AGENT_STOP_HOOK,
    PRE_COMPACT,
    // Legacy types for backward compatibility
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
    val activeHooks: Int = 0,
    val activeSessions: Set<String> = emptySet(),
    val recentSessionId: String? = null
)