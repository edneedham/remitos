package com.remitos.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import com.remitos.app.data.db.entity.UploadStatus
import com.remitos.app.network.RemitosApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for uploading remito images to Google Cloud Storage.
 * Handles upload queuing, retry logic, and cleanup.
 */
@Singleton
class ImageUploadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: RemitosRepository,
    private val settingsStore: SettingsStore,
    private val apiService: RemitosApiService,
) {
    companion object {
        private const val TAG = "ImageUploadManager"
        private const val MAX_RETRY_COUNT = 3
    }

    /**
     * Enqueue an image for upload. Will check upload timing settings and
     * upload immediately or queue for later depending on WiFi availability.
     *
     * @param noteId The ID of the inbound note
     * @param imageUri The local URI of the image to upload
     * @param timing The upload timing setting to use (defaults to current setting)
     */
    suspend fun enqueueUpload(
        noteId: Long,
        imageUri: Uri,
        timing: UploadTiming? = null
    ) {
        val uploadTiming = timing ?: settingsStore.getUploadTiming()
        val isWifiConnected = isWifiConnected()

        when (uploadTiming) {
            UploadTiming.IMMEDIATE -> {
                // Upload immediately regardless of network type
                uploadImage(noteId, imageUri)
            }
            UploadTiming.WIFI_ONLY -> {
                if (isWifiConnected) {
                    // Upload now since we're on WiFi
                    uploadImage(noteId, imageUri)
                } else {
                    // Queue for later upload when WiFi is available
                    queueForWifiUpload(noteId, imageUri)
                }
            }
        }
    }

    /**
     * Upload a single image to GCS.
     */
    private suspend fun uploadImage(noteId: Long, imageUri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                // Update status to uploading
                repository.updateUploadStatus(noteId, UploadStatus.UPLOADING)

                // Get the file from URI
                val file = uriToFile(imageUri) ?: run {
                    Log.e(TAG, "Could not convert URI to file: $imageUri")
                    repository.updateUploadStatus(noteId, UploadStatus.FAILED)
                    return@withContext
                }

                // Create multipart request
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData(
                    "image",
                    file.name,
                    requestFile
                )
                val entityTypePart = "inbound_note".toRequestBody("text/plain".toMediaTypeOrNull())
                val entityIdPart = noteId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                // Make API call
                val response = apiService.uploadImage(
                    image = imagePart,
                    entityType = entityTypePart,
                    entityId = entityIdPart
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        // Update note with upload info
                        repository.updateImageUploadInfo(
                            noteId = noteId,
                            imageUrl = body.signedUrl,
                            imageGcsPath = body.gcsPath,
                            status = UploadStatus.UPLOADED
                        )
                        Log.i(TAG, "Image uploaded successfully for note $noteId: ${body.gcsPath}")

                        // Optionally delete local file after successful upload
                        deleteLocalFile(file)
                    } else {
                        handleUploadFailure(noteId, "Empty response body")
                    }
                } else {
                    handleUploadFailure(noteId, "HTTP ${response.code()}: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading image for note $noteId", e)
                handleUploadFailure(noteId, e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Queue an image for upload when WiFi becomes available.
     */
    private suspend fun queueForWifiUpload(noteId: Long, imageUri: Uri) {
        // Update status to pending
        repository.updateUploadStatus(noteId, UploadStatus.PENDING)
        Log.i(TAG, "Queued image for note $noteId - waiting for WiFi")
    }

    /**
     * Process all pending uploads. Called when WiFi becomes available
     * or during periodic sync operations.
     */
    suspend fun processPendingUploads() {
        if (!isWifiConnected()) {
            Log.d(TAG, "Skipping pending uploads - not on WiFi")
            return
        }

        val pendingNotes = repository.getNotesByUploadStatus(UploadStatus.PENDING)
        Log.i(TAG, "Processing ${pendingNotes.size} pending image uploads")

        for (note in pendingNotes) {
            note.scanImagePath?.let { path ->
                try {
                    val uri = Uri.parse(path)
                    uploadImage(note.id, uri)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process pending upload for note ${note.id}", e)
                    handleUploadFailure(note.id, e.message ?: "Unknown error")
                }
            }
        }
    }

    /**
     * Retry a failed upload.
     */
    suspend fun retryUpload(noteId: Long) {
        val note = repository.getInboundNoteById(noteId) ?: return
        if (note.uploadRetryCount >= MAX_RETRY_COUNT) {
            Log.w(TAG, "Max retry attempts reached for note $noteId")
            return
        }

        note.scanImagePath?.let { path ->
            val uri = Uri.parse(path)
            uploadImage(noteId, uri)
        }
    }

    /**
     * Handle upload failure with retry logic.
     */
    private suspend fun handleUploadFailure(noteId: Long, errorMessage: String) {
        val note = repository.getInboundNoteById(noteId) ?: return
        val newRetryCount = note.uploadRetryCount + 1

        if (newRetryCount >= MAX_RETRY_COUNT) {
            repository.updateUploadStatus(noteId, UploadStatus.FAILED, newRetryCount)
            Log.e(TAG, "Upload failed permanently for note $noteId: $errorMessage")
        } else {
            repository.updateUploadStatus(noteId, UploadStatus.PENDING, newRetryCount)
            Log.w(TAG, "Upload failed for note $noteId (attempt $newRetryCount): $errorMessage")
        }
    }

    /**
     * Refresh a signed URL for an image that's about to expire.
     */
    suspend fun refreshSignedUrl(noteId: Long, imageId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getSignedUrl(imageId)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        repository.updateImageUrl(noteId, body.signedUrl)
                        Log.i(TAG, "Refreshed signed URL for note $noteId")
                        body.signedUrl
                    } else {
                        null
                    }
                } else {
                    Log.e(TAG, "Failed to refresh signed URL: ${response.code()}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing signed URL for note $noteId", e)
                null
            }
        }
    }

    /**
     * Check if device is connected to WiFi.
     */
    private fun isWifiConnected(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * Convert URI to File object.
     */
    private fun uriToFile(uri: Uri): File? {
        return try {
            when (uri.scheme) {
                "file" -> File(uri.path ?: return null)
                "content" -> {
                    // For content URIs, we need to copy to a temp file
                    val inputStream = context.contentResolver.openInputStream(uri) ?: return null
                    val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
                    tempFile.outputStream().use { output ->
                        inputStream.copyTo(output)
                    }
                    inputStream.close()
                    tempFile
                }
                else -> null
            }
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "File not found for URI: $uri", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error converting URI to file: $uri", e)
            null
        }
    }

    /**
     * Delete local file after successful upload.
     * This helps save device storage.
     */
    private fun deleteLocalFile(file: File) {
        try {
            // Only delete if it's in the cache directory (temp files) or app's private storage
            val isCacheFile = file.parentFile?.path?.contains(context.cacheDir.path) == true
            val isAppFile = file.parentFile?.path?.contains(context.filesDir.path) == true

            if (isCacheFile || isAppFile) {
                if (file.delete()) {
                    Log.d(TAG, "Deleted local file after upload: ${file.name}")
                } else {
                    Log.w(TAG, "Failed to delete local file: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting local file: ${file.name}", e)
        }
    }
}
