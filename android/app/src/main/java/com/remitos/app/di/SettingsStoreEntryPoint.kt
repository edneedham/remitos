package com.remitos.app.di

import com.remitos.app.data.SettingsStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SettingsStoreEntryPoint {
    fun settingsStore(): SettingsStore
}
