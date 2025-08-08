package com.claudehooks.dashboard.data.repository

import com.claudehooks.dashboard.data.local.HookDao
import com.claudehooks.dashboard.data.local.SessionSummaryEntity
import com.claudehooks.dashboard.data.local.ToolCountEntity
import com.claudehooks.dashboard.data.remote.RedisService
import com.claudehooks.dashboard.domain.model.*
import com.claudehooks.dashboard.domain.repository.HookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Instant
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HookRepositoryImpl @Inject constructor(
    private val hookDao: HookDao,
    private val redisService: RedisService
) : HookRepository {
    
    override suspend fun connectToRedis(): Boolean {
        return redisService.connect()
    }
    
    override suspend fun disconnectFromRedis() {
        redisService.disconnect()
    }
    
    override fun isConnectedToRedis(): Boolean {
        return redisService.isConnected()
    }
    
    override fun subscribeToRealtimeHooks(): Flow<HookData> {
        return redisService.subscribeToHooks()
            .onEach { hookData ->
                // Cache incoming hook data locally
                try {
                    hookDao.insertHook(hookData)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to cache hook data locally")
                }
            }
            .catch { e ->
                Timber.e(e, "Error in real-time hook subscription")
                throw e
            }
    }
    
    override suspend fun getHooks(limit: Int, offset: Int): List<HookData> {
        return hookDao.getHooks(limit, offset)
    }
    
    override fun getHooksFlow(): Flow<List<HookData>> {
        return hookDao.getHooksFlow()
    }
    
    override suspend fun getHooksBySession(sessionId: String): List<HookData> {
        return hookDao.getHooksBySession(sessionId)
    }
    
    override fun getHooksBySessionFlow(sessionId: String): Flow<List<HookData>> {
        return hookDao.getHooksBySessionFlow(sessionId)
    }
    
    override suspend fun getFilteredHooks(criteria: FilterCriteria): List<HookData> {
        return hookDao.getFilteredHooks(
            sessionId = criteria.sessionId,
            hookTypes = if (criteria.hookTypes.isNotEmpty()) criteria.hookTypes.toList() else null,
            statuses = if (criteria.statuses.isNotEmpty()) criteria.statuses.toList() else null,
            platforms = if (criteria.platforms.isNotEmpty()) criteria.platforms.toList() else null,
            projectTypes = if (criteria.projectTypes.isNotEmpty()) criteria.projectTypes.toList() else null,
            startTime = criteria.startTime?.toString(),
            endTime = criteria.endTime?.toString(),
            searchQuery = criteria.searchQuery,
            toolNames = if (criteria.toolNames.isNotEmpty()) criteria.toolNames.toList() else null
        )
    }
    
    override suspend fun getAllSessionIds(): List<String> {
        return hookDao.getAllSessionIds()
    }
    
    override suspend fun getAllToolNames(): List<String> {
        return hookDao.getAllToolNames()
    }
    
    override suspend fun getSessionSummaries(): List<SessionSummary> {
        val entities = hookDao.getSessionSummaries()
        return entities.map { entity ->
            val hooks = hookDao.getHooksBySession(entity.session_id)
            
            val hookTypeCounts = hooks.groupBy { it.hook_type }.mapValues { it.value.size }
            val statusCounts = hooks.groupBy { it.core.status }.mapValues { it.value.size }
            
            val startTime = entity.start_time?.let { parseInstant(it) }
            val endTime = entity.end_time?.let { parseInstant(it) }
            
            // Check if session is active (has activity in last hour)
            val now = Instant.fromEpochSeconds(System.currentTimeMillis() / 1000)
            val isActive = endTime?.let { 
                (now.epochSeconds - it.epochSeconds) < 3600 
            } ?: false
            
            SessionSummary(
                sessionId = entity.session_id,
                startTime = startTime,
                endTime = endTime,
                totalHooks = entity.total_hooks,
                hookTypeCounts = hookTypeCounts,
                statusCounts = statusCounts,
                averageExecutionTime = entity.avg_execution_time,
                platform = entity.platform,
                projectType = entity.project_type,
                gitBranch = entity.git_branch,
                isActive = isActive
            )
        }
    }
    
    override suspend fun getDashboardStats(): DashboardStats {
        val totalHooks = hookDao.getTotalHooksCount()
        val totalSessions = hookDao.getTotalSessionsCount()
        val activeSessions = hookDao.getActiveSessionsCount()
        
        val allHooks = hookDao.getHooks(Int.MAX_VALUE, 0)
        
        val hookTypeCounts = allHooks.groupBy { it.hook_type }.mapValues { it.value.size }
        val statusCounts = allHooks.groupBy { it.core.status }.mapValues { it.value.size }
        val platformCounts = allHooks.groupBy { it.context.platform }.mapValues { it.value.size }
        val projectTypeCounts = allHooks
            .filter { it.context.project_type != null }
            .groupBy { it.context.project_type!! }
            .mapValues { it.value.size }
        
        val averageExecutionTime = hookDao.getAverageExecutionTime()
        val hooksPerHour = hookDao.getHooksPerHour() ?: 0f
        val successRate = hookDao.getSuccessRate() ?: 0f
        val mostActiveSession = hookDao.getMostActiveSession()?.session_id
        val topToolNames = hookDao.getTopToolNames().map { it.tool_name to it.count }
        
        return DashboardStats(
            totalHooks = totalHooks,
            activeSessions = activeSessions,
            totalSessions = totalSessions,
            hookTypeCounts = hookTypeCounts,
            statusCounts = statusCounts,
            platformCounts = platformCounts,
            projectTypeCounts = projectTypeCounts,
            averageExecutionTime = averageExecutionTime,
            hooksPerHour = hooksPerHour,
            successRate = successRate,
            mostActiveSession = mostActiveSession,
            topToolNames = topToolNames
        )
    }
    
    override suspend fun insertHook(hook: HookData) {
        hookDao.insertHook(hook)
    }
    
    override suspend fun insertHooks(hooks: List<HookData>) {
        hookDao.insertHooks(hooks)
    }
    
    override suspend fun deleteHook(hook: HookData) {
        hookDao.deleteHook(hook)
    }
    
    override suspend fun deleteOldHooks(beforeTime: Instant) {
        hookDao.deleteOldHooks(beforeTime.toString())
    }
    
    override suspend fun clearAllHooks() {
        hookDao.deleteAllHooks()
    }
    
    private fun parseInstant(timestamp: String): Instant? {
        return try {
            Instant.parse(timestamp)
        } catch (e: Exception) {
            // Try parsing as epoch seconds if ISO format fails
            try {
                Instant.fromEpochSeconds(timestamp.toLong())
            } catch (e: Exception) {
                Timber.w(e, "Failed to parse timestamp: $timestamp")
                null
            }
        }
    }
}