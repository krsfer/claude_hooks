package com.claudehooks.dashboard.data.model

import java.time.Instant

/**
 * Performance metrics data model
 */
data class PerformanceMetrics(
    val timestamp: Instant = Instant.now(),
    val memoryUsageMB: Long = 0L,
    val maxMemoryMB: Long = 0L,
    val eventsPerSecond: Float = 0f,
    val connectionLatencyMs: Long = 0L,
    val cacheHitRate: Float = 0f,
    val eventQueueSize: Int = 0,
    val cpuUsagePercent: Float = 0f,
    val networkBytesIn: Long = 0L,
    val networkBytesOut: Long = 0L
) {
    /**
     * Calculate memory usage percentage
     */
    val memoryUsagePercent: Float
        get() = if (maxMemoryMB > 0) (memoryUsageMB.toFloat() / maxMemoryMB.toFloat() * 100f) else 0f
    
    /**
     * Determine memory pressure level
     */
    val memoryPressureLevel: MemoryPressureLevel
        get() = when {
            memoryUsagePercent < 50f -> MemoryPressureLevel.LOW
            memoryUsagePercent < 75f -> MemoryPressureLevel.MEDIUM
            memoryUsagePercent < 90f -> MemoryPressureLevel.HIGH
            else -> MemoryPressureLevel.CRITICAL
        }
    
    /**
     * Determine connection quality based on latency
     */
    val connectionQuality: ConnectionQuality
        get() = when {
            connectionLatencyMs < 50 -> ConnectionQuality.EXCELLENT
            connectionLatencyMs < 100 -> ConnectionQuality.GOOD
            connectionLatencyMs < 200 -> ConnectionQuality.FAIR
            connectionLatencyMs < 500 -> ConnectionQuality.POOR
            else -> ConnectionQuality.VERY_POOR
        }
    
    /**
     * Calculate performance health score (0-100)
     */
    val healthScore: Float
        get() {
            val memoryScore = (100f - memoryUsagePercent).coerceAtLeast(0f)
            val latencyScore = when {
                connectionLatencyMs < 50 -> 100f
                connectionLatencyMs < 100 -> 80f
                connectionLatencyMs < 200 -> 60f
                connectionLatencyMs < 500 -> 40f
                else -> 20f
            }
            val cacheScore = cacheHitRate * 100f
            val cpuScore = (100f - cpuUsagePercent).coerceAtLeast(0f)
            
            return (memoryScore * 0.3f + latencyScore * 0.3f + cacheScore * 0.2f + cpuScore * 0.2f)
        }
}

/**
 * Memory pressure levels for visual feedback
 */
enum class MemoryPressureLevel {
    LOW,        // < 50% memory usage
    MEDIUM,     // 50-75% memory usage
    HIGH,       // 75-90% memory usage
    CRITICAL    // > 90% memory usage
}

/**
 * Connection quality levels based on latency
 */
enum class ConnectionQuality {
    EXCELLENT,  // < 50ms
    GOOD,       // 50-100ms
    FAIR,       // 100-200ms
    POOR,       // 200-500ms
    VERY_POOR   // > 500ms
}

/**
 * Performance trend data point
 */
data class PerformanceTrendPoint(
    val timestamp: Instant,
    val value: Float,
    val label: String = ""
)

/**
 * Historical performance data
 */
data class PerformanceHistory(
    val memoryUsage: List<PerformanceTrendPoint> = emptyList(),
    val eventRate: List<PerformanceTrendPoint> = emptyList(),
    val connectionLatency: List<PerformanceTrendPoint> = emptyList(),
    val healthScore: List<PerformanceTrendPoint> = emptyList(),
    val maxDataPoints: Int = 60 // Keep last 60 data points (1 minute at 1 point/second)
) {
    /**
     * Add new performance metrics and maintain history size
     */
    fun addMetrics(metrics: PerformanceMetrics): PerformanceHistory {
        val timestamp = metrics.timestamp
        
        return copy(
            memoryUsage = (memoryUsage + PerformanceTrendPoint(timestamp, metrics.memoryUsagePercent))
                .takeLast(maxDataPoints),
            eventRate = (eventRate + PerformanceTrendPoint(timestamp, metrics.eventsPerSecond))
                .takeLast(maxDataPoints),
            connectionLatency = (connectionLatency + PerformanceTrendPoint(timestamp, metrics.connectionLatencyMs.toFloat()))
                .takeLast(maxDataPoints),
            healthScore = (healthScore + PerformanceTrendPoint(timestamp, metrics.healthScore))
                .takeLast(maxDataPoints)
        )
    }
    
    /**
     * Get average values over the history
     */
    val averages: PerformanceAverages
        get() = PerformanceAverages(
            memoryUsage = memoryUsage.map { it.value }.average().toFloat(),
            eventRate = eventRate.map { it.value }.average().toFloat(),
            connectionLatency = connectionLatency.map { it.value }.average().toFloat(),
            healthScore = healthScore.map { it.value }.average().toFloat()
        )
}

/**
 * Performance averages for summary display
 */
data class PerformanceAverages(
    val memoryUsage: Float = 0f,
    val eventRate: Float = 0f,
    val connectionLatency: Float = 0f,
    val healthScore: Float = 0f
)