package com.claudehooks.dashboard.di

import com.claudehooks.dashboard.data.repository.HookRepositoryImpl
import com.claudehooks.dashboard.domain.repository.HookRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindHookRepository(
        hookRepositoryImpl: HookRepositoryImpl
    ): HookRepository
}