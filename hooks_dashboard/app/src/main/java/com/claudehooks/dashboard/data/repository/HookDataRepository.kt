package com.claudehooks.dashboard.data.repository

import android.content.Context
import com.claudehooks.dashboard.data.mapper.HookDataMapper
import com.claudehooks.dashboard.data.model.DashboardStats
import com.claudehooks.dashboard.data.model.HookEvent
import com.claudehooks.dashboard.data.model.HookType
import com.claudehooks.dashboard.data.remote.RedisConfig
import com.claudehooks.dashboard.data.remote.RedisService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration.Companion.minutes

class HookDataRepository(
    private val redisConfig: RedisConfig,
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val redisService = RedisService(redisConfig, context)
    
    private val _events = MutableStateFlow<List<HookEvent>>(emptyList())
    val events: StateFlow<List<HookEvent>> = _events.asStateFlow()
    
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Keep events in memory (last 1000 for performance)
    private val eventQueue = ConcurrentLinkedQueue<HookEvent>()
    private val maxEvents = 1000
    
    init {
        startRedisConnection()
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
                        val hookEvent = HookDataMapper.redisToHookEvent(redisData)
                        addEvent(hookEvent)
                        Timber.d("Received hook event: ${hookEvent.type} - ${hookEvent.title}")
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
    }
    
    fun getFilteredEvents(
        typeFilter: Set<HookType> = emptySet(),
        sessionId: String? = null,
        limit: Int = 100
    ): Flow<List<HookEvent>> {
        return events.map { allEvents ->
            var filtered = allEvents
            
            if (typeFilter.isNotEmpty()) {
                filtered = filtered.filter { it.type in typeFilter }
            }
            
            if (sessionId != null) {
                filtered = filtered.filter { it.metadata["session_id"] == sessionId }
            }
            
            filtered.take(limit)
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
    }
}

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}