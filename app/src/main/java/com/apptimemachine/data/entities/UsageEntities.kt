package com.apptimemachine.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per app per calendar day (Part 2.1 Usage Data / Daily Usage Table).
 * Upserted throughout the day as UsageStatsManager reports new totals;
 * once the day rolls over a new row is created rather than overwritten.
 */
@Entity(
    tableName = "daily_usage",
    foreignKeys = [
        ForeignKey(
            entity = InstalledAppEntity::class,
            parentColumns = ["appId"],
            childColumns = ["appId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["appId", "dateEpochDay"], unique = true), Index(value = ["dateEpochDay"])]
)
data class DailyUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val dailyUsageId: Long = 0,

    val appId: Long,
    val dateEpochDay: Long, // LocalDate.toEpochDay()

    val foregroundTimeMs: Long = 0,
    val launchCount: Int = 0,
    val lastUsedTimestamp: Long? = null,

    val longestSessionMs: Long = 0,
    val sessionCount: Int = 0,

    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * One row per foreground session (Part 2.1 Session Detection / Session History).
 */
@Entity(
    tableName = "usage_sessions",
    foreignKeys = [
        ForeignKey(
            entity = InstalledAppEntity::class,
            parentColumns = ["appId"],
            childColumns = ["appId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["appId"]), Index(value = ["sessionStart"])]
)
data class UsageSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val sessionId: Long = 0,

    val appId: Long,
    val sessionStart: Long,
    val sessionEnd: Long?,
    val durationMs: Long?
)
