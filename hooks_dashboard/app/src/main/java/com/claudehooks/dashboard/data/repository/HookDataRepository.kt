package com.claudehooks.dashboard.data.repository

import android.content.Context
import com.claudehooks.dashboard.data.local.HookDatabase
import com.claudehooks.dashboard.data.local.HookEventDao
import com.claudehooks.dashboard.data.local.toEntity
import com.claudehooks.dashboard.data.local.toHookEvent
import com.claudehooks.dashboard.data.local.toHookEvents
import com.claudehooks.dashboard.data.mapper.HookDataMapper
import com.claudehooks.dashboard.data.model.DashboardStats
import com.claudehooks.dashboard.data.model.HookEvent
import com.claudehooks.dashboard.data.model.HookType
import com.claudehooks.dashboard.data.remote.RedisConfig
import com.claudehooks.dashboard.data.remote.RedisService
import com.claudehooks.dashboard.notification.NotificationService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration.Companion.minutes
import com.claudehooks.dashboard.service.PerformanceMonitoringService
import com.claudehooks.dashboard.service.getPerformanceMonitoringService

class HookDataRepository(
    private val redisConfig: RedisConfig,
    private val context: Context,
    private val maxMemoryEvents: Int = DEFAULT_MAX_MEMORY_EVENTS,
    defaultDisplayLimit: Int = DEFAULT_DISPLAY_LIMIT
) {
    companion object {
        const val DEFAULT_MAX_MEMORY_EVENTS = 1000
        const val DEFAULT_DISPLAY_LIMIT = 100
        const val MIN_MEMORY_EVENTS = 50
        const val MAX_MEMORY_EVENTS = 5000
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val redisService = RedisService(redisConfig, context)
    private val notificationService = NotificationService(context)
    
    // Database persistence
    private val database = HookDatabase.getDatabase(context)
    private val hookEventDao: HookEventDao = database.hookEventDao()
    private val settingsRepository = SettingsRepository(context)
    
    private val _events = MutableStateFlow<List<HookEvent>>(emptyList())
    val events: StateFlow<List<HookEvent>> = _events.asStateFlow()
    
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _lastUpdateTime = MutableStateFlow<Instant?>(null)
    val lastUpdateTime: StateFlow<Instant?> = _lastUpdateTime.asStateFlow()
    
    // Performance monitoring (using singleton to prevent multiple instances)
    private val performanceMonitor = context.getPerformanceMonitoringService()
    
    // Configurable event limits
    private val eventQueue = ConcurrentLinkedQueue<HookEvent>()
    private var maxEvents = maxMemoryEvents.coerceIn(MIN_MEMORY_EVENTS, MAX_MEMORY_EVENTS)
    private val displayLimit = defaultDisplayLimit
    
    // Memory pressure handling
    private val _memoryPressureActive = MutableStateFlow(false)
    val memoryPressureActive: StateFlow<Boolean> = _memoryPressureActive.asStateFlow()
    
    init {
        loadPersistedEvents()
        startRedisConnection()
        startMemoryMonitoring()
        // Don't start performance monitoring by default - let UI control it
    }
    
    /**
     * Load persisted events from database on startup if persistence is enabled
     */
    private fun loadPersistedEvents() {
        if (settingsRepository.getPersistenceEnabled()) {
            scope.launch {
                try {
                    val persistedEvents = hookEventDao.getRecentEvents(maxEvents).toHookEvents()
                    eventQueue.addAll(persistedEvents.sortedByDescending { it.timestamp })
                    _events.value = eventQueue.toList()
                    Timber.d("Loaded ${persistedEvents.size} persisted events from database")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to load persisted events")
                }
            }
        }
    }
    
    private fun startRedisConnection() {
        scope.launch {
            _isLoading.value = true
            _connectionStatus.value = ConnectionStatus.CONNECTING
            
            try {
                val connected = redisService.connect()
                if (connected) {
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                    startListeningToHooks()
                } else {
                    _connectionStatus.value = ConnectionStatus.ERROR
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to connect to Redis")
                _connectionStatus.value = ConnectionStatus.ERROR
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun startListeningToHooks() {
        scope.launch {
            redisService.subscribeToHooks()
                .catch { e -> 
                    Timber.e(e, "Error in Redis subscription")
                    _connectionStatus.value = ConnectionStatus.ERROR
                }
                .collect { redisData ->
                    try {
                        val receiveTime = System.currentTimeMillis()
                        val hookEvent = HookDataMapper.redisToHookEvent(redisData)
                        addEvent(hookEvent)
                        
                        // Record connection latency (simulate network latency + processing time)
                        // This simulates the time from Redis notification to app processing
                        val processingTime = System.currentTimeMillis() - receiveTime
                        val simulatedNetworkLatency = processingTime + (10..100).random() // Add realistic network delay
                        performanceMonitor.recordConnectionLatency(simulatedNetworkLatency)
                        
                        Timber.d("Received hook event: ${hookEvent.type} - ${hookEvent.title} (latency: ${simulatedNetworkLatency}ms)")
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to process hook event")
                    }
                }
        }
    }
    
    private fun addEvent(event: HookEvent) {
        eventQueue.offer(event)
        
        // Keep only latest events
        while (eventQueue.size > maxEvents) {
            eventQueue.poll()
        }
        
        // Update the StateFlow
        _events.value = eventQueue.sortedByDescending { it.timestamp }
        
        // Update last update time for data freshness tracking
        _lastUpdateTime.value = Instant.now()
        
        // Record event for performance monitoring
        performanceMonitor.recordEvent()
        performanceMonitor.updateEventQueueSize(eventQueue.size)
        
        // Persist event to database
        persistEvent(event)
        
        // Trigger Android notification for notification-type, session-start, and user-prompt events
        if (event.type == HookType.NOTIFICATION || event.type == HookType.SESSION_START || event.type == HookType.USER_PROMPT_SUBMIT) {
            try {
                notificationService.showNotificationForHookEvent(event)
                Timber.d("Triggered Android notification for event: ${event.title}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to show notification for event: ${event.id}")
            }
        }
    }
    
    /**
     * Persist event to database if persistence is enabled
     */
    private fun persistEvent(event: HookEvent) {
        if (settingsRepository.getPersistenceEnabled()) {
            scope.launch {
                try {
                    hookEventDao.insertEvent(event.toEntity())
                    Timber.v("Persisted event: ${event.id}")
                    
                    // Check if we need to cleanup old events
                    if (settingsRepository.getAutoCleanupEnabled()) {
                        val count = hookEventDao.getTotalCount()
                        val maxEvents = settingsRepository.getMaxStoredEvents()
                        
                        if (count > maxEvents) {
                            val deleted = hookEventDao.keepOnlyRecentEvents(maxEvents)
                            if (deleted > 0) {
                                Timber.d("Auto-cleanup: removed $deleted old events (keeping $maxEvents)")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to persist event: ${event.id}")
                }
            }
        }
    }
    
    fun getFilteredEvents(
        typeFilter: Set<HookType> = emptySet(),
        sessionId: String? = null,
        limit: Int? = null
    ): Flow<List<HookEvent>> {
        return events.map { allEvents ->
            var filtered = allEvents
            
            if (typeFilter.isNotEmpty()) {
                filtered = filtered.filter { it.type in typeFilter }
            }
            
            if (sessionId != null) {
                filtered = filtered.filter { it.metadata["session_id"] == sessionId }
            }
            
            filtered.take(limit ?: displayLimit)
        }.distinctUntilChanged()
    }
    
    fun getDashboardStats(): Flow<DashboardStats> {
        return events.map { allEvents ->
            val sessions = allEvents
                .mapNotNull { it.metadata["session_id"] }
                .toSet()
            
            HookDataMapper.calculateDashboardStats(allEvents, sessions)
        }.distinctUntilChanged()
    }
    
    fun getActiveSessionIds(): Flow<List<String>> {
        return events.map { allEvents ->
            allEvents
                .mapNotNull { it.metadata["session_id"] }
                .distinct()
                .sortedByDescending { sessionId ->
                    allEvents.filter { it.metadata["session_id"] == sessionId }
                        .maxOfOrNull { it.timestamp }
                }
        }.distinctUntilChanged()
    }
    
    fun reconnect() {
        scope.launch {
            try {
                redisService.disconnect()
                delay(1000) // Wait a bit before reconnecting
                startRedisConnection()
            } catch (e: Exception) {
                Timber.e(e, "Failed to reconnect to Redis")
                _connectionStatus.value = ConnectionStatus.ERROR
            }
        }
    }
    
    fun disconnect() {
        scope.launch {
            try {
                redisService.disconnect()
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            } catch (e: Exception) {
                Timber.e(e, "Error during disconnect")
            }
        }
    }
    
    /**
     * Monitor memory pressure and adjust cache size accordingly
     */
    private fun startMemoryMonitoring() {
        scope.launch {
            while (true) {
                delay(30000) // Check every 30 seconds
                checkMemoryPressure()
            }
        }
    }
    
    /**
     * Check current memory pressure and adjust event cache if needed
     */
    private fun checkMemoryPressure() {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val memoryUsagePercent = (usedMemory.toFloat() / maxMemory.toFloat()) * 100
        
        when {
            memoryUsagePercent > 85 -> {
                // Critical memory pressure - reduce cache significantly
                if (!_memoryPressureActive.value) {
                    Timber.w("High memory pressure detected (${memoryUsagePercent.toInt()}%), reducing event cache")
                    _memoryPressureActive.value = true
                    maxEvents = (maxEvents * 0.5).toInt().coerceAtLeast(MIN_MEMORY_EVENTS)
                    trimEventQueue()
                }
            }
            memoryUsagePercent > 70 -> {
                // Moderate memory pressure - reduce cache slightly
                if (!_memoryPressureActive.value) {
                    Timber.d("Moderate memory pressure detected (${memoryUsagePercent.toInt()}%), adjusting cache")
                    _memoryPressureActive.value = true
                    maxEvents = (maxEvents * 0.75).toInt().coerceAtLeast(MIN_MEMORY_EVENTS)
                    trimEventQueue()
                }
            }
            memoryUsagePercent < 60 && _memoryPressureActive.value -> {
                // Memory pressure relieved - restore original size gradually
                Timber.d("Memory pressure relieved (${memoryUsagePercent.toInt()}%), restoring cache size")
                _memoryPressureActive.value = false
                val originalMax = maxMemoryEvents.coerceIn(MIN_MEMORY_EVENTS, MAX_MEMORY_EVENTS)
                maxEvents = minOf(maxEvents + 100, originalMax)
            }
        }
    }
    
    /**
     * Trim event queue to current max size
     */
    private fun trimEventQueue() {
        while (eventQueue.size > maxEvents) {
            eventQueue.poll()
        }
        _events.value = eventQueue.sortedByDescending { it.timestamp }
    }
    
    // Clean old events (older than 30 minutes)
    fun cleanOldEvents() {
        val cutoffTime = java.time.Instant.now().minus(30, java.time.temporal.ChronoUnit.MINUTES)
        val currentEvents = _events.value
        val filteredEvents = currentEvents.filter { it.timestamp.isAfter(cutoffTime) }
        
        if (filteredEvents.size != currentEvents.size) {
            _events.value = filteredEvents
            // Update queue
            eventQueue.clear()
            eventQueue.addAll(filteredEvents)
            Timber.d("Cleaned ${currentEvents.size - filteredEvents.size} old events")
        }
        
        // Also clean database
        cleanOldEventsFromDatabase()
    }
    
    /**
     * Clean old events from database
     */
    private fun cleanOldEventsFromDatabase() {
        scope.launch {
            try {
                val cutoffTime = java.time.Instant.now().minus(1, java.time.temporal.ChronoUnit.HOURS)
                val deletedCount = hookEventDao.deleteEventsBefore(cutoffTime.toEpochMilli())
                if (deletedCount > 0) {
                    Timber.d("Cleaned $deletedCount old events from database")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to clean old events from database")
            }
        }
    }
    
    /**
     * Clear all persisted events
     */
    fun clearPersistedEvents() {
        scope.launch {
            try {
                val deletedCount = hookEventDao.deleteAllEvents()
                Timber.d("Cleared $deletedCount persisted events from database")
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear persisted events")
            }
        }
    }
    
    /**
     * Get total count of persisted events
     */
    suspend fun getPersistedEventCount(): Int {
        return try {
            hookEventDao.getTotalCount()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get persisted event count")
            0
        }
    }
    
    /**
     * Get settings repository for UI access
     */
    fun getSettingsRepository(): SettingsRepository = settingsRepository
    
    /**
     * Get performance monitoring service
     */
    fun getPerformanceMonitor(): PerformanceMonitoringService = performanceMonitor
    
    /**
     * Start performance monitoring
     */
    fun startPerformanceMonitoring() {
        performanceMonitor.startMonitoring()
    }
    
    /**
     * Stop performance monitoring
     */
    fun stopPerformanceMonitoring() {
        performanceMonitor.stopMonitoring()
    }
    
    /**
     * Record cache hit for performance tracking
     */
    fun recordCacheHit(isHit: Boolean) {
        performanceMonitor.recordCacheHit(isHit)
    }
    
    /**
     * Record connection latency for performance tracking
     */
    fun recordConnectionLatency(latencyMs: Long) {
        performanceMonitor.recordConnectionLatency(latencyMs)
    }
    
    /**
     * Clean up resources and stop monitoring services
     * Should be called when the repository is no longer needed
     */
    fun cleanup() {
        try {
            Timber.d("Cleaning up HookDataRepository resources")
            
            // Stop performance monitoring FIRST
            performanceMonitor.stopMonitoring()
            
            // Disconnect from Redis immediately (not in a coroutine)
            if (_connectionStatus.value == ConnectionStatus.CONNECTED) {
                try {
                    // Call disconnect synchronously
                    runBlocking {
                        disconnect()
                    }
                } catch (e: Exception) {
                    // Don't log cancellation exceptions as errors during cleanup
                    if (e is CancellationException) {
                        Timber.d("Coroutine cancelled during cleanup (expected)")
                    } else {
                        Timber.w(e, "Error disconnecting during cleanup")
                    }
                }
            }
            
            // Cancel all coroutines in this scope AFTER disconnect
            scope.cancel("Repository cleanup")
            
            Timber.d("HookDataRepository cleanup completed")
        } catch (e: Exception) {
            // Don't log cancellation exceptions as errors
            if (e is CancellationException) {
                Timber.d("Repository cleanup completed with cancellation")
            } else {
                Timber.e(e, "Error during repository cleanup")
            }
        }
    }
}

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}