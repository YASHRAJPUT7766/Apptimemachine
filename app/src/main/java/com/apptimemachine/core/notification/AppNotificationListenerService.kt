package com.apptimemachine.core.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.apptimemachine.core.datastore.UserPreferences
import com.apptimemachine.data.entities.NotificationEventType
import com.apptimemachine.data.entities.NotificationHistoryEntity
import com.apptimemachine.data.entities.NotificationPrivacyMode
import com.apptimemachine.data.repository.AppRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Part 2.6 Notification Monitoring Engine.
 *
 * Critical privacy rule enforced here at the SOURCE, not downstream:
 * the selected [NotificationPrivacyMode] determines exactly what gets
 * written to Room. Title/body are stripped before the entity is even
 * constructed under Metadata Only / Metadata+Title modes — there is no
 * "store everything then filter on export" path, because that would mean
 * sensitive content touches disk even briefly under a stricter mode.
 *
 * This service does nothing until the user explicitly grants Notification
 * Listener access (Part 2.6: "must never access notifications before
 * permission is granted" — enforced by Android itself refusing to bind
 * the service without that grant).
 */
@AndroidEntryPoint
class AppNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var appRepository: AppRepository
    @Inject lateinit var userPreferences: UserPreferences
    @Inject lateinit var notificationHistoryRepository: com.apptimemachine.data.repository.NotificationRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        handle(sbn, NotificationEventType.POSTED)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        handle(sbn, NotificationEventType.REMOVED)
    }

    private fun handle(sbn: StatusBarNotification, eventType: NotificationEventType) {
        scope.launch {
            val app = appRepository.findByPackageName(sbn.packageName) ?: return@launch
            val privacyMode = userPreferences.notificationPrivacyMode.first()

            val extras = sbn.notification?.extras
            val title = extras?.getCharSequence("android.title")?.toString()
            val text = extras?.getCharSequence("android.text")?.toString()

            val (storedTitle, storedBody) = when (privacyMode) {
                NotificationPrivacyMode.METADATA_ONLY -> null to null
                NotificationPrivacyMode.METADATA_PLUS_TITLE -> title to null
                NotificationPrivacyMode.FULL -> title to text
            }

            notificationHistoryRepository.insert(
                NotificationHistoryEntity(
                    appId = app.appId,
                    systemNotificationKey = sbn.key,
                    title = storedTitle,
                    body = storedBody,
                    channelId = sbn.notification?.channelId,
                    importance = null,
                    category = sbn.notification?.category,
                    isOngoing = sbn.isOngoing,
                    isGroup = sbn.isGroup,
                    eventType = eventType,
                    privacyModeUsed = privacyMode,
                    postedAt = sbn.postTime,
                    removedAt = if (eventType == NotificationEventType.REMOVED) System.currentTimeMillis() else null
                )
            )
        }
    }
}
