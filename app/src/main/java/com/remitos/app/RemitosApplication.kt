package com.remitos.app

import android.app.Application
import com.remitos.app.data.RemitosRepository
import com.remitos.app.data.SettingsStore
import com.remitos.app.data.db.AppDatabase

class RemitosApplication : Application() {
    val database: AppDatabase by lazy {
        AppDatabase.build(this)
    }

    val repository: RemitosRepository by lazy {
        RemitosRepository(database)
    }

    val settingsStore: SettingsStore by lazy {
        SettingsStore(this)
    }
}
