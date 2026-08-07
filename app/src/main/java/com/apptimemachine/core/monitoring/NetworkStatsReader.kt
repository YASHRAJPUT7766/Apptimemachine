package com.apptimemachine.core.monitoring

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Process
import android.telephony.TelephonyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class RawNetworkSnapshot(
    val wifiRxBytes: Long?,
    val wifiTxBytes: Long?,
    val mobileRxBytes: Long?,
    val mobileTxBytes: Long?
)

/**
 * Wraps NetworkStatsManager (Part 2.5). Per-app network stats require the
 * same Usage Access permission as UsageStatsManager and are not available
 * on every device/Android version — per spec: "If unsupported, display
 * 'Per-app network statistics are unavailable on this device.' Never
 * estimate. Never fake." Every failure path returns null fields.
 */
@Singleton
class NetworkStatsReader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val networkStatsManager: NetworkStatsManager? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
        } else null
    }

    fun isSupported(): Boolean = networkStatsManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    /**
     * Reads per-UID Wi-Fi and mobile data usage between [startTime] and
     * [endTime]. Requires Usage Access (same permission as
     * [UsageStatsReader]) to succeed for apps other than the caller.
     */
    fun readUsageForUid(uid: Int, startTime: Long, endTime: Long): RawNetworkSnapshot {
        val manager = networkStatsManager ?: return RawNetworkSnapshot(null, null, null, null)

        val wifi = runCatching { queryTotals(manager, ConnectivityManager.TYPE_WIFI, uid, startTime, endTime) }
            .getOrNull()
        val mobile = runCatching { queryTotals(manager, ConnectivityManager.TYPE_MOBILE, uid, startTime, endTime) }
            .getOrNull()

        return RawNetworkSnapshot(
            wifiRxBytes = wifi?.first,
            wifiTxBytes = wifi?.second,
            mobileRxBytes = mobile?.first,
            mobileTxBytes = mobile?.second
        )
    }

    /** Returns (rxBytes, txBytes) summed across all buckets for the given UID/type/window. */
    private fun queryTotals(
        manager: NetworkStatsManager, networkType: Int, uid: Int, startTime: Long, endTime: Long
    ): Pair<Long, Long>? {
        val subscriberId: String? = null // null works for Wi-Fi and is required-null on most mobile queries without READ_PHONE_STATE
        val bucket = NetworkStats.Bucket()
        var rx = 0L
        var tx = 0L
        val stats = manager.queryDetailsForUid(networkType, subscriberId, startTime, endTime, uid)
        stats.use {
            while (it.hasNextBucket()) {
                it.getNextBucket(bucket)
                rx += bucket.rxBytes
                tx += bucket.txBytes
            }
        }
        return rx to tx
    }
}
