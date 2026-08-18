package com.apptimemachine.core.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.apptimemachine.core.datastore.UserPreferences
import com.apptimemachine.data.entities.EventCategory
import com.apptimemachine.data.entities.EventSeverity
import com.apptimemachine.data.entities.NotificationEventType
import com.apptimemachine.data.entities.NotificationHistoryEntity
import com.apptimemachine.data.entities.NotificationPrivacyMode
import com.apptimemachine.data.entities.ScanType
import com.apptimemachine.data.entities.TimelineEventEntity
import com.apptimemachine.data.repository.AppRepository
import com.apptimemachine.data.repository.TimelineRepository
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
    @Inject lateinit var timelineRepository: TimelineRepository

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
            val rawTitle = extras?.getCharSequence("android.title")?.toString()
            val rawText = extras?.getCharSequence("android.text")?.toString()

            // OTP detection runs before privacy-mode stripping and before
            // storedTitle/storedBody are ever decided — an OTP notification
            // gets NO title/body stored at all, regardless of privacy mode,
            // only the isOtp flag + normal metadata (app, channel, time).
            // See OtpDetector doc for why this can't be "redact and store".
            val otpResult = OtpDetector.analyze(rawTitle, rawText)

            val (storedTitle, storedBody) = when {
                otpResult.isOtp -> null to null
                privacyMode == NotificationPrivacyMode.METADATA_ONLY -> null to null
                privacyMode == NotificationPrivacyMode.METADATA_PLUS_TITLE -> rawTitle to null
                else -> rawTitle to rawText
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
                    isOtp = otpResult.isOtp,
                    postedAt = sbn.postTime,
                    removedAt = if (eventType == NotificationEventType.REMOVED) System.currentTimeMillis() else null
                )
            )

            // Surface into the main Timeline too (Part 3.6 "All" feed), but
            // only for POSTED — REMOVED just marks a POSTED row leaving the
            // status bar, and a second Timeline row for the same
            // notification would just duplicate the feed. The permanent
            // in-app log (NotificationHistoryEntity, above) already
            // survives regardless of what happens to the row here.
            if (eventType == NotificationEventType.POSTED) {
                val summary = when {
                    otpResult.isOtp -> "OTP received"
                    storedTitle != null -> storedTitle
                    else -> "New notification"
                }
                timelineRepository.insert(
                    TimelineEventEntity(
                        appId = app.appId,
                        packageName = app.packageName,
                        appName = app.appName,
                        iconCachePath = app.iconCachePath,
                        eventCategory = EventCategory.NOTIFICATIONS,
                        eventType = "NOTIFICATION_POSTED",
                        newValue = summary,
                        severity = EventSeverity.INFO,
                        createdTimestamp = sbn.postTime,
                        sourceApi = "NotificationListenerService",
                        scanType = ScanType.REALTIME_BROADCAST,
                        scanId = null
                    )
                )
            }
        }
    }
}
