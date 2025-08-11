package com.claudehooks.dashboard.data.repository

import android.content.Context
import com.claudehooks.dashboard.data.model.DashboardStats
import com.claudehooks.dashboard.data.model.HookEvent
import com.claudehooks.dashboard.data.model.HookType
import com.claudehooks.dashboard.service.ConnectionStatus
import com.claudehooks.dashboard.data.repository.ConnectionStatus as RepoConnectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.Instant
import com.claudehooks.dashboard.service.PerformanceMonitoringService

/**
 * Repository interface for background service operations
 * This provides a clean abstraction for the dashboard to interact with
 * background services that monitor Claude Code hooks
 */
interface BackgroundServiceRepository {
    /**
     * Connection status to the Redis service
     */
    val connectionStatus: StateFlow<ConnectionStatus>
    
    /**
     * Dashboard statistics flow
     */
    val dashboardStats: StateFlow<DashboardStats>
    
    /**
     * Last update time for data freshness tracking
     */
    val lastUpdateTime: StateFlow<Instant?>
    
    /**
     * Get filtered events based on selected hook types
     */
    fun getFilteredEvents(typeFilter: Set<HookType> = emptySet()): Flow<List<HookEvent>>
    
    /**
     * Attempt to reconnect to the service
     */
    suspend fun reconnect()
    
    /**
     * Disconnect from the service
     */
    suspend fun disconnect()
    
    /**
     * Get settings repository for persistence settings
     */
    fun getSettingsRepository(): SettingsRepository
    
    /**
     * Get count of persisted events
     */
    suspend fun getPersistedEventCount(): Int
    
    /**
     * Clear all persisted events
     */
    fun clearPersistedEvents()
    
    /**
     * Get performance monitoring service
     */
    fun getPerformanceMonitor(): PerformanceMonitoringService?
    
    /**
     * Start performance monitoring
     */
    fun startPerformanceMonitoring()
    
    /**
     * Stop performance monitoring
     */
    fun stopPerformanceMonitoring()
}

/**
 * Implementation of BackgroundServiceRepository that wraps the existing HookDataRepository
 * This allows the dashboard to work with both direct repositories and background services
 * using the same interface
 */
class BackgroundServiceRepositoryImpl(
    private val hookDataRepository: HookDataRepository
) : BackgroundServiceRepository {
    
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    private val _dashboardStats = MutableStateFlow(DashboardStats())
    private val _lastUpdateTime = MutableStateFlow<Instant?>(null)
    
    init {
        // Convert repository status to service status
        CoroutineScope(Dispatchers.Main).launch {
            hookDataRepository.connectionStatus.collect { repoStatus ->
                _connectionStatus.value = when (repoStatus) {
                    RepoConnectionStatus.DISCONNECTED -> ConnectionStatus.DISCONNECTED
                    RepoConnectionStatus.CONNECTING -> ConnectionStatus.CONNECTING
                    RepoConnectionStatus.CONNECTED -> ConnectionStatus.CONNECTED
                    RepoConnectionStatus.ERROR -> ConnectionStatus.ERROR
                }
            }
        }
        
        // Convert dashboard stats flow to state flow
        CoroutineScope(Dispatchers.Main).launch {
            hookDataRepository.getDashboardStats().collect {
                _dashboardStats.value = it
            }
        }
        
        // Track last update time from events
        CoroutineScope(Dispatchers.Main).launch {
            hookDataRepository.getFilteredEvents(emptySet()).collect { events ->
                if (events.isNotEmpty()) {
                    _lastUpdateTime.value = Instant.now()
                }
            }
        }
    }
    
    override val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus
    override val dashboardStats: StateFlow<DashboardStats> = _dashboardStats
    override val lastUpdateTime: StateFlow<Instant?> = _lastUpdateTime
    
    override fun getFilteredEvents(typeFilter: Set<HookType>): Flow<List<HookEvent>> {
        return hookDataRepository.getFilteredEvents(typeFilter)
    }
    
    override suspend fun reconnect() {
        hookDataRepository.reconnect()
    }
    
    override suspend fun disconnect() {
        hookDataRepository.disconnect()
    }
    
    override fun getSettingsRepository(): SettingsRepository {
        return hookDataRepository.getSettingsRepository()
    }
    
    override suspend fun getPersistedEventCount(): Int {
        return hookDataRepository.getPersistedEventCount()
    }
    
    override fun clearPersistedEvents() {
        hookDataRepository.clearPersistedEvents()
    }
    
    override fun getPerformanceMonitor(): PerformanceMonitoringService {
        return hookDataRepository.getPerformanceMonitor()
    }
    
    override fun startPerformanceMonitoring() {
        hookDataRepository.startPerformanceMonitoring()
    }
    
    override fun stopPerformanceMonitoring() {
        hookDataRepository.stopPerformanceMonitoring()
    }
}