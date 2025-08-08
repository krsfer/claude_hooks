package com.claudehooks.dashboard.data.local

import androidx.room.*
import com.claudehooks.dashboard.domain.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HookDao {
    
    @Query("SELECT * FROM hook_data ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getHooks(limit: Int, offset: Int): List<HookData>
    
    @Query("SELECT * FROM hook_data ORDER BY timestamp DESC")
    fun getHooksFlow(): Flow<List<HookData>>
    
    @Query("SELECT * FROM hook_data WHERE session_id = :sessionId ORDER BY sequence ASC")
    suspend fun getHooksBySession(sessionId: String): List<HookData>
    
    @Query("SELECT * FROM hook_data WHERE session_id = :sessionId ORDER BY sequence ASC")
    fun getHooksBySessionFlow(sessionId: String): Flow<List<HookData>>
    
    @Query("""
        SELECT * FROM hook_data 
        WHERE (:sessionId IS NULL OR session_id = :sessionId)
        AND (:hookTypes IS NULL OR hook_type IN (:hookTypes))
        AND (:statuses IS NULL OR core_status IN (:statuses))
        AND (:platforms IS NULL OR context_platform IN (:platforms))
        AND (:projectTypes IS NULL OR context_project_type IN (:projectTypes))
        AND (:startTime IS NULL OR timestamp >= :startTime)
        AND (:endTime IS NULL OR timestamp <= :endTime)
        AND (:searchQuery IS NULL OR 
             payload_prompt LIKE '%' || :searchQuery || '%' OR
             payload_tool_name LIKE '%' || :searchQuery || '%' OR
             payload_message LIKE '%' || :searchQuery || '%' OR
             session_id LIKE '%' || :searchQuery || '%')
        AND (:toolNames IS NULL OR payload_tool_name IN (:toolNames))
        ORDER BY timestamp DESC
    """)
    suspend fun getFilteredHooks(
        sessionId: String? = null,
        hookTypes: List<HookType>? = null,
        statuses: List<HookStatus>? = null,
        platforms: List<Platform>? = null,
        projectTypes: List<ProjectType>? = null,
        startTime: String? = null,
        endTime: String? = null,
        searchQuery: String? = null,
        toolNames: List<String>? = null
    ): List<HookData>
    
    @Query("SELECT DISTINCT session_id FROM hook_data ORDER BY timestamp DESC")
    suspend fun getAllSessionIds(): List<String>
    
    @Query("SELECT DISTINCT payload_tool_name FROM hook_data WHERE payload_tool_name IS NOT NULL")
    suspend fun getAllToolNames(): List<String>
    
    @Query("""
        SELECT 
            session_id,
            MIN(timestamp) as start_time,
            MAX(timestamp) as end_time,
            COUNT(*) as total_hooks,
            AVG(core_execution_time_ms) as avg_execution_time,
            MAX(context_platform) as platform,
            MAX(context_project_type) as project_type,
            MAX(context_git_branch) as git_branch
        FROM hook_data 
        GROUP BY session_id
        ORDER BY start_time DESC
    """)
    suspend fun getSessionSummaries(): List<SessionSummaryEntity>
    
    @Query("SELECT COUNT(*) FROM hook_data")
    suspend fun getTotalHooksCount(): Int
    
    @Query("SELECT COUNT(DISTINCT session_id) FROM hook_data")
    suspend fun getTotalSessionsCount(): Int
    
    @Query("""
        SELECT COUNT(DISTINCT session_id) 
        FROM hook_data 
        WHERE timestamp >= datetime('now', '-1 hour')
    """)
    suspend fun getActiveSessionsCount(): Int
    
    @Query("SELECT AVG(core_execution_time_ms) FROM hook_data WHERE core_execution_time_ms IS NOT NULL")
    suspend fun getAverageExecutionTime(): Long?
    
    @Query("""
        SELECT COUNT(*) * 1.0 / 
        (CAST(strftime('%s', MAX(timestamp)) AS INTEGER) - CAST(strftime('%s', MIN(timestamp)) AS INTEGER) + 1) * 3600
        FROM hook_data 
        WHERE timestamp >= datetime('now', '-24 hours')
    """)
    suspend fun getHooksPerHour(): Float?
    
    @Query("""
        SELECT COUNT(*) * 1.0 / (SELECT COUNT(*) FROM hook_data)
        FROM hook_data 
        WHERE core_status = 'success'
    """)
    suspend fun getSuccessRate(): Float?
    
    @Query("""
        SELECT session_id, COUNT(*) as count
        FROM hook_data 
        GROUP BY session_id 
        ORDER BY count DESC 
        LIMIT 1
    """)
    suspend fun getMostActiveSession(): SessionCountEntity?
    
    @Query("""
        SELECT payload_tool_name as tool_name, COUNT(*) as count
        FROM hook_data 
        WHERE payload_tool_name IS NOT NULL
        GROUP BY payload_tool_name 
        ORDER BY count DESC 
        LIMIT 10
    """)
    suspend fun getTopToolNames(): List<ToolCountEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHook(hook: HookData)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHooks(hooks: List<HookData>)
    
    @Delete
    suspend fun deleteHook(hook: HookData)
    
    @Query("DELETE FROM hook_data WHERE timestamp < :beforeTime")
    suspend fun deleteOldHooks(beforeTime: String)
    
    @Query("DELETE FROM hook_data")
    suspend fun deleteAllHooks()
}

data class SessionSummaryEntity(
    val session_id: String,
    val start_time: String?,
    val end_time: String?,
    val total_hooks: Int,
    val avg_execution_time: Long?,
    val platform: Platform?,
    val project_type: ProjectType?,
    val git_branch: String?
)

data class SessionCountEntity(
    val session_id: String,
    val count: Int
)

data class ToolCountEntity(
    val tool_name: String,
    val count: Int
)