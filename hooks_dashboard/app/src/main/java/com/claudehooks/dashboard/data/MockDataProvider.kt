package com.claudehooks.dashboard.data

import com.claudehooks.dashboard.data.model.DashboardStats
import com.claudehooks.dashboard.data.model.HookEvent
import com.claudehooks.dashboard.data.model.HookType
import com.claudehooks.dashboard.data.model.Severity
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.random.Random

object MockDataProvider {
    
    fun generateMockHookEvents(): List<HookEvent> {
        val now = Instant.now()
        return listOf(
            HookEvent(
                type = HookType.API_CALL,
                title = "User Authentication",
                message = "POST /api/v1/auth/login - Success",
                timestamp = now.minus(2, ChronoUnit.MINUTES),
                source = "AuthService",
                severity = Severity.INFO,
                metadata = mapOf("user" to "john.doe@example.com", "ip" to "192.168.1.100")
            ),
            HookEvent(
                type = HookType.DATABASE,
                title = "Database Query Slow",
                message = "Query execution time exceeded threshold (3.2s)",
                timestamp = now.minus(5, ChronoUnit.MINUTES),
                source = "PostgreSQL",
                severity = Severity.WARNING,
                metadata = mapOf("query" to "SELECT * FROM users", "duration" to "3200ms")
            ),
            HookEvent(
                type = HookType.SECURITY,
                title = "Failed Login Attempt",
                message = "Multiple failed login attempts detected",
                timestamp = now.minus(10, ChronoUnit.MINUTES),
                source = "SecurityMonitor",
                severity = Severity.CRITICAL,
                metadata = mapOf("attempts" to "5", "ip" to "10.0.0.1")
            ),
            HookEvent(
                type = HookType.FILE_SYSTEM,
                title = "File Upload",
                message = "Document uploaded successfully",
                timestamp = now.minus(15, ChronoUnit.MINUTES),
                source = "FileService",
                severity = Severity.INFO,
                metadata = mapOf("filename" to "report.pdf", "size" to "2.3MB")
            ),
            HookEvent(
                type = HookType.NETWORK,
                title = "API Response Time",
                message = "External API response time optimal",
                timestamp = now.minus(20, ChronoUnit.MINUTES),
                source = "NetworkMonitor",
                severity = Severity.INFO,
                metadata = mapOf("endpoint" to "payment-gateway", "latency" to "120ms")
            ),
            HookEvent(
                type = HookType.PERFORMANCE,
                title = "Memory Usage High",
                message = "Application memory usage above 80%",
                timestamp = now.minus(25, ChronoUnit.MINUTES),
                source = "SystemMonitor",
                severity = Severity.WARNING,
                metadata = mapOf("usage" to "82%", "heap" to "1.8GB")
            ),
            HookEvent(
                type = HookType.ERROR,
                title = "Null Pointer Exception",
                message = "NullPointerException in OrderService.processOrder()",
                timestamp = now.minus(30, ChronoUnit.MINUTES),
                source = "OrderService",
                severity = Severity.ERROR,
                metadata = mapOf("stacktrace" to "at OrderService.java:142")
            ),
            HookEvent(
                type = HookType.API_CALL,
                title = "Payment Processed",
                message = "Payment transaction completed successfully",
                timestamp = now.minus(35, ChronoUnit.MINUTES),
                source = "PaymentService",
                severity = Severity.INFO,
                metadata = mapOf("amount" to "$299.99", "method" to "credit_card")
            ),
            HookEvent(
                type = HookType.CUSTOM,
                title = "Cache Cleared",
                message = "Application cache cleared successfully",
                timestamp = now.minus(40, ChronoUnit.MINUTES),
                source = "CacheManager",
                severity = Severity.INFO,
                metadata = mapOf("size_cleared" to "450MB")
            ),
            HookEvent(
                type = HookType.DATABASE,
                title = "Connection Pool Exhausted",
                message = "Database connection pool limit reached",
                timestamp = now.minus(45, ChronoUnit.MINUTES),
                source = "ConnectionPool",
                severity = Severity.ERROR,
                metadata = mapOf("max_connections" to "100", "active" to "100")
            )
        )
    }
    
    fun generateMockStats(): DashboardStats {
        return DashboardStats(
            totalEvents = Random.nextInt(500, 2000),
            criticalCount = Random.nextInt(5, 20),
            warningCount = Random.nextInt(20, 50),
            successRate = Random.nextFloat() * 20 + 80, // 80-100%
            activeHooks = Random.nextInt(10, 30)
        )
    }
}