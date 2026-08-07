package com.apptimemachine.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single, permanent, append-only record of a detected change.
 *
 * Rule 4: nothing ever overwrites a previous event. Every row here is
 * immutable once written — updates/deletes only happen via explicit user
 * "Delete Timeline" actions (Part 3.3 Data Deletion), never automatically.
 */
@Entity(
    tableName = "timeline_events",
    foreignKeys = [
        ForeignKey(
            entity = InstalledAppEntity::class,
            parentColumns = ["appId"],
            childColumns = ["appId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["appId"]),
        Index(value = ["packageName"]),
        Index(value = ["eventCategory"]),
        Index(value = ["eventType"]),
        Index(value = ["createdTimestamp"]),
        Index(value = ["severity"])
    ]
)
data class TimelineEventEntity(
    @PrimaryKey(autoGenerate = true)
    val eventId: Long = 0,

    val appId: Long,
    val packageName: String,
    val appName: String,
    val iconCachePath: String? = null,

    @ColumnInfo(name = "eventCategory")
    val eventCategory: EventCategory,

    @ColumnInfo(name = "eventType")
    val eventType: String,

    val oldValue: String? = null,
    val newValue: String? = null,
    val difference: String? = null,
    val unit: String? = null,

    val severity: EventSeverity,

    val createdTimestamp: Long,

    val sourceApi: String? = null,
    val scanType: ScanType,
    val scanId: Long? = null,

    val notes: String? = null,

    // Groups multiple events generated from the same scan pass together
    // (Part 3.6 Smart Grouping — "WhatsApp: 3 Changes Detected").
    val groupKey: String? = null,

    val isBookmarked: Boolean = false
)

enum class EventCategory {
    INSTALLATION, VERSION, STORAGE, USAGE, PERMISSIONS,
    NOTIFICATIONS, BATTERY, NETWORK, MONITORING, BACKUP, EXPORT, SYSTEM
}

enum class EventSeverity {
    INFO, SUCCESS, WARNING, IMPORTANT, CRITICAL
}

enum class ScanType {
    AUTOMATIC, MANUAL, BOOT, REALTIME_BROADCAST
}
