package com.remitos.app.di

import com.remitos.app.data.AuthManager
import com.remitos.app.network.RemitosApiService
import com.remitos.app.network.ApiClient
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
    fun provideApiService(authManager: AuthManager): RemitosApiService {
        return ApiClient.getApiService(authManager)
    }
}
