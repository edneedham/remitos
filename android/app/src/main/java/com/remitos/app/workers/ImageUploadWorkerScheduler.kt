package com.remitos.app.workers

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ImageUploadWorkerScheduler {

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val uploadWorkRequest = PeriodicWorkRequestBuilder<ImageUploadWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10, TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ImageUploadWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            uploadWorkRequest
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(ImageUploadWorker.WORK_NAME)
    }

    fun isScheduled(context: Context): Boolean {
        val workInfo = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(ImageUploadWorker.WORK_NAME)
            .get()
        return workInfo.isNotEmpty() && workInfo.any { !it.state.isFinished }
    }
}
