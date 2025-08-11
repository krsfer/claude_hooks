package com.claudehooks.dashboard.data.local

import androidx.room.*
import com.claudehooks.dashboard.data.model.HookType
import com.claudehooks.dashboard.data.model.Severity
import kotlinx.coroutines.flow.Flow

@Dao
interface HookEventDao {
    
    @Query("SELECT * FROM hook_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentEvents(limit: Int): List<HookEventEntity>
    
    @Query("SELECT * FROM hook_events ORDER BY timestamp DESC")
    fun getAllEventsFlow(): Flow<List<HookEventEntity>>
    
    @Query("SELECT * FROM hook_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEventsFlow(limit: Int): Flow<List<HookEventEntity>>
    
    @Query("""
        SELECT * FROM hook_events 
        WHERE (:types IS NULL OR type IN (:types))
        AND (:severities IS NULL OR severity IN (:severities))
        AND timestamp >= :afterTime
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getFilteredEvents(
        types: List<HookType>? = null,
        severities: List<Severity>? = null,
        afterTime: Long = 0,
        limit: Int = 1000
    ): List<HookEventEntity>
    
    @Query("""
        SELECT * FROM hook_events 
        WHERE (:types IS NULL OR type IN (:types))
        AND (:severities IS NULL OR severity IN (:severities))
        AND timestamp >= :afterTime
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    fun getFilteredEventsFlow(
        types: List<HookType>? = null,
        severities: List<Severity>? = null,
        afterTime: Long = 0,
        limit: Int = 1000
    ): Flow<List<HookEventEntity>>
    
    @Query("SELECT COUNT(*) FROM hook_events")
    suspend fun getTotalCount(): Int
    
    @Query("SELECT COUNT(*) FROM hook_events WHERE timestamp >= :afterTime")
    suspend fun getCountSince(afterTime: Long): Int
    
    @Query("""
        SELECT COUNT(*) FROM hook_events 
        WHERE severity = 'ERROR' 
        AND timestamp >= :afterTime
    """)
    suspend fun getErrorCountSince(afterTime: Long): Int
    
    @Query("""
        SELECT COUNT(*) FROM hook_events 
        WHERE severity = 'WARNING' 
        AND timestamp >= :afterTime
    """)
    suspend fun getWarningCountSince(afterTime: Long): Int
    
    @Query("SELECT * FROM hook_events WHERE id = :id")
    suspend fun getEventById(id: String): HookEventEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: HookEventEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<HookEventEntity>)
    
    @Update
    suspend fun updateEvent(event: HookEventEntity)
    
    @Delete
    suspend fun deleteEvent(event: HookEventEntity)
    
    @Query("DELETE FROM hook_events WHERE timestamp < :beforeTime")
    suspend fun deleteEventsBefore(beforeTime: Long): Int
    
    @Query("DELETE FROM hook_events")
    suspend fun deleteAllEvents(): Int
    
    @Query("""
        DELETE FROM hook_events 
        WHERE id NOT IN (
            SELECT id FROM hook_events 
            ORDER BY timestamp DESC 
            LIMIT :keepCount
        )
    """)
    suspend fun keepOnlyRecentEvents(keepCount: Int): Int
}