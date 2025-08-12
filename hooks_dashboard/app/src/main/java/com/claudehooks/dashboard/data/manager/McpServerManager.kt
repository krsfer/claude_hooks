package com.claudehooks.dashboard.data.manager

import com.claudehooks.dashboard.data.model.HookEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.time.Instant

data class McpServerState(
    val name: String,
    val isConnected: Boolean = false,
    val lastActivity: Instant? = null,
    val lastError: String? = null,
    val toolCallCount: Int = 0,
    val successCount: Int = 0,
    val errorCount: Int = 0,
    val averageResponseTime: Long = 0L // in milliseconds
)

class McpServerManager {
    
    private val _serverStates = MutableStateFlow<Map<String, McpServerState>>(emptyMap())
    val serverStates: StateFlow<Map<String, McpServerState>> = _serverStates.asStateFlow()
    
    private val _activeServers = MutableStateFlow<Set<String>>(emptySet())
    val activeServers: StateFlow<Set<String>> = _activeServers.asStateFlow()
    
    private val responseTimesMap = mutableMapOf<String, MutableList<Long>>()
    
    fun trackMcpToolCall(event: HookEvent) {
        val serverName = event.metadata["mcp_server"] ?: return
        val isSuccess = event.metadata["error"]?.isNullOrEmpty() ?: true
        val responseTime = event.metadata["response_time"]?.toLongOrNull()
        
        updateServerState(serverName) { state ->
            val updatedResponseTimes = responseTime?.let {
                val times = responseTimesMap.getOrPut(serverName) { mutableListOf() }
                times.add(it)
                if (times.size > 100) times.removeAt(0) // Keep last 100 response times
                times.average().toLong()
            } ?: state.averageResponseTime
            
            state.copy(
                isConnected = true,
                lastActivity = event.timestamp,
                toolCallCount = state.toolCallCount + 1,
                successCount = if (isSuccess) state.successCount + 1 else state.successCount,
                errorCount = if (!isSuccess) state.errorCount + 1 else state.errorCount,
                lastError = if (!isSuccess) event.metadata["error"] else state.lastError,
                averageResponseTime = updatedResponseTimes
            )
        }
        
        // Mark server as active
        _activeServers.value = _activeServers.value + serverName
        
        Timber.d("MCP Server activity tracked - Server: $serverName, Success: $isSuccess, Response time: ${responseTime}ms")
    }
    
    fun trackMcpServerConnection(serverName: String, isConnected: Boolean, error: String? = null) {
        updateServerState(serverName) { state ->
            state.copy(
                isConnected = isConnected,
                lastError = error ?: state.lastError,
                lastActivity = Instant.now()
            )
        }
        
        if (isConnected) {
            _activeServers.value = _activeServers.value + serverName
        } else {
            _activeServers.value = _activeServers.value - serverName
        }
        
        Timber.d("MCP Server connection updated - Server: $serverName, Connected: $isConnected, Error: $error")
    }
    
    fun trackMcpServerError(serverName: String, error: String) {
        updateServerState(serverName) { state ->
            state.copy(
                lastError = error,
                errorCount = state.errorCount + 1,
                lastActivity = Instant.now()
            )
        }
        
        Timber.e("MCP Server error - Server: $serverName, Error: $error")
    }
    
    private fun updateServerState(serverName: String, update: (McpServerState) -> McpServerState) {
        val currentStates = _serverStates.value.toMutableMap()
        val currentState = currentStates[serverName] ?: McpServerState(name = serverName)
        currentStates[serverName] = update(currentState)
        _serverStates.value = currentStates
    }
    
    fun getServerState(serverName: String): McpServerState? {
        return _serverStates.value[serverName]
    }
    
    fun getServerStatistics(): Map<String, ServerStatistics> {
        return _serverStates.value.mapValues { (_, state) ->
            ServerStatistics(
                totalCalls = state.toolCallCount,
                successRate = if (state.toolCallCount > 0) {
                    (state.successCount.toDouble() / state.toolCallCount) * 100
                } else 0.0,
                averageResponseTime = state.averageResponseTime,
                lastActivity = state.lastActivity,
                isActive = state.name in _activeServers.value
            )
        }
    }
    
    fun resetServerState(serverName: String) {
        val currentStates = _serverStates.value.toMutableMap()
        currentStates.remove(serverName)
        _serverStates.value = currentStates
        _activeServers.value = _activeServers.value - serverName
        responseTimesMap.remove(serverName)
        
        Timber.d("MCP Server state reset - Server: $serverName")
    }
    
    fun resetAllServerStates() {
        _serverStates.value = emptyMap()
        _activeServers.value = emptySet()
        responseTimesMap.clear()
        
        Timber.d("All MCP Server states reset")
    }
}

data class ServerStatistics(
    val totalCalls: Int,
    val successRate: Double,
    val averageResponseTime: Long,
    val lastActivity: Instant?,
    val isActive: Boolean
)