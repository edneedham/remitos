package com.remitos.app

import android.app.Application
import com.remitos.app.data.db.AppDatabase

class RemitosApplication : Application() {
    val database: AppDatabase by lazy {
        AppDatabase.build(this)
    }
}
