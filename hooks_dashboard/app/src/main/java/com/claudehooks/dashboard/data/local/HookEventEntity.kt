package com.claudehooks.dashboard.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.claudehooks.dashboard.data.model.HookType
import com.claudehooks.dashboard.data.model.Severity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant

@Entity(tableName = "hook_events")
@TypeConverters(HookEventConverters::class)
data class HookEventEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "type")
    val type: HookType,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "message")
    val message: String,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Instant,
    
    @ColumnInfo(name = "source")
    val source: String,
    
    @ColumnInfo(name = "severity")
    val severity: Severity,
    
    @ColumnInfo(name = "metadata")
    val metadata: Map<String, String>,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

class HookEventConverters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromHookType(type: HookType): String = type.name
    
    @TypeConverter
    fun toHookType(type: String): HookType = HookType.valueOf(type)
    
    @TypeConverter
    fun fromSeverity(severity: Severity): String = severity.name
    
    @TypeConverter
    fun toSeverity(severity: String): Severity = Severity.valueOf(severity)
    
    @TypeConverter
    fun fromInstant(instant: Instant): Long = instant.toEpochMilli()
    
    @TypeConverter
    fun toInstant(epochMilli: Long): Instant = Instant.ofEpochMilli(epochMilli)
    
    @TypeConverter
    fun fromMetadata(metadata: Map<String, String>): String = gson.toJson(metadata)
    
    @TypeConverter
    fun toMetadata(metadataJson: String): Map<String, String> {
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(metadataJson, type) ?: emptyMap()
    }
    
}