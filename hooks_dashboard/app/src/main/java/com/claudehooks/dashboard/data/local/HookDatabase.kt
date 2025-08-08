package com.claudehooks.dashboard.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import android.content.Context
import com.claudehooks.dashboard.domain.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Database(
    entities = [HookData::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HookDatabase : RoomDatabase() {
    
    abstract fun hookDao(): HookDao
    
    companion object {
        @Volatile
        private var INSTANCE: HookDatabase? = null
        
        fun getDatabase(context: Context): HookDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HookDatabase::class.java,
                    "hook_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromHookType(value: HookType): String = value.name
    
    @TypeConverter
    fun toHookType(value: String): HookType = HookType.valueOf(value)
    
    @TypeConverter
    fun fromHookStatus(value: HookStatus): String = value.name
    
    @TypeConverter
    fun toHookStatus(value: String): HookStatus = HookStatus.valueOf(value)
    
    @TypeConverter
    fun fromPlatform(value: Platform): String = value.name
    
    @TypeConverter
    fun toPlatform(value: String): Platform = Platform.valueOf(value)
    
    @TypeConverter
    fun fromGitStatus(value: GitStatus?): String? = value?.name
    
    @TypeConverter
    fun toGitStatus(value: String?): GitStatus? = value?.let { GitStatus.valueOf(it) }
    
    @TypeConverter
    fun fromProjectType(value: ProjectType?): String? = value?.name
    
    @TypeConverter
    fun toProjectType(value: String?): ProjectType? = value?.let { ProjectType.valueOf(it) }
}