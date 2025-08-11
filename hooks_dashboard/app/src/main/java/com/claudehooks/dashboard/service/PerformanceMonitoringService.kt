package com.claudehooks.dashboard.service

import android.app.ActivityManager
import android.content.Context
import com.claudehooks.dashboard.data.model.PerformanceMetrics
import com.claudehooks.dashboard.data.model.PerformanceHistory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import kotlin.math.min
import kotlin.random.Random

/**
 * Service for monitoring and tracking application performance metrics
 */
class PerformanceMonitoringService(private val context: Context) {
    
    private val instanceId = System.currentTimeMillis().toString().takeLast(4)
    private var scope: CoroutineScope? = null
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    private val _currentMetrics = MutableStateFlow(PerformanceMetrics())
    val currentMetrics: StateFlow<PerformanceMetrics> = _currentMetrics.asStateFlow()
    
    private val _performanceHistory = MutableStateFlow(PerformanceHistory())
    val performanceHistory: StateFlow<PerformanceHistory> = _performanceHistory.asStateFlow()
    
    private var isMonitoring = false
    private var eventCount = 0L
    private var lastEventCountTime = System.currentTimeMillis()
    private var connectionLatencyMs = 0L
    private var cacheHits = 0L
    private var cacheAttempts = 0L
    private var eventQueueSize = 0
    
    // Sliding window for event rate calculation
    private val eventTimestamps = mutableListOf<Long>()
    private val RATE_WINDOW_MS = 5000L // 5 second window
    
    // Average latency tracking
    private val latencyValues = mutableListOf<Long>()
    private val MAX_LATENCY_SAMPLES = 10
    
    /**
     * Check if monitoring is currently active
     */
    fun isMonitoring(): Boolean = isMonitoring
    
    /**
     * Start performance monitoring
     */
    fun startMonitoring() {
        if (isMonitoring) {
            Timber.d("Performance monitoring already running, skipping start")
            return
        }
        
        // Create a new scope if needed
        if (scope == null) {
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        }
        
        isMonitoring = true
        scope?.launch {
            Timber.d("Performance monitoring coroutine started")
            while (isMonitoring) {
                try {
                    collectMetrics()
                    delay(1000) // Collect metrics every second
                } catch (e: Exception) {
                    // Don't log cancellation as error - it's expected during shutdown
                    if (e is CancellationException) {
                        Timber.d("Performance metrics collection cancelled (monitoring stopped)")
                        break // Exit the loop cleanly
                    } else {
                        Timber.e(e, "Error collecting performance metrics")
                        delay(5000) // Wait longer on error
                    }
                }
            }
            Timber.d("Performance monitoring coroutine ended")
        }
        
        Timber.d("Performance monitoring started (instance=$instanceId, isMonitoring=$isMonitoring)")
    }
    
    /**
     * Stop performance monitoring and clean up resources
     */
    fun stopMonitoring() {
        if (!isMonitoring) {
            Timber.d("Performance monitoring not running, skipping stop")
            return
        }
        
        Timber.d("Stopping performance monitoring (instance=$instanceId, isMonitoring was $isMonitoring)")
        isMonitoring = false
        
        // Cancel the coroutine scope to stop all ongoing coroutines
        scope?.cancel("Performance monitoring stopped")
        scope = null // Clear the reference so a new one can be created if needed
        
        // Clear collected data to free memory
        eventTimestamps.clear()
        latencyValues.clear()
        
        Timber.d("Performance monitoring stopped and resources cleaned up (instance=$instanceId, isMonitoring now $isMonitoring)")
    }
    
    /**
     * Update event count for rate calculation
     */
    fun recordEvent() {
        if (!isMonitoring) return // Don't record events when not monitoring
        
        eventCount++
        val now = System.currentTimeMillis()
        eventTimestamps.add(now)
        
        // Clean old timestamps outside the window
        eventTimestamps.removeAll { it < now - RATE_WINDOW_MS }
    }
    
    /**
     * Update connection latency
     */
    fun recordConnectionLatency(latencyMs: Long) {
        if (!isMonitoring) return // Don't record latency when not monitoring
        
        connectionLatencyMs = latencyMs
        
        // Keep a rolling average of latencies
        latencyValues.add(latencyMs)
        if (latencyValues.size > MAX_LATENCY_SAMPLES) {
            latencyValues.removeAt(0)
        }
    }
    
    /**
     * Record cache hit/miss
     */
    fun recordCacheHit(isHit: Boolean) {
        if (!isMonitoring) return // Don't record cache stats when not monitoring
        
        cacheAttempts++
        if (isHit) cacheHits++
    }
    
    /**
     * Update event queue size
     */
    fun updateEventQueueSize(size: Int) {
        if (!isMonitoring) return // Don't update queue size when not monitoring
        
        eventQueueSize = size
    }
    
