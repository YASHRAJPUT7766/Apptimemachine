package com.apptimemachine.core.monitoring

import android.app.usage.StorageStats
import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.os.UserHandle
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class RawStorageSnapshot(
    val appSizeBytes: Long?,
    val dataSizeBytes: Long?,
    val cacheSizeBytes: Long?
) {
    val totalBytes: Long? get() =
        if (appSizeBytes == null && dataSizeBytes == null && cacheSizeBytes == null) null
        else (appSizeBytes ?: 0) + (dataSizeBytes ?: 0) + (cacheSizeBytes ?: 0)
}

/**
 * Wraps StorageStatsManager (API 26+). Per Part 2.0 spec: "If Android does
 * not expose a value on a device, leave that value unavailable rather than
 * estimating it." Every failure path here returns null fields instead of a
 * guessed number.
 */
@Singleton
class StorageStatsReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageStatsReader: UsageStatsReader
) {
    private val storageStatsManager: StorageStatsManager? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(Context.STORAGE_SERVICE) as? StorageStatsManager
        } else null
    }

    /**
     * Requires PACKAGE_USAGE_STATS (Usage Access) to succeed for apps other
     * than the caller itself — this is an Android platform requirement, not
     * an app design choice (queryStatsForPackage silently no-ops/throws
     * without it). We check hasUsageAccessPermission() up front — the same
     * gate scanUsage() already used — instead of relying on the query to
     * fail on its own, since without this every app's storage came back
     * null and the whole Storage tab showed "Unavailable" with no
     * indication that granting Usage Access was the fix. Returns all-null
     * on any failure rather than throwing, so scans continue for other
     * apps (Part 2.0 Failure Handling).
     */
    /**
     * Requires PACKAGE_USAGE_STATS (Usage Access) to succeed for apps other
     * than the caller itself — this is an Android platform requirement, not
     * an app design choice. We check hasUsageAccessPermission() up front —
     * the same gate scanUsage() already used.
     *
     * Uses queryStatsForUid() rather than queryStatsForPackage(): the latter
     * throws NameNotFoundException for some system/vendor apps (observed
     * with MIUI system packages like com.miui.aod) even when Usage Access
     * IS granted — Android resolves them under a different UserHandle
     * internally, so a package+user lookup misses while a raw UID lookup
     * (which is also the path Android's own docs recommend as faster) finds
     * them. This is why Storage stayed "Unavailable" for some apps even
     * after granting Usage Access and running a scan.
     */
    fun readStorage(packageName: String, uid: Int): RawStorageSnapshot {
        val manager = storageStatsManager ?: return RawStorageSnapshot(null, null, null)
        if (!usageStatsReader.hasUsageAccessPermission()) return RawStorageSnapshot(null, null, null)

        val storageUuid = runCatching {
            context.packageManager.getApplicationInfo(packageName, 0).storageUuid
        }.getOrNull() ?: return RawStorageSnapshot(null, null, null)

        // Primary path: queryStatsForUid — works for system/vendor packages
        // that queryStatsForPackage can't resolve.
        runCatching {
            val stats: StorageStats = manager.queryStatsForUid(storageUuid, uid)
            return RawStorageSnapshot(
                appSizeBytes = stats.appBytes,
                dataSizeBytes = stats.dataBytes,
                cacheSizeBytes = stats.cacheBytes
            )
        }.onFailure { e ->
            android.util.Log.w("StorageStatsReader", "queryStatsForUid failed for $packageName (uid=$uid)", e)
        }

        // Fallback: queryStatsForPackage, in case a package ever behaves
        // the other way around (resolvable by name but not by uid).
        return runCatching {
            val userHandle: UserHandle = Process.myUserHandle()
            val stats: StorageStats = manager.queryStatsForPackage(storageUuid, packageName, userHandle)
            RawStorageSnapshot(
                appSizeBytes = stats.appBytes,
                dataSizeBytes = stats.dataBytes,
                cacheSizeBytes = stats.cacheBytes
            )
        }.getOrElse { e ->
            android.util.Log.w("StorageStatsReader", "queryStatsForPackage fallback also failed for $packageName", e)
            RawStorageSnapshot(null, null, null)
        }
    }
}
