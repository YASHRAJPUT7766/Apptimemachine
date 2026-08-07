package com.apptimemachine.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The current-state snapshot of a monitored application.
 *
 * This table always reflects Android's CURRENT view of the app (Rule 3).
 * It is the row that gets compared against on every scan; changes are what
 * generate [TimelineEventEntity] rows (Rule 4) — this table itself is
 * overwritten in place because it represents "now", not history.
 *
 * appId is a stable synthetic key that never changes across rescans, even
 * if the app is uninstalled and reinstalled (see Part 2.2 Reinstall Detection).
 */
@Entity(
    tableName = "installed_apps",
    indices = [
        Index(value = ["packageName"], unique = true),
        Index(value = ["appName"]),
        Index(value = ["isSystemApp"]),
        Index(value = ["isRemoved"])
    ]
)
data class InstalledAppEntity(
    @PrimaryKey(autoGenerate = true)
    val appId: Long = 0,

    @ColumnInfo(name = "packageName")
    val packageName: String,

    val appName: String,
    val packageUid: Int,

    // Icon is stored as a cached file path, never as a blob, to keep the DB light.
    val iconCachePath: String? = null,

    val versionName: String?,
    val versionCode: Long,

    val installTime: Long,
    val lastUpdateTime: Long,

    val apkSizeBytes: Long? = null,
    val appSizeBytes: Long? = null,
    val dataSizeBytes: Long? = null,
    val cacheSizeBytes: Long? = null,

    val targetSdk: Int,
    val minSdk: Int,

    val category: String? = null,
    val isSystemApp: Boolean,
    val isEnabled: Boolean,
    val isSuspended: Boolean,
    val launchableActivity: String? = null,

    // Permissions stored as a comma-separated snapshot for quick diffing;
    // authoritative history lives in PermissionHistoryEntity.
    val grantedPermissionsSnapshot: String = "",

    val monitoringStartTimestamp: Long,
    val snapshotVersion: Int = 1,

    // True once an ACTION_PACKAGE_REMOVED is observed. Row is kept, never
    // deleted, so the timeline remains readable (Part 2.2 Application Removal).
    val isRemoved: Boolean = false,
    val removedAt: Long? = null,

    val isFavorite: Boolean = false,
    val isIgnored: Boolean = false,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
