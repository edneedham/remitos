package com.remitos.app.data

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages user session lifecycle including auto-logout on inactivity.
 * Tracks app foreground/background state and user interactions.
 */
class SessionManager(
    private val context: Context,
    private val authManager: AuthManager,
    private val onSessionExpired: () -> Unit
) : Application.ActivityLifecycleCallbacks {
    
    companion object {
        private const val CHECK_INTERVAL_MS = 60_000L // Check every minute
    }
    
    private val handler = Handler(Looper.getMainLooper())
    private var lastActivityTime = System.currentTimeMillis()
    private var isAppInForeground = false
    private val scope = CoroutineScope(Dispatchers.Main)
    
    private val sessionCheckRunnable = object : Runnable {
        override fun run() {
            checkSessionTimeout()
            if (isAppInForeground) {
                handler.postDelayed(this, CHECK_INTERVAL_MS)
            }
        }
    }
    
    /**
     * Initialize session manager. Call from Application.onCreate()
     */
    fun initialize(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }
    
    /**
     * Update last activity timestamp.
     * Call this on user interactions (clicks, typing, etc.)
     */
    fun onUserInteraction() {
        lastActivityTime = System.currentTimeMillis()
    }
    
    /**
     * Check if auto-logout is enabled and if session has expired.
     */
    private fun checkSessionTimeout() {
        // Skip if auto-logout is disabled
        if (FeatureFlags.autoLogoutMinutes <= 0) return
        
        // Skip if no user is logged in
        val currentUser = authManager.getCurrentUser() ?: return
        
        val timeoutMs = FeatureFlags.autoLogoutMinutes * 60_000L
        val elapsedMs = System.currentTimeMillis() - lastActivityTime
        
        if (elapsedMs >= timeoutMs) {
            // Session expired - logout user
            scope.launch {
                logoutCurrentUser()
            }
        }
    }
    
    /**
     * Logout current user and notify callback.
     */
    private suspend fun logoutCurrentUser() {
        val currentUser = authManager.getCurrentUser()
        if (currentUser != null) {
            authManager.removeToken(currentUser)
            onSessionExpired()
        }
    }
    
    /**
     * Reset the session timer.
     * Call when user performs authentication or switches accounts.
     */
    fun resetSession() {
        lastActivityTime = System.currentTimeMillis()
    }
    
    /**
     * Stop session monitoring.
     * Call when app is terminating.
     */
    fun stop() {
        handler.removeCallbacks(sessionCheckRunnable)
        (context as? Application)?.unregisterActivityLifecycleCallbacks(this)
    }
    
    // Activity Lifecycle Callbacks
    
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    
    override fun onActivityStarted(activity: Activity) {}
    
    override fun onActivityResumed(activity: Activity) {
        isAppInForeground = true
        lastActivityTime = System.currentTimeMillis()
        handler.post(sessionCheckRunnable)
    }
    
    override fun onActivityPaused(activity: Activity) {
        // App might be going to background, but don't stop monitoring yet
    }
    
    override fun onActivityStopped(activity: Activity) {
        // Check if app is truly in background
        if (!isAppInForeground) {
            handler.removeCallbacks(sessionCheckRunnable)
        }
    }
    
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    
    override fun onActivityDestroyed(activity: Activity) {}
}

/**
 * Extension function to track user interactions in Activities.
 * Call from Activity.dispatchTouchEvent() or onUserInteraction()
 */
fun Activity.trackUserInteraction(sessionManager: SessionManager) {
    sessionManager.onUserInteraction()
}
