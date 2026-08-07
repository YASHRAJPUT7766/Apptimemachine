package com.apptimemachine.core.monitoring

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class RawUsageSnapshot(
    val packageName: String,
    val totalForegroundTimeMs: Long,
    val lastTimeUsed: Long,
    val launchCount: Int
)

/**
 * Wraps UsageStatsManager (Part 2.1). Never estimates — if Usage Access is
 * not granted, [hasUsageAccessPermission] returns false and callers must
 * skip usage monitoring entirely rather than fabricate numbers (Part 2.1
 * "Never estimate usage. Never generate fake statistics.").
 */
@Singleton
class UsageStatsReader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val usageStatsManager: UsageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    fun hasUsageAccessPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Reads aggregated usage between [startTime] and [endTime] (typically
     * start-of-day to now, per Part 2.1 Daily Usage tracking). Returns an
     * empty map if permission is missing — callers must check
     * [hasUsageAccessPermission] first and surface that state to the UI
     * rather than silently reporting zero usage as if it were measured.
     */
    fun readUsageBetween(startTime: Long, endTime: Long): Map<String, RawUsageSnapshot> {
        if (!hasUsageAccessPermission()) return emptyMap()

        val statsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        ) ?: return emptyMap()

        return statsList
            .filter { it.totalTimeInForeground > 0 }
            .associate { stat ->
                stat.packageName to RawUsageSnapshot(
                    packageName = stat.packageName,
                    totalForegroundTimeMs = stat.totalTimeInForeground,
                    lastTimeUsed = stat.lastTimeUsed,
                    launchCount = readLaunchCount(stat)
                )
            }
    }

    private fun readLaunchCount(stat: android.app.usage.UsageStats): Int {
        // Per-app launch count is not exposed by the public UsageStats API
        // on any API level — exposed as 0 rather than guessed or read via
        // hidden/reflection APIs (spec Rule 6: no hidden APIs, no
        // reflection; spec: never estimate, never fabricate).
        return 0
    }
}
