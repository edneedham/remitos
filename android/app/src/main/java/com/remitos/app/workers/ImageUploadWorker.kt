package com.remitos.app.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.remitos.app.data.ImageUploadManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import java.io.IOException

@HiltWorker
class ImageUploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val imageUploadManager: ImageUploadManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            imageUploadManager.processPendingUploads(forWorker = true)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.w(TAG, "Transient upload failure; will retry", e)
            Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "Image upload worker failed permanently", e)
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "ImageUploadWorker"
        const val WORK_NAME = "image_upload_worker"
    }
}
