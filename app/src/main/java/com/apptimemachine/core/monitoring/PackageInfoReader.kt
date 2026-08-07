package com.apptimemachine.core.monitoring

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A read-only, immutable snapshot of one package as Android currently
 * reports it — the raw material for [SnapshotComparator]. Every field here
 * traces back to an official PackageManager/ApplicationInfo call (Rule 1:
 * never generate fake information; Rule 6: no hidden APIs, no reflection,
 * no root).
 */
data class RawPackageSnapshot(
    val packageName: String,
    val appName: String,
    val packageUid: Int,
    val versionName: String?,
    val versionCode: Long,
    val installTime: Long,
    val lastUpdateTime: Long,
    val apkSizeBytes: Long?,
    val targetSdk: Int,
    val minSdk: Int,
    val isSystemApp: Boolean,
    val isEnabled: Boolean,
    val isSuspended: Boolean,
    val launchableActivity: String?,
    val grantedPermissions: Set<String>,
    val category: String?
)

@Singleton
class PackageInfoReader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val pm: PackageManager get() = context.packageManager

    /**
     * Reads every installed application currently visible to this app via
     * QUERY_ALL_PACKAGES. Sequential processing per Part 1.4C Memory
     * Optimization — "never keep complete package list in RAM" beyond what's
     * needed for one comparison pass; caller is expected to process/emit
     * one [RawPackageSnapshot] at a time rather than materializing huge lists
     * long-term.
     */
    fun readAllPackages(): List<RawPackageSnapshot> {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PackageManager.GET_PERMISSIONS or PackageManager.PackageInfoFlags.of(0).let { 0 }
        } else {
            PackageManager.GET_PERMISSIONS
        }

        val packages: List<PackageInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        }

        return packages.mapNotNull { runCatching { toSnapshot(it) }.getOrNull() }
    }

    fun readSinglePackage(packageName: String): RawPackageSnapshot? {
        return runCatching {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            }
            toSnapshot(info)
        }.getOrNull()
    }

    private fun toSnapshot(info: PackageInfo): RawPackageSnapshot {
        val appInfo = info.applicationInfo
            ?: throw IllegalStateException("No ApplicationInfo for ${info.packageName}")

        val grantedPermissions = buildSet {
            val perms = info.requestedPermissions
            val flags = info.requestedPermissionsFlags
            if (perms != null && flags != null) {
                for (i in perms.indices) {
                    if (flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0) {
                        add(perms[i])
                    }
                }
            }
        }

        val launchIntent = pm.getLaunchIntentForPackage(info.packageName)

        return RawPackageSnapshot(
            packageName = info.packageName,
            appName = pm.getApplicationLabel(appInfo).toString(),
            packageUid = appInfo.uid,
            versionName = info.versionName,
            versionCode = PackageInfoCompat.getLongVersionCode(info),
            installTime = info.firstInstallTime,
            lastUpdateTime = info.lastUpdateTime,
            apkSizeBytes = runCatching { java.io.File(appInfo.sourceDir).length() }.getOrNull(),
            targetSdk = appInfo.targetSdkVersion,
            minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) appInfo.minSdkVersion else 0,
            isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
            isEnabled = appInfo.enabled,
            isSuspended = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                (appInfo.flags and ApplicationInfo.FLAG_SUSPENDED) != 0
            } else false,
            launchableActivity = launchIntent?.component?.className,
            grantedPermissions = grantedPermissions,
            category = categoryName(appInfo)
        )
    }

    private fun categoryName(appInfo: ApplicationInfo): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        return when (appInfo.category) {
            ApplicationInfo.CATEGORY_GAME -> "Game"
            ApplicationInfo.CATEGORY_AUDIO -> "Audio"
            ApplicationInfo.CATEGORY_VIDEO -> "Video"
            ApplicationInfo.CATEGORY_IMAGE -> "Image"
            ApplicationInfo.CATEGORY_SOCIAL -> "Social"
            ApplicationInfo.CATEGORY_NEWS -> "News"
            ApplicationInfo.CATEGORY_MAPS -> "Maps"
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
            else -> null
        }
    }
}
