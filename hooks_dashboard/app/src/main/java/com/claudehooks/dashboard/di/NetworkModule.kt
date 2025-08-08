package com.claudehooks.dashboard.di

import com.claudehooks.dashboard.data.remote.RedisConfig
import com.claudehooks.dashboard.data.remote.RedisService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideRedisConfig(): RedisConfig {
        return RedisConfig.fromEnvironment()
    }
    
    @Provides
    @Singleton
    fun provideRedisService(config: RedisConfig): RedisService {
        return RedisService(config)
    }
}