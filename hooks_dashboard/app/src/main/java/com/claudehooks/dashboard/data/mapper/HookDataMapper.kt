package com.claudehooks.dashboard.data.mapper

import com.claudehooks.dashboard.data.model.DashboardStats
import com.claudehooks.dashboard.data.model.HookEvent
import com.claudehooks.dashboard.data.model.HookType
import com.claudehooks.dashboard.data.model.RedisHookData
import com.claudehooks.dashboard.data.model.Severity
import java.time.Instant
import java.time.format.DateTimeFormatter

object HookDataMapper {
    
    fun redisToHookEvent(redisData: RedisHookData): HookEvent {
        return HookEvent(
            id = redisData.id,
            type = mapHookType(redisData.hook_type),
            title = generateTitle(redisData),
            message = generateMessage(redisData),
            timestamp = parseTimestamp(redisData.timestamp),
            source = generateSource(redisData),
            severity = mapSeverity(redisData),
            metadata = buildMetadata(redisData)
        )
    }
    
    private fun mapHookType(hookType: String): HookType {
        return when (hookType.lowercase()) {
            "session_start" -> HookType.SESSION_START
            "user_prompt_submit" -> HookType.USER_PROMPT_SUBMIT
            "pre_tool_use" -> HookType.PRE_TOOL_USE
            "post_tool_use" -> HookType.POST_TOOL_USE
            "notification" -> HookType.NOTIFICATION
            "stop_hook" -> HookType.STOP_HOOK
            "sub_agent_stop_hook" -> HookType.SUB_AGENT_STOP_HOOK
            "pre_compact" -> HookType.PRE_COMPACT
            else -> HookType.CUSTOM
        }
    }
    
    private fun generateTitle(redisData: RedisHookData): String {
        return when (redisData.hook_type) {
            "session_start" -> "Session Started"
            "user_prompt_submit" -> "User Prompt"
            "pre_tool_use" -> "Tool Use: ${redisData.payload.tool_name ?: "Unknown"}"
            "post_tool_use" -> "Tool Completed: ${redisData.payload.tool_name ?: "Unknown"}"
            "notification" -> "Notification: ${redisData.payload.notification_type ?: "System"}"
            "stop_hook" -> "Session Stopped"
            "sub_agent_stop_hook" -> "Sub-Agent Stopped"
            "pre_compact" -> "Pre-Compact: ${redisData.payload.compact_reason ?: "Memory"}"
            else -> "Hook Event"
        }
    }
    
    private fun generateMessage(redisData: RedisHookData): String {
        return when (redisData.hook_type) {
            "session_start" -> "Claude Code session initiated"
            "user_prompt_submit" -> redisData.payload.prompt?.take(100) + 
                if ((redisData.payload.prompt?.length ?: 0) > 100) "..." else ""
            "pre_tool_use" -> "Preparing to execute ${redisData.payload.tool_name}"
            "post_tool_use" -> "Tool execution completed in ${redisData.core.execution_time_ms}ms"
            "notification" -> redisData.payload.message ?: "System notification"
            "stop_hook" -> "Session terminated"
            "sub_agent_stop_hook" -> "Sub-agent task completed"
            "pre_compact" -> "Memory compaction: ${redisData.payload.compact_reason}"
            else -> redisData.payload.message ?: "Hook event occurred"
        } ?: "Event occurred"
    }
    
    private fun generateSource(redisData: RedisHookData): String {
        return when {
            redisData.payload.tool_name != null -> "Tool: ${redisData.payload.tool_name}"
            redisData.context.git_branch != null -> "Git: ${redisData.context.git_branch}"
            redisData.context.platform != null -> "Platform: ${redisData.context.platform}"
            else -> "Claude Code"
        }
    }
    
    private fun mapSeverity(redisData: RedisHookData): Severity {
        return when {
            redisData.core.status == "error" -> Severity.ERROR
            redisData.core.status == "blocked" -> Severity.CRITICAL
            redisData.hook_type == "notification" -> Severity.WARNING
            redisData.core.execution_time_ms > 5000 -> Severity.WARNING
            else -> Severity.INFO
        }
    }
    
    private fun parseTimestamp(timestamp: String): Instant {
        return try {
            // Try ISO 8601 parsing
            Instant.parse(timestamp)
        } catch (e: Exception) {
            try {
                // Try custom format with timezone
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                Instant.from(formatter.parse(timestamp))
            } catch (e2: Exception) {
                // Fallback to current time
                Instant.now()
            }
        }
    }
    
    private fun buildMetadata(redisData: RedisHookData): Map<String, String> {
        val metadata = mutableMapOf<String, String>()
        
        metadata["session_id"] = redisData.session_id
        metadata["sequence"] = redisData.sequence.toString()
        metadata["status"] = redisData.core.status
        metadata["execution_time_ms"] = redisData.core.execution_time_ms.toString()
        
        redisData.context.platform?.let { metadata["platform"] = it }
        redisData.context.git_branch?.let { metadata["git_branch"] = it }
        redisData.context.git_status?.let { metadata["git_status"] = it }
        redisData.context.cwd?.let { metadata["cwd"] = it }
        
        redisData.payload.tool_name?.let { metadata["tool_name"] = it }
        redisData.payload.notification_type?.let { metadata["notification_type"] = it }
        redisData.payload.compact_reason?.let { metadata["compact_reason"] = it }
        
        return metadata
    }
    
    fun calculateDashboardStats(events: List<HookEvent>, sessions: Set<String>): DashboardStats {
        val totalEvents = events.size
        val criticalCount = events.count { it.severity == Severity.CRITICAL }
        val warningCount = events.count { it.severity == Severity.WARNING }
        val errorCount = events.count { it.severity == Severity.ERROR }
        val successCount = events.count { it.severity == Severity.INFO }
        
        val successRate = if (totalEvents > 0) {
            (successCount.toFloat() / totalEvents) * 100
        } else 0f
        
        val recentSessionId = events
            .sortedByDescending { it.timestamp }
            .firstOrNull()
            ?.metadata?.get("session_id")
        
        return DashboardStats(
            totalEvents = totalEvents,
            criticalCount = criticalCount,
            warningCount = warningCount + errorCount,
            successRate = successRate,
            activeHooks = events.distinctBy { it.type }.size,
            activeSessions = sessions,
            recentSessionId = recentSessionId
        )
    }
}