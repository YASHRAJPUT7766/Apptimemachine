package com.apptimemachine.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per version change (Part 2.2 Version History).
 * "Version 2.1.0 Installed -> 2.2.0 Updated -> 2.3.0 Updated" chain.
 */
@Entity(
    tableName = "version_history",
    foreignKeys = [
        ForeignKey(
            entity = InstalledAppEntity::class,
            parentColumns = ["appId"],
            childColumns = ["appId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["appId"]), Index(value = ["changedAt"])]
)
data class VersionHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val versionHistoryId: Long = 0,

    val appId: Long,

    val oldVersionName: String?,
    val newVersionName: String?,
    val oldVersionCode: Long?,
    val newVersionCode: Long?,

    val changeType: VersionChangeType,

    val changedAt: Long,
    val updateSource: String? = null
)

enum class VersionChangeType {
    INSTALLED, UPDATED, REMOVED, REINSTALLED
}
