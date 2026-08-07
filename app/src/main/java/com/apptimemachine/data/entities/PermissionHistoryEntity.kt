package com.apptimemachine.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per permission state change (Part 2.3 Permission History).
 * Sensitivity classification is informational only (spec: "Do not label
 * applications as malicious").
 */
@Entity(
    tableName = "permission_history",
    foreignKeys = [
        ForeignKey(
            entity = InstalledAppEntity::class,
            parentColumns = ["appId"],
            childColumns = ["appId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["appId"]), Index(value = ["changedAt"]), Index(value = ["permissionName"])]
)
data class PermissionHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val permissionHistoryId: Long = 0,

    val appId: Long,
    val permissionName: String,

    val previousState: PermissionState,
    val currentState: PermissionState,

    val sensitivity: PermissionSensitivity,

    val changedAt: Long,
    val scanType: ScanType
)

enum class PermissionState { GRANTED, DENIED, UNKNOWN }

enum class PermissionSensitivity { LOW, MEDIUM, HIGH }
