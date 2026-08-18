package com.apptimemachine.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per app per calendar day — a battery-drain PROXY, not a measured
 * battery percentage.
 *
 * IMPORTANT — why this is a proxy and not real per-app battery %: Android
 * does not expose per-app battery consumption to third-party apps on any
 * non-rooted device. The API that reports it, BatteryStatsManager
 * .getBatteryUsageStats(), is marked @SystemApi/@hide and requires the
 * signature-level BATTERY_STATS permission — it isn't part of the public
 * SDK and no ordinary app, however many permissions it's granted, can call
 * it. Building this feature on that API would mean either it silently
 * fails on every real device, or resorting to hidden/reflection APIs,
 * which the spec's Rule 6 forbids outright.
 *
 * What this table stores instead is honestly derived from data the app
 * genuinely has: [DailyUsageEntity.foregroundTimeMs] (via UsageStatsManager,
 * a real, public, permitted API). foregroundTimeMs is the strongest
 * available real signal for "how much this app likely drained the
 * battery today" — more screen-on time for an app essentially always
 * means more battery used by it — but it is explicitly NOT the same
 * number Android's own Settings > Battery screen would show, since it
 * ignores background/wakelock/network-radio drain the OS can see but a
 * third-party app cannot. The UI must always label this "Estimated from
 * usage time" / "battery drain proxy" and never as measured battery %,
 * so the distinction the spec insists on (Rule 1: never fabricate) stays
 * visible to the user rather than just to the code.
 */
@Entity(
    tableName = "battery_usage",
    foreignKeys = [
        ForeignKey(
            entity = InstalledAppEntity::class,
            parentColumns = ["appId"],
            childColumns = ["appId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["appId", "dateEpochDay"], unique = true),
        Index(value = ["dateEpochDay"]),
        Index(value = ["proxySharePercent"])
    ]
)
data class BatteryUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val batteryUsageId: Long = 0,

    val appId: Long,
    val dateEpochDay: Long,

    /**
     * This app's foreground time as a share of the total foreground time
     * across all apps that day, 0f..100f. A ranking/proportion figure, not
     * a battery percentage — see class doc.
     */
    val proxySharePercent: Float,

    val foregroundMs: Long,

    /**
     * Total battery percent lost across all charging-disconnected windows
     * that day, when available (device-level, from BatteryHistoryEntity).
     * Shown alongside the proxy purely as context ("phone dropped 40%
     * today"), never split per-app.
     */
    val deviceBatteryDropPercent: Int? = null,

    val updatedAt: Long
)