    /**
     * Collect current performance metrics
     */
    private suspend fun collectMetrics() {
        val runtime = Runtime.getRuntime()
        val currentTime = System.currentTimeMillis()
        
        // Memory metrics
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()
        
        val usedMemoryMB = usedMemory / (1024 * 1024)
        val maxMemoryMB = maxMemory / (1024 * 1024)
        
        // Event rate calculation using sliding window
        val now = System.currentTimeMillis()
        val recentEvents = eventTimestamps.filter { it >= now - RATE_WINDOW_MS }
        val eventsPerSecond = if (RATE_WINDOW_MS > 0) {
            (recentEvents.size.toFloat() / (RATE_WINDOW_MS / 1000f))
        } else 0f
        
        // Use average latency if available
        val avgLatency = if (latencyValues.isNotEmpty()) {
            latencyValues.average().toLong()
        } else {
            connectionLatencyMs
        }
        
        // Cache hit rate
        val cacheHitRate = if (cacheAttempts > 0) {
            cacheHits.toFloat() / cacheAttempts.toFloat()
        } else 0f
        
        // Reset cache stats periodically
        if (cacheAttempts > 100) {
            cacheHits = 0
            cacheAttempts = 0
        }
        
        // CPU usage (simulated - Android doesn't easily provide per-app CPU)
        val cpuUsage = getCpuUsageEstimate()
        
        // Network bytes (simulated)
        val networkBytesIn = getNetworkBytesIn()
        val networkBytesOut = getNetworkBytesOut()
        
        val metrics = PerformanceMetrics(
            timestamp = Instant.now(),
            memoryUsageMB = usedMemoryMB,
            maxMemoryMB = maxMemoryMB,
            eventsPerSecond = eventsPerSecond,
            connectionLatencyMs = avgLatency,
            cacheHitRate = cacheHitRate,
            eventQueueSize = eventQueueSize,
            cpuUsagePercent = cpuUsage,
            networkBytesIn = networkBytesIn,
            networkBytesOut = networkBytesOut
        )
        
        _currentMetrics.value = metrics
        _performanceHistory.value = _performanceHistory.value.addMetrics(metrics)
        
        Timber.v("Performance metrics (instance=$instanceId): Memory=${usedMemoryMB}MB/${maxMemoryMB}MB (${metrics.memoryUsagePercent}%), " +
                "Events/sec=$eventsPerSecond, Latency=${connectionLatencyMs}ms, Cache hit rate=${cacheHitRate * 100}%")
    }
    
    /**
     * Estimate CPU usage based on system metrics and activity
     */
    private fun getCpuUsageEstimate(): Float {
        return try {
            // Simulate CPU usage based on event processing and memory pressure
            val baseUsage = min(20f, eventQueueSize * 0.5f)
            val memoryPressure = (_currentMetrics.value.memoryUsagePercent * 0.1f)
            val eventProcessing = min(30f, _currentMetrics.value.eventsPerSecond * 2f)
            
            (baseUsage + memoryPressure + eventProcessing).coerceIn(0f, 100f)
        } catch (e: Exception) {
            Timber.w(e, "Error estimating CPU usage")
            Random.nextFloat() * 15f // Fallback to low random value
        }
    }
    
    /**
     * Get estimated network bytes received (simulated)
     */
    private fun getNetworkBytesIn(): Long {
        // Simulate based on event processing
        return _currentMetrics.value.eventsPerSecond.toLong() * 200L // ~200 bytes per event
    }
    
    /**
     * Get estimated network bytes sent (simulated)
     */
    private fun getNetworkBytesOut(): Long {
        // Simulate based on API calls and responses
        return _currentMetrics.value.eventsPerSecond.toLong() * 50L // ~50 bytes per response
    }
    
    /**
     * Get formatted memory info string
     */
    fun getMemoryInfoString(): String {
        val metrics = _currentMetrics.value
        return "${metrics.memoryUsageMB}MB / ${metrics.maxMemoryMB}MB (${String.format("%.1f", metrics.memoryUsagePercent)}%)"
    }
    
    /**
     * Get performance summary
     */
    fun getPerformanceSummary(): String {
        val metrics = _currentMetrics.value
        val quality = when (metrics.connectionQuality) {
            com.claudehooks.dashboard.data.model.ConnectionQuality.EXCELLENT -> "Excellent"
            com.claudehooks.dashboard.data.model.ConnectionQuality.GOOD -> "Good"
            com.claudehooks.dashboard.data.model.ConnectionQuality.FAIR -> "Fair"
            com.claudehooks.dashboard.data.model.ConnectionQuality.POOR -> "Poor"
            com.claudehooks.dashboard.data.model.ConnectionQuality.VERY_POOR -> "Very Poor"
        }
        
        return "Health: ${String.format("%.0f", metrics.healthScore)}% | " +
                "Latency: ${metrics.connectionLatencyMs}ms ($quality) | " +
                "Events/sec: ${String.format("%.1f", metrics.eventsPerSecond)}"
    }
}

/**
 * Singleton holder for PerformanceMonitoringService
 */
object PerformanceMonitoringSingleton {
    @Volatile
    private var INSTANCE: PerformanceMonitoringService? = null

    fun getInstance(context: Context): PerformanceMonitoringService {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: PerformanceMonitoringService(context.applicationContext).also { 
                INSTANCE = it
                Timber.d("Created singleton PerformanceMonitoringService instance")
            }
        }
    }
}

/**
 * Extension function to get singleton PerformanceMonitoringService
 */
fun Context.getPerformanceMonitoringService(): PerformanceMonitoringService {
    return PerformanceMonitoringSingleton.getInstance(this)
}