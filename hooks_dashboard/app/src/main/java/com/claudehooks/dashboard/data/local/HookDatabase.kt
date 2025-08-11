package com.claudehooks.dashboard.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(
    entities = [HookEventEntity::class],
    version = 1,
    exportSchema = false
)
abstract class HookDatabase : RoomDatabase() {
    
    abstract fun hookEventDao(): HookEventDao
    
    companion object {
        @Volatile
        private var INSTANCE: HookDatabase? = null
        
        fun getDatabase(context: Context): HookDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HookDatabase::class.java,
                    "hook_events_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}