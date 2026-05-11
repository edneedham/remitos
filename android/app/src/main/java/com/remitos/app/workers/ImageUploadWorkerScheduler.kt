package com.remitos.app.workers

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.remitos.app.data.FeatureFlags
import java.util.concurrent.TimeUnit

object ImageUploadWorkerScheduler {

    /** WorkManager enforces a 15-minute minimum interval for periodic work. */
    private const val MIN_PERIODIC_INTERVAL_MINUTES = 15L

    /**
     * Enqueue or replace the periodic upload worker using [FeatureFlags.syncIntervalMinutes],
     * clamped to [MIN_PERIODIC_INTERVAL_MINUTES].
     */
    fun scheduleOrUpdate(context: Context) {
        val intervalMinutes =
            MIN_PERIODIC_INTERVAL_MINUTES.coerceAtLeast(FeatureFlags.syncIntervalMinutes.toLong())

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val uploadWorkRequest = PeriodicWorkRequestBuilder<ImageUploadWorker>(
            intervalMinutes,
            TimeUnit.MINUTES,
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.MINUTES,
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ImageUploadWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            uploadWorkRequest,
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
