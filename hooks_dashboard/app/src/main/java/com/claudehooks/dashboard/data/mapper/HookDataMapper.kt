package com.claudehooks.dashboard.data.mapper

import com.claudehooks.dashboard.data.model.DashboardStats
import com.claudehooks.dashboard.data.model.HookEvent
import com.claudehooks.dashboard.data.model.HookType
import com.claudehooks.dashboard.data.model.PayloadData
import com.claudehooks.dashboard.data.model.RedisHookData
import com.claudehooks.dashboard.data.model.Severity
import timber.log.Timber
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
        val title = when (redisData.hook_type) {
            "session_start" -> "Session Started"
            "user_prompt_submit" -> "User Prompt"
            "pre_tool_use" -> {
                val toolName = extractToolName(redisData.payload)
                Timber.d("pre_tool_use - extracted tool name: '$toolName'")
                if (toolName == "Unknown") "Tool Use" else "Tool Use: $toolName"
            }
            "post_tool_use" -> {
                val toolName = extractToolName(redisData.payload)
                Timber.d("post_tool_use - extracted tool name: '$toolName'")
                val payloadExecTime = redisData.payload.execution_time_ms
                val coreExecTime = redisData.core.execution_time_ms
                // Use core execution time if payload is null or 0, otherwise use payload
                val execTime = if (payloadExecTime != null && payloadExecTime > 0) payloadExecTime else coreExecTime
                Timber.d("post_tool_use - execution times - payload: $payloadExecTime, core: $coreExecTime, final: $execTime")
                if (toolName == "Unknown") "Tool Completed (${execTime}ms)" else "Tool Completed: $toolName (${execTime}ms)"
            }
            "notification" -> "Notification: ${redisData.payload.notification_type ?: "System"}"
            "stop_hook" -> "Session Stopped"
            "sub_agent_stop_hook" -> "Sub-Agent Stopped"
            "pre_compact" -> "Pre-Compact: ${redisData.payload.compact_reason ?: "Memory"}"
            else -> "Hook Event"
        }
        
        Timber.d("Generated title for ${redisData.hook_type}: '$title'")
        return title
    }
    
    private fun generateMessage(redisData: RedisHookData): String {
        return when (redisData.hook_type) {
            "session_start" -> "Claude Code session initiated"
            "user_prompt_submit" -> {
                val prompt = redisData.payload.prompt
                val promptPreview = redisData.payload.prompt_preview
                
                when {
                    !prompt.isNullOrEmpty() -> prompt.take(100) + if (prompt.length > 100) "..." else ""
                    !promptPreview.isNullOrEmpty() && promptPreview != "..." -> 
                        promptPreview.take(100) + if (promptPreview.length > 100) "..." else ""
                    else -> "User submitted prompt"
                }
            }
            "pre_tool_use" -> {
                val toolName = extractToolName(redisData.payload)
                if (toolName == "Unknown") "Preparing to execute tool" else "Preparing to execute $toolName"
            }
            "post_tool_use" -> {
                val payloadExecTime = redisData.payload.execution_time_ms
                val coreExecTime = redisData.core.execution_time_ms
                val execTime = if (payloadExecTime != null && payloadExecTime > 0) payloadExecTime else coreExecTime
                val success = redisData.payload.success ?: true
                val status = if (success) "completed" else "failed"
                
                "Tool execution $status in ${execTime}ms"
            }
            "notification" -> redisData.payload.message ?: "System notification"
            "stop_hook" -> "Session terminated"
            "sub_agent_stop_hook" -> "Sub-agent task completed"
            "pre_compact" -> "Memory compaction: ${redisData.payload.compact_reason}"
            else -> redisData.payload.message ?: "Hook event occurred"
        } ?: "Event occurred"
    }
    
    private fun extractToolName(payload: PayloadData): String {
        // If tool_name is "unknown" or "Unknown", try to extract from other sources
        val toolName = when {
            payload.tool_name != null && 
            payload.tool_name.lowercase() !in listOf("unknown", "null") -> payload.tool_name
            payload.name != null -> payload.name
            payload.tool != null -> payload.tool
            payload.command != null -> "Bash"
            payload.action != null -> payload.action
            // Try direct field extraction first
            payload.file_path != null && payload.content != null -> "Write"
            payload.file_path != null && payload.old_string != null -> "Edit"
            payload.file_path != null -> "Read"
            payload.pattern != null -> "Grep"
            payload.path != null -> "LS"
            payload.url != null -> "WebFetch"
            payload.query != null -> "WebSearch"
            else -> extractToolFromInput(payload.tool_input) 
                ?: extractToolFromInput(payload.tool_input_preview)
                ?: inferToolFromContext(payload)
        }
        
        Timber.d("Tool name extraction - tool_name: '${payload.tool_name}', name: '${payload.name}', " +
                "tool: '${payload.tool}', command: '${payload.command}', action: '${payload.action}', " +
                "tool_input: '${payload.tool_input?.take(50)}...', " +
                "tool_input_preview: '${payload.tool_input_preview?.take(50)}...', result: '$toolName'")
        
        return toolName ?: "Unknown"
    }
    
    private fun extractToolFromInput(toolInput: String?): String? {
        if (toolInput.isNullOrEmpty()) return null
        
        try {
            // Try to extract tool name from tool_input JSON structure
            // Common patterns: {"command": "ls"}, {"file_path": "/path"}, etc.
            val commandMatch = Regex("\"command\"\\s*:\\s*\"([^\"]+)\"").find(toolInput)
            if (commandMatch != null) {
                val command = commandMatch.groupValues[1]
                // Extract just the command name (first word)
                return command.split(" ").firstOrNull()?.takeIf { it.isNotEmpty() }
            }
            
            // Check for specific tool patterns
            when {
                toolInput.contains("\"file_path\"\\s*:".toRegex()) -> return "Read"
                toolInput.contains("\"pattern\"\\s*:".toRegex()) -> return "Grep"
                toolInput.contains("\"path\"\\s*:".toRegex()) -> return "LS"
                toolInput.contains("\"content\"\\s*:".toRegex()) -> return "Write"
                toolInput.contains("\"old_string\"\\s*:".toRegex()) -> return "Edit"
                toolInput.contains("\"url\"\\s*:".toRegex()) -> return "WebFetch"
                toolInput.contains("\"query\"\\s*:".toRegex()) -> return "WebSearch"
                toolInput.contains("\"description\"\\s*:".toRegex()) -> return "Task"
                toolInput.contains("\"prompt\"\\s*:".toRegex()) -> return "UserPrompt"
            }
            
        } catch (e: Exception) {
            Timber.w(e, "Failed to extract tool name from tool_input: $toolInput")
        }
        
        return null
    }
    
    private fun inferToolFromContext(payload: PayloadData): String? {
        // Try to infer tool from context clues in the payload
        return when {
            payload.message?.contains("file", ignoreCase = true) == true -> "File"
            payload.message?.contains("command", ignoreCase = true) == true -> "Bash"
            payload.message?.contains("search", ignoreCase = true) == true -> "Search"
            payload.message?.contains("web", ignoreCase = true) == true -> "Web"
            payload.message?.contains("edit", ignoreCase = true) == true -> "Edit"
            payload.message?.contains("write", ignoreCase = true) == true -> "Write"
            payload.message?.contains("read", ignoreCase = true) == true -> "Read"
            else -> null
        }
    }
    
    private fun generateSource(redisData: RedisHookData): String {
        return when {
            extractToolName(redisData.payload) != "Unknown" -> "Tool: ${extractToolName(redisData.payload)}"
            !redisData.context.git_branch.isNullOrEmpty() -> "Git: ${redisData.context.git_branch}"
            !redisData.context.platform.isNullOrEmpty() -> "Platform: ${redisData.context.platform}"
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