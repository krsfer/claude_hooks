package com.claudehooks.dashboard.data

import android.content.Context
import com.claudehooks.dashboard.data.model.DashboardStats
import com.claudehooks.dashboard.data.model.HookEvent
import com.claudehooks.dashboard.data.model.HookType
import com.claudehooks.dashboard.data.model.Severity
import com.claudehooks.dashboard.data.remote.RedisConfig
import com.claudehooks.dashboard.data.repository.HookDataRepository
import com.claudehooks.dashboard.data.repository.TestHookDataRepository
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.random.Random

object DataProvider {
    
    private var _repository: HookDataRepository? = null
    
    fun getRepository(context: Context): HookDataRepository {
        return _repository ?: createRepository(context).also { _repository = it }
    }
    
    private fun createRepository(context: Context): HookDataRepository {
        // Redis configuration from environment
        val config = RedisConfig(
            host = "redis-18773.c311.eu-central-1-1.ec2.redns.redis-cloud.com",
            port = 18773,
            password = "t7H13cIHhR2cm89qyk0y1nRZE2Iy73Pv",
            useTls = true,
            channel = "hooksdata"
        )
        
        return HookDataRepository(config, context)
    }
    
    fun createTestRepository(): TestHookDataRepository {
        return TestHookDataRepository()
    }
    
    // Fallback mock data for offline mode or testing
    fun generateMockHookEvents(): List<HookEvent> {
        val now = Instant.now()
        return listOf(
            HookEvent(
                type = HookType.SESSION_START,
                title = "Session Started",
                message = "Claude Code session initiated",
                timestamp = now.minus(2, ChronoUnit.MINUTES),
                source = "Claude Code",
                severity = Severity.INFO,
                metadata = mapOf("session_id" to "sess-mock-001", "platform" to "darwin")
            ),
            HookEvent(
                type = HookType.USER_PROMPT_SUBMIT,
                title = "User Prompt",
                message = "Help me implement a Redis integration for my Android app...",
                timestamp = now.minus(5, ChronoUnit.MINUTES),
                source = "Claude Code",
                severity = Severity.INFO,
                metadata = mapOf("session_id" to "sess-mock-001", "sequence" to "1")
            ),
            HookEvent(
                type = HookType.PRE_TOOL_USE,
                title = "Tool Use: Read",
                message = "Preparing to execute Read",
                timestamp = now.minus(10, ChronoUnit.MINUTES),
                source = "Tool: Read",
                severity = Severity.INFO,
                metadata = mapOf("session_id" to "sess-mock-001", "tool_name" to "Read")
            ),
            HookEvent(
                type = HookType.POST_TOOL_USE,
                title = "Tool Completed: Read",
                message = "Tool execution completed in 150ms",
                timestamp = now.minus(15, ChronoUnit.MINUTES),
                source = "Tool: Read",
                severity = Severity.INFO,
                metadata = mapOf("session_id" to "sess-mock-001", "execution_time_ms" to "150")
            ),
            HookEvent(
                type = HookType.NOTIFICATION,
                title = "Notification: System",
                message = "Redis connection established successfully",
                timestamp = now.minus(20, ChronoUnit.MINUTES),
                source = "Claude Code",
                severity = Severity.INFO,
                metadata = mapOf("session_id" to "sess-mock-001", "notification_type" to "system")
            ),
            HookEvent(
                type = HookType.PRE_TOOL_USE,
                title = "Tool Use: Edit",
                message = "Preparing to execute Edit",
                timestamp = now.minus(25, ChronoUnit.MINUTES),
                source = "Tool: Edit",
                severity = Severity.INFO,
                metadata = mapOf("session_id" to "sess-mock-002", "tool_name" to "Edit")
            )
        )
    }
    
    fun generateMockStats(): DashboardStats {
        return DashboardStats(
            totalEvents = 24,
            criticalCount = 0,
            warningCount = 2,
            successRate = 95.8f,
            activeHooks = 6,
            activeSessions = setOf("sess-mock-001", "sess-mock-002"),
            recentSessionId = "sess-mock-001"
        )
    }
}