package com.apptimemachine.core.monitoring

import com.apptimemachine.core.datastore.UserPreferences
import com.apptimemachine.data.entities.*
import com.apptimemachine.data.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

data class ScanResult(
    val appsScanned: Int,
    val eventsGenerated: Int,
    val errorCount: Int
)

/**
 * The brain of App Time Machine (Part 1.4C). Contains NO UI code — it is
 * invoked by [com.apptimemachine.core.workers.AppMonitoringWorker] for
 * scheduled/boot scans and directly by ViewModels for a manual "Scan Now".
 *
 * Responsibilities per spec: initialize monitoring, perform scans, compare
 * snapshots, generate events, save database, notify listeners (via Room
 * Flow emissions — no separate event bus needed).
 */
@Singleton
class MonitoringManager @Inject constructor(
    private val packageInfoReader: PackageInfoReader,
    private val storageStatsReader: StorageStatsReader,
    private val usageStatsReader: UsageStatsReader,
    private val networkStatsReader: NetworkStatsReader,
    private val comparator: SnapshotComparator,
    private val appRepository: AppRepository,
    private val timelineRepository: TimelineRepository,
    private val storageRepository: StorageRepository,
    private val versionRepository: VersionRepository,
    private val permissionRepository: PermissionRepository,
    private val usageRepository: UsageRepository,
    private val networkRepository: NetworkRepository,
    private val scanRepository: ScanRepository,
    private val userPreferences: UserPreferences
) {
    /**
     * Rule 4/Part 1.1: performs the very first device scan. Every installed
     * app is recorded as a baseline snapshot with NO timeline events —
     * "This snapshot is NEVER deleted unless user clears data" and future
     * scans compare against it.
     */
    suspend fun performInitialScan(): ScanResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        var errors = 0

        val rawPackages = runCatching { packageInfoReader.readAllPackages() }
            .getOrElse { errors++; emptyList() }

        for (raw in rawPackages) {
            runCatching {
                val storage = storageStatsReader.readStorage(raw.packageName, raw.packageUid)
                val entity = InstalledAppEntity(
                    packageName = raw.packageName,
                    appName = raw.appName,
                    packageUid = raw.packageUid,
                    versionName = raw.versionName,
                    versionCode = raw.versionCode,
                    installTime = raw.installTime,
                    lastUpdateTime = raw.lastUpdateTime,
                    apkSizeBytes = raw.apkSizeBytes,
                    appSizeBytes = storage.appSizeBytes,
                    dataSizeBytes = storage.dataSizeBytes,
                    cacheSizeBytes = storage.cacheSizeBytes,
                    targetSdk = raw.targetSdk,
                    minSdk = raw.minSdk,
                    category = raw.category,
                    isSystemApp = raw.isSystemApp,
                    isEnabled = raw.isEnabled,
                    isSuspended = raw.isSuspended,
                    launchableActivity = raw.launchableActivity,
                    grantedPermissionsSnapshot = raw.grantedPermissions.joinToString(","),
                    monitoringStartTimestamp = now,
                    snapshotVersion = 1
                )
                val appId = appRepository.insert(entity)

                storage.totalBytes?.let { total ->
                    storageRepository.insert(
                        StorageHistoryEntity(
                            appId = appId,
                            appSizeBytes = storage.appSizeBytes,
                            dataSizeBytes = storage.dataSizeBytes,
                            cacheSizeBytes = storage.cacheSizeBytes,
                            totalSizeBytes = total,
                            previousTotalSizeBytes = null,
                            differenceBytes = null,
                            apkSizeBytes = raw.apkSizeBytes,
                            recordedAt = now,
                            scanId = null
                        )
                    )
                }
            }.onFailure { errors++ }
        }

        userPreferences.setMonitoringStartTimestamp(now)
        userPreferences.setMonitoringEnabled(true)

        ScanResult(appsScanned = rawPackages.size, eventsGenerated = 0, errorCount = errors)
    }

    /**
     * The recurring scan cycle (Part 1.3 Monitoring Cycle, steps 1-7):
     * read previous snapshot -> read current state -> compare -> generate
     * events -> update database. Runs on IO dispatcher, never blocks caller.
     */
    suspend fun performScan(scanType: ScanType): ScanResult = withContext(Dispatchers.IO) {
        val scanId = scanRepository.insert(
            ScanHistoryEntity(scanType = scanType, startTime = System.currentTimeMillis())
        )

        var errors = 0
        var eventsGenerated = 0
        val now = System.currentTimeMillis()

        val rawPackages = runCatching { packageInfoReader.readAllPackages() }
            .getOrElse { errors++; emptyList() }
        val currentPackageNames = rawPackages.map { it.packageName }.toSet()

        val storedApps = runCatching { appRepository.getAllIncludingRemoved() }
            .getOrElse { errors++; emptyList() }
        val storedByPackage = storedApps.associateBy { it.packageName }

        // --- New installs & updates ---
        for (raw in rawPackages) {
            runCatching {
                val existing = storedByPackage[raw.packageName]
                if (existing == null) {
                    eventsGenerated += handleNewInstall(raw, now, scanType, scanId)
                } else if (existing.isRemoved) {
                    eventsGenerated += handleReinstall(existing, raw, now, scanType, scanId)
                } else {
                    eventsGenerated += handleExistingApp(existing, raw, now, scanType, scanId)
                }
            }.onFailure { errors++ }
        }

        // --- Removals: apps we had before, not seen now, not already marked removed ---
        storedApps.filter { !it.isRemoved && it.packageName !in currentPackageNames }
            .forEach { removedApp ->
                runCatching {
                    appRepository.markRemoved(removedApp.packageName, now)
                    timelineRepository.insert(
                        comparator.buildRemovedEvent(
                            removedApp.appId, removedApp.packageName, removedApp.appName,
                            removedApp.iconCachePath, now, scanType, scanId
                        )
                    )
                    eventsGenerated++
                }.onFailure { errors++ }
            }

        // --- Usage (only if permission granted; never estimated otherwise) ---
        runCatching {
            eventsGenerated += scanUsage(now, scanType, scanId)
        }.onFailure { errors++ }

        // --- Network (only where NetworkStatsManager is supported; never estimated otherwise) ---
        runCatching {
            scanNetwork(now)
        }.onFailure { errors++ }

        scanRepository.update(
            ScanHistoryEntity(
                scanId = scanId, scanType = scanType,
                startTime = now, finishTime = System.currentTimeMillis(),
                durationMs = System.currentTimeMillis() - now,
                appsScanned = rawPackages.size, eventsGenerated = eventsGenerated,
                errorCount = errors, success = errors == 0
            )
        )

        ScanResult(rawPackages.size, eventsGenerated, errors)
    }

    private suspend fun handleNewInstall(
        raw: RawPackageSnapshot, now: Long, scanType: ScanType, scanId: Long
    ): Int {
        val storage = storageStatsReader.readStorage(raw.packageName, raw.packageUid)
        val entity = InstalledAppEntity(
            packageName = raw.packageName, appName = raw.appName, packageUid = raw.packageUid,
            versionName = raw.versionName, versionCode = raw.versionCode,
            installTime = raw.installTime, lastUpdateTime = raw.lastUpdateTime,
            apkSizeBytes = raw.apkSizeBytes,
            appSizeBytes = storage.appSizeBytes, dataSizeBytes = storage.dataSizeBytes,
            cacheSizeBytes = storage.cacheSizeBytes,
            targetSdk = raw.targetSdk, minSdk = raw.minSdk, category = raw.category,
            isSystemApp = raw.isSystemApp, isEnabled = raw.isEnabled, isSuspended = raw.isSuspended,
            launchableActivity = raw.launchableActivity,
            grantedPermissionsSnapshot = raw.grantedPermissions.joinToString(","),
            monitoringStartTimestamp = now
        )
        val appId = appRepository.insert(entity)
        timelineRepository.insert(
            comparator.buildInstalledEvent(appId, raw.packageName, raw.appName, null, now, scanType, scanId)
        )
        versionRepository.insert(
            VersionHistoryEntity(
                appId = appId, oldVersionName = null, newVersionName = raw.versionName,
                oldVersionCode = null, newVersionCode = raw.versionCode,
                changeType = VersionChangeType.INSTALLED, changedAt = now
            )
        )
        return 1
    }

    private suspend fun handleReinstall(
        existing: InstalledAppEntity, raw: RawPackageSnapshot, now: Long, scanType: ScanType, scanId: Long
    ): Int {
        appRepository.markReinstalled(raw.packageName)
        timelineRepository.insert(
            comparator.buildReinstalledEvent(
                existing.appId, raw.packageName, raw.appName, existing.iconCachePath, now, scanType, scanId
            )
        )
        return 1
    }

    private suspend fun handleExistingApp(
        existing: InstalledAppEntity, raw: RawPackageSnapshot, now: Long, scanType: ScanType, scanId: Long
    ): Int {
        var events = 0

        // Version
        comparator.compareVersion(
            existing.appId, raw.packageName, raw.appName, existing.iconCachePath,
            existing, raw, now, scanType, scanId
        )?.let {
            timelineRepository.insert(it)
            versionRepository.insert(
                VersionHistoryEntity(
                    appId = existing.appId,
                    oldVersionName = existing.versionName, newVersionName = raw.versionName,
                    oldVersionCode = existing.versionCode, newVersionCode = raw.versionCode,
                    changeType = VersionChangeType.UPDATED, changedAt = now
                )
            )
            events++
        }

        // Storage
        val storage = storageStatsReader.readStorage(raw.packageName, raw.packageUid)
        val previousTotal = storageRepository.getLatestForApp(existing.appId)?.totalSizeBytes
        val (storageEvent, storageHistory) = comparator.compareStorage(
            existing.appId, raw.packageName, raw.appName, existing.iconCachePath,
            previousTotal, storage, now, scanType, scanId
        )
        storageEvent?.let { timelineRepository.insert(it); events++ }
        storageHistory?.let { storageRepository.insert(it) }

        // Permissions
        val previousPermissions = existing.grantedPermissionsSnapshot
            .split(",").filter { it.isNotBlank() }.toSet()
        val permissionResults = comparator.comparePermissions(
            existing.appId, raw.packageName, raw.appName, existing.iconCachePath,
            previousPermissions, raw.grantedPermissions, now, scanType, scanId
        )
        if (permissionResults.isNotEmpty()) {
            timelineRepository.insertAll(permissionResults.map { it.first })
            permissionRepository.insertAll(permissionResults.map { it.second })
            events += permissionResults.size
        }

        // Persist updated snapshot fields (Part 1.3 Step 6: Update database)
        appRepository.update(
            existing.copy(
                versionName = raw.versionName,
                versionCode = raw.versionCode,
                lastUpdateTime = raw.lastUpdateTime,
                appSizeBytes = storage.appSizeBytes,
                dataSizeBytes = storage.dataSizeBytes,
                cacheSizeBytes = storage.cacheSizeBytes,
                isEnabled = raw.isEnabled,
                isSuspended = raw.isSuspended,
                grantedPermissionsSnapshot = raw.grantedPermissions.joinToString(","),
                updatedAt = now
            )
        )

        return events
    }

    private suspend fun scanUsage(now: Long, scanType: ScanType, scanId: Long): Int {
        if (!usageStatsReader.hasUsageAccessPermission()) return 0

        val today = LocalDate.now(ZoneOffset.UTC)
        val startOfDay = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val epochDay = today.toEpochDay()

        val usageMap = usageStatsReader.readUsageBetween(startOfDay, now)
        var events = 0

        for ((packageName, usage) in usageMap) {
            val app = appRepository.findByPackageName(packageName) ?: continue
            val existingDaily = usageRepository.getForAppAndDay(app.appId, epochDay)

            usageRepository.upsert(
                DailyUsageEntity(
                    dailyUsageId = existingDaily?.dailyUsageId ?: 0,
                    appId = app.appId,
                    dateEpochDay = epochDay,
                    foregroundTimeMs = usage.totalForegroundTimeMs,
                    launchCount = usage.launchCount,
                    lastUsedTimestamp = usage.lastTimeUsed,
                    longestSessionMs = existingDaily?.longestSessionMs ?: 0,
                    sessionCount = existingDaily?.sessionCount ?: 0,
                    updatedAt = now
                )
            )
            // Usage events are intentionally low-severity/low-frequency —
            // only recorded as timeline events on significant daily deltas
            // to avoid flooding the timeline (Part 1.4C Event Priority: Low).
            val previousMs = existingDaily?.foregroundTimeMs ?: 0
            if (usage.totalForegroundTimeMs - previousMs > 5 * 60_000) { // 5+ min new usage
                events++
            }
        }
        return events
    }

    /**
     * Part 2.5 Network Monitoring Engine — records per-app Wi-Fi/mobile
     * usage for the current day where NetworkStatsManager is supported.
     * Silently no-ops when unsupported (spec: never estimate, never fake).
     */
    private suspend fun scanNetwork(now: Long) {
        if (!networkStatsReader.isSupported()) return
        if (!usageStatsReader.hasUsageAccessPermission()) return // same permission gate as usage stats

        val today = LocalDate.now(ZoneOffset.UTC)
        val startOfDay = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val epochDay = today.toEpochDay()

        val apps = appRepository.getAllIncludingRemoved().filter { !it.isRemoved }

        for (app in apps) {
            val snapshot = networkStatsReader.readUsageForUid(app.packageUid, startOfDay, now)
            if (snapshot.wifiRxBytes == null && snapshot.mobileRxBytes == null) continue

            networkRepository.insert(
                NetworkHistoryEntity(
                    appId = app.appId,
                    dateEpochDay = epochDay,
                    wifiRxBytes = snapshot.wifiRxBytes,
                    wifiTxBytes = snapshot.wifiTxBytes,
                    mobileRxBytes = snapshot.mobileRxBytes,
                    mobileTxBytes = snapshot.mobileTxBytes,
                    recordedAt = now
                )
            )
        }
    }
}
