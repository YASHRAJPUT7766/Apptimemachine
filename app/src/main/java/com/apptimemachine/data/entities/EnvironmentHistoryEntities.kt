package com.apptimemachine.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Part 2.6 Notification History. Content stored depends on the user's
 * selected NotificationPrivacyMode — enforced at write time in the
 * notification listener/repository, never relaxed here.
 */
@Entity(
    tableName = "notification_history",
    foreignKeys = [
        ForeignKey(
            entity = InstalledAppEntity::class,
            parentColumns = ["appId"],
            childColumns = ["appId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["appId"]), Index(value = ["postedAt"]), Index(value = ["category"])]
)
data class NotificationHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val notificationHistoryId: Long = 0,

    val appId: Long,
    val systemNotificationKey: String? = null,

    val title: String? = null,     // only populated under Metadata+Title / Full modes
    val body: String? = null,      // only populated under Full mode

    val channelId: String? = null,
    val importance: Int? = null,
    val category: String? = null,

    val isOngoing: Boolean = false,
    val isGroup: Boolean = false,

    val eventType: NotificationEventType,
    val privacyModeUsed: NotificationPrivacyMode,

    // True when this notification was heuristically detected as containing
    // a one-time code (OTP). Per explicit instruction: OTP notifications
    // are never stored with their title/body content — only this flag and
    // the metadata (app, channel, time) are kept, so the log shows "OTP
    // received" without ever persisting the code itself. Detection happens
    // at capture time in the listener, before title/body are even attached
    // to the entity, mirroring how privacy-mode stripping already works.
    val isOtp: Boolean = false,

    val postedAt: Long,
    val removedAt: Long? = null,

    // Soft-delete: the user can clear a notification from the in-app log
    // (separate from clearing it off the system status bar, which the
    // system already does independently). Kept as a row rather than a hard
    // delete so a "cleared" state can't be confused with "was never
    // captured" — but excluded from every normal query/feed.
    val clearedByUser: Boolean = false
)

enum class NotificationEventType { POSTED, REMOVED, UPDATED }

enum class NotificationPrivacyMode { METADATA_ONLY, METADATA_PLUS_TITLE, FULL }

/**
 * Part 2.4 Battery History — charging sessions and device-level battery events.
 * Per-app battery percentages are intentionally NOT modeled: Android does not
 * expose exact per-app battery consumption to third-party apps (spec Rule /
 * "Important Android Limitation" in Part 2.4).
 */
@Entity(
    tableName = "battery_history",
    indices = [Index(value = ["startTime"])]
)
data class BatteryHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val batteryHistoryId: Long = 0,

    val eventType: BatteryEventType,

    val startTime: Long,
    val endTime: Long? = null,

    val batteryStartPercent: Int? = null,
    val batteryEndPercent: Int? = null,

    val chargingMethod: ChargingMethod? = null,
    val averageTemperature: Float? = null,
    val averageVoltage: Float? = null
)

enum class BatteryEventType {
    CHARGING_STARTED, CHARGING_STOPPED, FULLY_CHARGED, FAST_CHARGING_STARTED,
    TEMPERATURE_CHANGED, HEALTH_CHANGED, OPTIMIZATION_CHANGED,
    POWER_CONNECTED, POWER_DISCONNECTED
}

enum class ChargingMethod { AC, USB, WIRELESS, UNKNOWN }

/**
 * Part 2.5 Network History. Populated only where NetworkStatsManager /
 * TrafficStats expose data on the given device — otherwise rows are simply
 * not written and the UI shows "unavailable" (never estimated).
 */
@Entity(
    tableName = "network_history",
    foreignKeys = [
        ForeignKey(
            entity = InstalledAppEntity::class,
            parentColumns = ["appId"],
            childColumns = ["appId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["appId"]), Index(value = ["dateEpochDay"])]
)
data class NetworkHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val networkHistoryId: Long = 0,

    val appId: Long,
    val dateEpochDay: Long,

    val wifiRxBytes: Long? = null,
    val wifiTxBytes: Long? = null,
    val mobileRxBytes: Long? = null,
    val mobileTxBytes: Long? = null,

    val recordedAt: Long
)
