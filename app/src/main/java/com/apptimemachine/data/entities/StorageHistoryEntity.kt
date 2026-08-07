package com.apptimemachine.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per detected storage change for an app (Part 2.0 Storage History).
 * Never overwritten — a full growth/shrink curve accumulates here.
 */
@Entity(
    tableName = "storage_history",
    foreignKeys = [
        ForeignKey(
            entity = InstalledAppEntity::class,
            parentColumns = ["appId"],
            childColumns = ["appId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["appId"]), Index(value = ["recordedAt"])]
)
data class StorageHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val storageHistoryId: Long = 0,

    val appId: Long,

    val appSizeBytes: Long?,
    val dataSizeBytes: Long?,
    val cacheSizeBytes: Long?,
    val totalSizeBytes: Long?,

    val previousTotalSizeBytes: Long?,
    val differenceBytes: Long?,

    val apkSizeBytes: Long?,

    val recordedAt: Long,
    val scanId: Long? = null
)
