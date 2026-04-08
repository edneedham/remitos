package com.remitos.app.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.remitos.app.data.ImageUploadManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ImageUploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val imageUploadManager: ImageUploadManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            imageUploadManager.processPendingUploads()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "image_upload_worker"
    }
}
