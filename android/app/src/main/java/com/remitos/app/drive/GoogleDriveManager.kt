package com.remitos.app.drive

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.remitos.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Collections

/**
 * Manages Google Drive authentication and file uploads.
 * Provides functionality to export CSV files to Google Drive folders.
 */
class GoogleDriveManager(private val context: Context) {

    private val gsonFactory = GsonFactory.getDefaultInstance()
    private val httpTransport = NetHttpTransport()

    /**
     * Creates a Google Sign-In client configured for Drive access.
     */
    fun getGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    /**
     * Checks if a user is currently signed in.
     */
    fun isSignedIn(): Boolean {
        return GoogleSignIn.getLastSignedInAccount(context) != null
    }

    /**
     * Gets the currently signed in account, or null if not signed in.
     */
    fun getSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    /**
     * Signs out the current user.
     */
    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            getGoogleSignInClient().signOut()
        }
    }

    /**
     * Creates a Drive service instance for the signed-in user.
     */
    private fun createDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            Collections.singleton(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account

        return Drive.Builder(httpTransport, gsonFactory, credential)
            .setApplicationName(context.getString(R.string.app_name))
            .build()
    }

    /**
     * Checks if the device has an active internet connection.
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Creates the "Remitos Exports" folder in Google Drive if it doesn't exist.
     * Returns the folder ID.
     */
    private suspend fun getOrCreateDefaultFolder(driveService: Drive): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Check if folder already exists
                val result = driveService.files().list()
                    .setQ("mimeType='application/vnd.google-apps.folder' and name='Remitos Exports' and trashed=false")
                    .setSpaces("drive")
                    .setFields("files(id, name)")
                    .execute()

                if (result.files.isNotEmpty()) {
                    return@withContext result.files.first().id
                }

                // Create new folder
                val folderMetadata = File().apply {
                    name = "Remitos Exports"
                    mimeType = "application/vnd.google-apps.folder"
                }

                val folder = driveService.files().create(folderMetadata)
                    .setFields("id")
                    .execute()

                folder.id
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Uploads a CSV file to Google Drive.
     *
     * @param filePath The local path to the CSV file
     * @param fileName The desired name for the file in Drive
     * @param folderId Optional folder ID to upload to (uses "Remitos Exports" folder if null)
     * @param useDefaultFolder Whether to use the default "Remitos Exports" folder
     * @return Upload result with file ID and web view link
     */
    suspend fun uploadCsvFile(
        filePath: String,
        fileName: String,
        folderId: String? = null,
        useDefaultFolder: Boolean = true
    ): DriveUploadResult {
        return withContext(Dispatchers.IO) {
            try {
                // Check network
                if (!isNetworkAvailable()) {
                    return@withContext DriveUploadResult.Error(
                        context.getString(R.string.error_de_conexi_n_no_se_pudo_subir_a_drive),
                        isNetworkError = true
                    )
                }

                val account = getSignedInAccount()
                    ?: return@withContext DriveUploadResult.Error(
                        context.getString(R.string.debes_iniciar_sesion_para_subir_a_drive),
                        isAuthError = true
                    )

                val driveService = createDriveService(account)

                // Determine target folder
                val targetFolderId = when {
                    folderId != null -> folderId
                    useDefaultFolder -> getOrCreateDefaultFolder(driveService)
                    else -> null
                }

                // Create file metadata
                val fileMetadata = File().apply {
                    name = fileName
                    mimeType = "text/csv"
                    if (targetFolderId != null) {
                        parents = Collections.singletonList(targetFolderId)
                    }
                }

                // Upload file
                val fileContent = java.io.File(filePath)
                val mediaContent = FileContent("text/csv", fileContent)

                val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id, webViewLink")
                    .execute()

                DriveUploadResult.Success(
                    fileId = uploadedFile.id,
                    webViewLink = uploadedFile.webViewLink,
                    folderId = targetFolderId
                )
            } catch (e: ApiException) {
                DriveUploadResult.Error(
                    context.getString(R.string.error_al_subir_a_drive),
                    isAuthError = e.statusCode == 401 || e.statusCode == 403
                )
            } catch (e: IOException) {
                DriveUploadResult.Error(
                    if (isNetworkAvailable()) {
                        context.getString(R.string.error_al_subir_a_drive)
                    } else {
                        context.getString(R.string.error_de_conexi_n_no_se_pudo_subir_a_drive)
                    },
                    isNetworkError = !isNetworkAvailable()
                )
            } catch (e: Exception) {
                DriveUploadResult.Error(
                    context.getString(R.string.error_al_subir_a_drive)
                )
            }
        }
    }

    /**
     * Opens the Google Drive folder in the user's browser or Drive app.
     */
    fun openDriveFolder(folderId: String?) {
        val url = if (folderId != null) {
            "https://drive.google.com/drive/folders/$folderId"
        } else {
            "https://drive.google.com/drive/my-drive"
        }
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse(url)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Opens the uploaded file in Google Drive.
     */
    fun openDriveFile(fileId: String) {
        val url = "https://drive.google.com/file/d/$fileId/view"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse(url)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

/**
 * Sealed class representing the result of a Drive upload operation.
 */
sealed class DriveUploadResult {
    data class Success(
        val fileId: String,
        val webViewLink: String,
        val folderId: String?
    ) : DriveUploadResult()

    data class Error(
        val message: String,
        val isNetworkError: Boolean = false,
        val isAuthError: Boolean = false
    ) : DriveUploadResult()
}
