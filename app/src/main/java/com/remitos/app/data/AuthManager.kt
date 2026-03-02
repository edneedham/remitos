package com.remitos.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Token data for a user session.
 */
data class TokenData(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,  // Unix timestamp in milliseconds
    val userEmail: String,
    val userName: String? = null
)

/**
 * User information for account switching.
 */
data class UserInfo(
    val userId: String,
    val email: String,
    val name: String?,
    val lastLoginAt: Long
)

/**
 * Manages authentication tokens for multiple users.
 * Uses EncryptedSharedPreferences with Android Keystore for security.
 */
class AuthManager(private val context: Context) {
    companion object {
        private const val PREFS_FILE = "auth_encrypted"
        private const val KEY_CURRENT_USER = "current_user_id"
        private const val KEY_USERS_LIST = "users_list"
        private const val KEY_TOKEN_PREFIX = "token_"
        private const val BUFFER_TIME_MS = 5 * 60 * 1000L // Refresh 5 min before expiry
    }
    
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private val encryptedPrefs: EncryptedSharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }
    
    /**
     * Save authentication tokens for a user.
     */
    suspend fun saveToken(userId: String, tokenData: TokenData) = withContext(Dispatchers.IO) {
        val tokenJson = JSONObject().apply {
            put("access_token", tokenData.accessToken)
            put("refresh_token", tokenData.refreshToken)
            put("expires_at", tokenData.expiresAt)
            put("user_email", tokenData.userEmail)
            put("user_name", tokenData.userName)
        }
        
        encryptedPrefs.edit().apply {
            putString("$KEY_TOKEN_PREFIX$userId", tokenJson.toString())
            apply()
        }
        
        // Add to users list
        addToUsersList(userId, tokenData.userEmail, tokenData.userName)
    }
    
    /**
     * Get stored token data for a user.
     * Returns null if no token exists or parsing fails.
     */
    suspend fun getToken(userId: String): TokenData? = withContext(Dispatchers.IO) {
        getTokenSync(userId)
    }
    
    /**
     * Get stored token data synchronously.
     * Returns null if no token exists or parsing fails.
     */
    fun getTokenSync(userId: String): TokenData? {
        return try {
            val tokenString = encryptedPrefs.getString("$KEY_TOKEN_PREFIX$userId", null)
                ?: return null
            
            val json = JSONObject(tokenString)
            TokenData(
                accessToken = json.getString("access_token"),
                refreshToken = json.getString("refresh_token"),
                expiresAt = json.getLong("expires_at"),
                userEmail = json.optString("user_email", ""),
                userName = json.optString("user_name", "")
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Remove a user's tokens (logout).
     */
    suspend fun removeToken(userId: String) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().apply {
            remove("$KEY_TOKEN_PREFIX$userId")
            apply()
        }
        removeFromUsersList(userId)
        
        // If this was the current user, clear current user
        if (getCurrentUser() == userId) {
            setCurrentUser("")
        }
    }
    
    /**
     * Check if a token is expired or about to expire.
     */
    suspend fun isTokenExpired(userId: String): Boolean {
        val token = getToken(userId) ?: return true
        return System.currentTimeMillis() >= (token.expiresAt - BUFFER_TIME_MS)
    }
    
    /**
     * Get the currently active user ID.
     */
    fun getCurrentUser(): String? {
        val userId = encryptedPrefs.getString(KEY_CURRENT_USER, "")
        return if (userId.isNullOrEmpty()) null else userId
    }
    
    /**
     * Set the currently active user.
     */
    fun setCurrentUser(userId: String) {
        encryptedPrefs.edit().apply {
            putString(KEY_CURRENT_USER, userId)
            apply()
        }
    }
    
    /**
     * List all logged-in users.
     */
    suspend fun listLoggedInUsers(): List<UserInfo> = withContext(Dispatchers.IO) {
        try {
            val usersString = encryptedPrefs.getString(KEY_USERS_LIST, "[]")
            val jsonArray = JSONArray(usersString)
            
            (0 until jsonArray.length()).map { i ->
                val obj = jsonArray.getJSONObject(i)
                UserInfo(
                    userId = obj.getString("user_id"),
                    email = obj.getString("email"),
                    name = obj.optString("name", null),
                    lastLoginAt = obj.getLong("last_login_at")
                )
            }.sortedByDescending { it.lastLoginAt }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Check if any user is logged in.
     */
    suspend fun hasLoggedInUsers(): Boolean {
        return listLoggedInUsers().isNotEmpty()
    }
    
    /**
     * Get user info by ID.
     */
    suspend fun getUserInfo(userId: String): UserInfo? {
        return listLoggedInUsers().find { it.userId == userId }
    }
    
    /**
     * Clear all authentication data.
     * Use with caution - logs out ALL users.
     */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().apply {
            clear()
            apply()
        }
    }
    
    private suspend fun addToUsersList(userId: String, email: String, name: String?) {
        val users = listLoggedInUsers().toMutableList()
        
        // Remove if already exists (update)
        users.removeAll { it.userId == userId }
        
        // Add to front (most recent)
        users.add(0, UserInfo(
            userId = userId,
            email = email,
            name = name,
            lastLoginAt = System.currentTimeMillis()
        ))
        
        saveUsersList(users)
    }
    
    private suspend fun removeFromUsersList(userId: String) {
        val users = listLoggedInUsers().filter { it.userId != userId }
        saveUsersList(users)
    }
    
    private fun saveUsersList(users: List<UserInfo>) {
        val jsonArray = JSONArray()
        users.forEach { user ->
            jsonArray.put(JSONObject().apply {
                put("user_id", user.userId)
                put("email", user.email)
                put("name", user.name)
                put("last_login_at", user.lastLoginAt)
            })
        }
        
        encryptedPrefs.edit().apply {
            putString(KEY_USERS_LIST, jsonArray.toString())
            apply()
        }
    }
}
