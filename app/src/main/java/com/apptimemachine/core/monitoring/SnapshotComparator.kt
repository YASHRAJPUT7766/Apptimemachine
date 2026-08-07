package com.apptimemachine.core.monitoring

import com.apptimemachine.data.entities.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Compares a previously stored [InstalledAppEntity] against a freshly read
 * [RawPackageSnapshot] and produces the list of [TimelineEventEntity] rows
 * that should be appended.
 *
 * This class contains no I/O — it is pure comparison logic, kept separate
 * from the workers/repositories so it's trivially unit-testable (Part 3.9
 * Testability: "Business logic should be independent from UI" and
 * mockable/testable in isolation).
 *
 * Governing rule (Part 1.4C Change Detection): "Only generate an event when
 * an actual value changes." Every method below is a no-op (empty list) when
 * old == new.
 */
@Singleton
class SnapshotComparator @Inject constructor() {

    fun compareVersion(
        appId: Long,
        packageName: String,
        appName: String,
        iconPath: String?,
        previous: InstalledAppEntity,
        current: RawPackageSnapshot,
        now: Long,
        scanType: ScanType,
        scanId: Long?
    ): TimelineEventEntity? {
        val versionChanged = previous.versionName != current.versionName ||
            previous.versionCode != current.versionCode
        if (!versionChanged) return null

        return TimelineEventEntity(
            appId = appId,
            packageName = packageName,
            appName = appName,
            iconCachePath = iconPath,
            eventCategory = EventCategory.VERSION,
            eventType = "VERSION_UPDATED",
            oldValue = "${previous.versionName} (${previous.versionCode})",
            newValue = "${current.versionName} (${current.versionCode})",
            difference = null,
            severity = EventSeverity.CRITICAL,
            createdTimestamp = now,
            sourceApi = "PackageManager",
            scanType = scanType,
            scanId = scanId
        )
    }

    /**
     * Storage comparison uses a minimum threshold to avoid noise from
     * filesystem rounding — spec example: 1.45 GB -> 1.45 GB should be
     * ignored, but any real change should be captured.
     */
    fun compareStorage(
        appId: Long,
        packageName: String,
        appName: String,
        iconPath: String?,
        previousTotal: Long?,
        currentStorage: RawStorageSnapshot,
        now: Long,
        scanType: ScanType,
        scanId: Long?,
        minDeltaBytes: Long = 51_200 // 50 KB noise floor
    ): Pair<TimelineEventEntity?, StorageHistoryEntity?> {
        val currentTotal = currentStorage.totalBytes ?: return null to null
        val prev = previousTotal

        if (prev != null) {
            val delta = currentTotal - prev
            if (kotlin.math.abs(delta) < minDeltaBytes) return null to null

            val isGrowth = delta > 0
            val event = TimelineEventEntity(
                appId = appId,
                packageName = packageName,
                appName = appName,
                iconCachePath = iconPath,
                eventCategory = EventCategory.STORAGE,
                eventType = if (isGrowth) "STORAGE_INCREASED" else "STORAGE_REDUCED",
                oldValue = prev.toString(),
                newValue = currentTotal.toString(),
                difference = delta.toString(),
                unit = "bytes",
                severity = EventSeverity.INFO,
                createdTimestamp = now,
                sourceApi = "StorageStatsManager",
                scanType = scanType,
                scanId = scanId
            )
            val history = StorageHistoryEntity(
                appId = appId,
                appSizeBytes = currentStorage.appSizeBytes,
                dataSizeBytes = currentStorage.dataSizeBytes,
                cacheSizeBytes = currentStorage.cacheSizeBytes,
                totalSizeBytes = currentTotal,
                previousTotalSizeBytes = prev,
                differenceBytes = delta,
                apkSizeBytes = null,
                recordedAt = now,
                scanId = scanId
            )
            return event to history
        } else {
            // First-ever storage reading for this app after monitoring
            // started — baseline only, no event (Rule 2: no fake history).
            val history = StorageHistoryEntity(
                appId = appId,
                appSizeBytes = currentStorage.appSizeBytes,
                dataSizeBytes = currentStorage.dataSizeBytes,
                cacheSizeBytes = currentStorage.cacheSizeBytes,
                totalSizeBytes = currentTotal,
                previousTotalSizeBytes = null,
                differenceBytes = null,
                apkSizeBytes = null,
                recordedAt = now,
                scanId = scanId
            )
            return null to history
        }
    }

