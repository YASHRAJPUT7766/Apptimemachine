package com.apptimemachine.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.apptimemachine.MainActivity
import com.apptimemachine.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends local alert notifications the moment a real-time event is detected
 * (new install, update, removal, permission change, etc). These are pure
 * on-device notifications — no network/server involved, works identically
 * with mobile data on, Wi-Fi on, or fully offline, since PackageManager
 * broadcasts and WorkManager both run locally.
 */
@Singleton
class AppNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "app_activity_channel"
        private const val CHANNEL_NAME = "App Activity"
        private const val CHANNEL_DESC = "Alerts when an app is installed, updated, removed, or changed"
    }

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Fires an immediate alert for a single detected event. [notificationId]
     * should be distinct per event (e.g. derived from timeline eventId) so
     * multiple events in the same scan each show their own notification
     * instead of overwriting one another.
     */
    fun notifyEvent(notificationId: Int, title: String, message: String) {
        if (!hasPermission()) return

        val tapIntent = android.content.Intent(context, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, notificationId, tapIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        androidx.core.app.NotificationManagerCompat.from(context).apply {
            // Guarded by hasPermission() above; NotificationManagerCompat
            // itself also enforces the POST_NOTIFICATIONS check on API 33+.
            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            ) {
                notify(notificationId, notification)
            }
        }
    }
}
