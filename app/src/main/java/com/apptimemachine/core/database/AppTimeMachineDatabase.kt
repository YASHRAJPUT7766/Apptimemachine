package com.apptimemachine.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.apptimemachine.data.dao.*
import com.apptimemachine.data.entities.*

/**
 * Single Room database for the whole app (Part 3.9 Database Layer).
 *
 * Versioning discipline (Part 4.0 Database Rules): every schema change gets
 * a new version + an explicit Migration in DatabaseModule. Destructive
 * migrations are never used in release builds — user monitoring history
 * must never be silently dropped on an app update.
 */
@Database(
    entities = [
        InstalledAppEntity::class,
        TimelineEventEntity::class,
        StorageHistoryEntity::class,
        DailyUsageEntity::class,
        UsageSessionEntity::class,
        VersionHistoryEntity::class,
        PermissionHistoryEntity::class,
        NotificationHistoryEntity::class,
        BatteryHistoryEntity::class,
        NetworkHistoryEntity::class,
        ScanHistoryEntity::class,
        ExportHistoryEntity::class,
        BackupHistoryEntity::class,
        ReportEntity::class,
        BookmarkEntity::class,
        SavedSearchEntity::class,
        RecentSearchEntity::class,
        InsightEntity::class,
        BatteryUsageEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppTimeMachineDatabase : RoomDatabase() {

    abstract fun installedAppDao(): InstalledAppDao
    abstract fun timelineEventDao(): TimelineEventDao
    abstract fun storageHistoryDao(): StorageHistoryDao
    abstract fun dailyUsageDao(): DailyUsageDao
    abstract fun usageSessionDao(): UsageSessionDao
    abstract fun versionHistoryDao(): VersionHistoryDao
    abstract fun permissionHistoryDao(): PermissionHistoryDao
    abstract fun notificationHistoryDao(): NotificationHistoryDao
    abstract fun batteryHistoryDao(): BatteryHistoryDao
    abstract fun networkHistoryDao(): NetworkHistoryDao
    abstract fun scanHistoryDao(): ScanHistoryDao
    abstract fun exportHistoryDao(): ExportHistoryDao
    abstract fun backupHistoryDao(): BackupHistoryDao
    abstract fun reportDao(): ReportDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun searchDao(): SearchDao
    abstract fun insightDao(): InsightDao
    abstract fun batteryUsageDao(): BatteryUsageDao

    companion object {
        const val DATABASE_NAME = "app_time_machine.db"
        const val DATABASE_VERSION_FOR_BACKUP = 2
    }
}
