package com.remitos.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.remitos.app.MainActivity
import com.remitos.app.R
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@EntryPoint
@InstallIn(SingletonComponent::class)
interface OperationalNotifierEntryPoint {
    fun operationalNotifier(): OperationalNotifier
}

/**
 * Local (non-FCM) notifications for operators: sync, device/session, uploads, and scan/save issues.
 */
@Singleton
class OperationalNotifier @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val notificationManager: NotificationManagerCompat
        get() = NotificationManagerCompat.from(appContext)

    init {
        createChannelsIfNeeded()
    }

    private fun createChannelsIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = appContext.getSystemService(NotificationManager::class.java) ?: return
        val sync = NotificationChannel(
            CHANNEL_SYNC,
            appContext.getString(R.string.notif_channel_sync_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = appContext.getString(R.string.notif_channel_sync_desc)
        }
        val account = NotificationChannel(
            CHANNEL_ACCOUNT,
            appContext.getString(R.string.notif_channel_account_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = appContext.getString(R.string.notif_channel_account_desc)
        }
        val work = NotificationChannel(
            CHANNEL_WORK,
            appContext.getString(R.string.notif_channel_work_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = appContext.getString(R.string.notif_channel_work_desc)
        }
        mgr.createNotificationChannel(sync)
        mgr.createNotificationChannel(account)
        mgr.createNotificationChannel(work)
    }

    private fun canPost(): Boolean {
        if (Build.VERSION.SDK_INT >= 33) {
            return ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun contentIntent(): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(appContext, 0, intent, flags)
    }

    private fun show(
        id: Int,
        channelId: String,
        title: String,
        text: String,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
    ) {
        if (!canPost() || !notificationManager.areNotificationsEnabled()) return

        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(priority)
            .setContentIntent(contentIntent())
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(id, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS revoked at runtime
        }
    }

    fun cancelSyncError() {
        notificationManager.cancel(ID_SYNC_ERROR)
    }

    fun cancelPendingReconnect() {
        notificationManager.cancel(ID_PENDING_SYNC)
    }

    fun notifySyncFailed(message: String) {
        show(
            ID_SYNC_ERROR,
            CHANNEL_SYNC,
            appContext.getString(R.string.notif_sync_failed_title),
            message.ifBlank { appContext.getString(R.string.notif_sync_failed_body) },
            NotificationCompat.PRIORITY_HIGH,
        )
    }

    /** After Wi‑Fi returns: hint that local changes still need upload (debounced). */
    fun maybeNotifyPendingAfterReconnect(pendingCount: Int) {
        if (pendingCount <= 0) return
        val now = System.currentTimeMillis()
        val last = prefs.getLong(KEY_LAST_PENDING_RECONNECT_NOTIF, 0L)
        if (now - last < PENDING_RECONNECT_DEBOUNCE_MS) return
        prefs.edit().putLong(KEY_LAST_PENDING_RECONNECT_NOTIF, now).apply()
        show(
            ID_PENDING_SYNC,
            CHANNEL_SYNC,
            appContext.getString(R.string.notif_pending_sync_title),
            appContext.getString(R.string.notif_pending_sync_body, pendingCount),
        )
    }

    fun notifyUserSuspended() {
        show(
            ID_ACCOUNT_SUSPENDED,
            CHANNEL_ACCOUNT,
            appContext.getString(R.string.notif_account_suspended_title),
            appContext.getString(R.string.notif_account_suspended_body),
            NotificationCompat.PRIORITY_HIGH,
        )
    }

    fun notifyDeviceRevoked() {
        show(
            ID_DEVICE_REVOKED,
            CHANNEL_ACCOUNT,
            appContext.getString(R.string.notif_device_revoked_title),
            appContext.getString(R.string.notif_device_revoked_body),
            NotificationCompat.PRIORITY_HIGH,
        )
    }

    fun notifyAuthSessionLost() {
        show(
            ID_AUTH_SESSION,
            CHANNEL_ACCOUNT,
            appContext.getString(R.string.notif_auth_session_title),
            appContext.getString(R.string.notif_auth_session_body),
            NotificationCompat.PRIORITY_HIGH,
        )
    }

    fun notifyInactivityLogout() {
        show(
            ID_INACTIVITY,
            CHANNEL_ACCOUNT,
            appContext.getString(R.string.notif_inactivity_title),
            appContext.getString(R.string.notif_inactivity_body),
        )
    }

    fun notifyEntitlementUploadBlocked() {
        show(
            ID_ENTITLEMENT,
            CHANNEL_SYNC,
            appContext.getString(R.string.notif_entitlement_title),
            appContext.getString(R.string.notif_entitlement_body),
        )
    }

    fun notifyImageUploadPermanentFailure(noteId: Long, detail: String) {
        val text = appContext.getString(R.string.notif_image_upload_failed_body, noteId, detail)
        show(
            ID_IMAGE_UPLOAD_BASE + (noteId % 32).toInt(),
            CHANNEL_WORK,
            appContext.getString(R.string.notif_image_upload_failed_title),
            text,
        )
    }

    fun notifyInboundSaveFailed(message: String) {
        show(
            ID_INBOUND_SAVE,
            CHANNEL_WORK,
            appContext.getString(R.string.notif_inbound_save_failed_title),
            message.ifBlank { appContext.getString(R.string.notif_inbound_save_failed_body) },
        )
    }

    companion object {
        private const val PREFS = "operational_notifier"
        private const val KEY_LAST_PENDING_RECONNECT_NOTIF = "last_pending_reconnect_notif"
        private const val PENDING_RECONNECT_DEBOUNCE_MS = 30L * 60L * 1000L

        const val CHANNEL_SYNC = "operational_sync"
        const val CHANNEL_ACCOUNT = "operational_account"
        const val CHANNEL_WORK = "operational_work"

        private const val ID_SYNC_ERROR = 9101
        private const val ID_ACCOUNT_SUSPENDED = 9102
        private const val ID_DEVICE_REVOKED = 9103
        private const val ID_AUTH_SESSION = 9104
        private const val ID_PENDING_SYNC = 9105
        private const val ID_IMAGE_UPLOAD_BASE = 9110
        private const val ID_INBOUND_SAVE = 9140
        private const val ID_ENTITLEMENT = 9141
        private const val ID_INACTIVITY = 9142
    }
}

fun Context.operationalNotifier(): OperationalNotifier {
    return EntryPointAccessors.fromApplication(
        applicationContext,
        OperationalNotifierEntryPoint::class.java,
    ).operationalNotifier()
}
