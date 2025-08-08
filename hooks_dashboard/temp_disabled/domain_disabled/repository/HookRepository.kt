package com.claudehooks.dashboard.domain.repository

import com.claudehooks.dashboard.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

interface HookRepository {
    
    // Redis connection management
    suspend fun connectToRedis(): Boolean
    suspend fun disconnectFromRedis()
    fun isConnectedToRedis(): Boolean
    
    // Real-time data streaming
    fun subscribeToRealtimeHooks(): Flow<HookData>
    
    // Local data access
    suspend fun getHooks(limit: Int = 100, offset: Int = 0): List<HookData>
    fun getHooksFlow(): Flow<List<HookData>>
    
    // Session management
    suspend fun getHooksBySession(sessionId: String): List<HookData>
    fun getHooksBySessionFlow(sessionId: String): Flow<List<HookData>>
    suspend fun getAllSessionIds(): List<String>
    suspend fun getSessionSummaries(): List<SessionSummary>
    
    // Filtering and search
    suspend fun getFilteredHooks(criteria: FilterCriteria): List<HookData>
    suspend fun getAllToolNames(): List<String>
    
    // Statistics
    suspend fun getDashboardStats(): DashboardStats
    
    // Data management
    suspend fun insertHook(hook: HookData)
    suspend fun insertHooks(hooks: List<HookData>)
    suspend fun deleteHook(hook: HookData)
    suspend fun deleteOldHooks(beforeTime: Instant)
    suspend fun clearAllHooks()
}