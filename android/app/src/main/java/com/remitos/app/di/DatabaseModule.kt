package com.remitos.app.di

import android.content.Context
import androidx.room.Room
import com.remitos.app.RemitosApplication
import com.remitos.app.data.DatabaseManager
import com.remitos.app.data.RemitosRepository
import com.remitos.app.data.db.AppDatabase
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
    fun provideOfflineDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "remitos_offline"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideRepository(@ApplicationContext context: Context): RemitosRepository {
        return (context.applicationContext as RemitosApplication).requireRepository()
    }

    @Provides
    @Singleton
    fun provideDatabaseManager(): DatabaseManager {
        return DatabaseManager
    }
}