    /**
     * Permission comparison (Part 2.3). Emits one event per permission that
     * actually changed state — never a synthetic "no change" event.
     */
    fun comparePermissions(
        appId: Long,
        packageName: String,
        appName: String,
        iconPath: String?,
        previousGranted: Set<String>,
        currentGranted: Set<String>,
        now: Long,
        scanType: ScanType,
        scanId: Long?
    ): List<Pair<TimelineEventEntity, PermissionHistoryEntity>> {
        val results = mutableListOf<Pair<TimelineEventEntity, PermissionHistoryEntity>>()

        val newlyGranted = currentGranted - previousGranted
        val newlyRevoked = previousGranted - currentGranted

        newlyGranted.forEach { permission ->
            val sensitivity = classifySensitivity(permission)
            val severity = if (sensitivity == PermissionSensitivity.HIGH) EventSeverity.WARNING else EventSeverity.INFO
            val event = TimelineEventEntity(
                appId = appId, packageName = packageName, appName = appName, iconCachePath = iconPath,
                eventCategory = EventCategory.PERMISSIONS,
                eventType = "PERMISSION_GRANTED",
                oldValue = "Denied", newValue = "Granted",
                notes = permission,
                severity = severity,
                createdTimestamp = now,
                sourceApi = "PackageManager",
                scanType = scanType, scanId = scanId
            )
            val history = PermissionHistoryEntity(
                appId = appId, permissionName = permission,
                previousState = PermissionState.DENIED, currentState = PermissionState.GRANTED,
                sensitivity = sensitivity, changedAt = now, scanType = scanType
            )
            results += event to history
        }

        newlyRevoked.forEach { permission ->
            val sensitivity = classifySensitivity(permission)
            val event = TimelineEventEntity(
                appId = appId, packageName = packageName, appName = appName, iconCachePath = iconPath,
                eventCategory = EventCategory.PERMISSIONS,
                eventType = "PERMISSION_REVOKED",
                oldValue = "Granted", newValue = "Denied",
                notes = permission,
                severity = EventSeverity.WARNING,
                createdTimestamp = now,
                sourceApi = "PackageManager",
                scanType = scanType, scanId = scanId
            )
            val history = PermissionHistoryEntity(
                appId = appId, permissionName = permission,
                previousState = PermissionState.GRANTED, currentState = PermissionState.DENIED,
                sensitivity = sensitivity, changedAt = now, scanType = scanType
            )
            results += event to history
        }

        return results
    }

    private fun classifySensitivity(permission: String): PermissionSensitivity {
        val high = setOf(
            "android.permission.CAMERA", "android.permission.RECORD_AUDIO",
            "android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_BACKGROUND_LOCATION", "android.permission.READ_CONTACTS",
            "android.permission.READ_PHONE_STATE", "android.permission.CALL_PHONE",
            "android.permission.READ_SMS", "android.permission.SEND_SMS"
        )
        val medium = setOf(
            "android.permission.BLUETOOTH_CONNECT", "android.permission.READ_CALENDAR",
            "android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VIDEO",
            "android.permission.READ_MEDIA_AUDIO"
        )
        return when (permission) {
            in high -> PermissionSensitivity.HIGH
            in medium -> PermissionSensitivity.MEDIUM
            else -> PermissionSensitivity.LOW
        }
    }

    fun buildInstalledEvent(
        appId: Long, packageName: String, appName: String, iconPath: String?,
        now: Long, scanType: ScanType, scanId: Long?
    ) = TimelineEventEntity(
        appId = appId, packageName = packageName, appName = appName, iconCachePath = iconPath,
        eventCategory = EventCategory.INSTALLATION,
        eventType = "APPLICATION_INSTALLED",
        severity = EventSeverity.SUCCESS,
        createdTimestamp = now,
        sourceApi = "PackageManager",
        scanType = scanType, scanId = scanId
    )

    fun buildRemovedEvent(
        appId: Long, packageName: String, appName: String, iconPath: String?,
        now: Long, scanType: ScanType, scanId: Long?
    ) = TimelineEventEntity(
        appId = appId, packageName = packageName, appName = appName, iconCachePath = iconPath,
        eventCategory = EventCategory.INSTALLATION,
        eventType = "APPLICATION_REMOVED",
        severity = EventSeverity.CRITICAL,
        createdTimestamp = now,
        sourceApi = "PackageManager",
        scanType = scanType, scanId = scanId
    )

    fun buildReinstalledEvent(
        appId: Long, packageName: String, appName: String, iconPath: String?,
        now: Long, scanType: ScanType, scanId: Long?
    ) = TimelineEventEntity(
        appId = appId, packageName = packageName, appName = appName, iconCachePath = iconPath,
        eventCategory = EventCategory.INSTALLATION,
        eventType = "APPLICATION_REINSTALLED",
        severity = EventSeverity.IMPORTANT,
        createdTimestamp = now,
        sourceApi = "PackageManager",
        scanType = scanType, scanId = scanId
    )
}
