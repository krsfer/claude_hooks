package com.claudehooks.dashboard.di

import android.content.Context
import androidx.room.Room
import com.claudehooks.dashboard.data.local.HookDao
import com.claudehooks.dashboard.data.local.HookDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideHookDatabase(
        @ApplicationContext context: Context
    ): HookDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            HookDatabase::class.java,
            "hook_database"
        )
        .fallbackToDestructiveMigration() // For development - remove in production
        .build()
    }
    
    @Provides
    fun provideHookDao(database: HookDatabase): HookDao {
        return database.hookDao()
    }
}