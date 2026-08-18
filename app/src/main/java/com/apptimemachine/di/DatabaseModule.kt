package com.apptimemachine.di

import android.content.Context
import androidx.room.Room
import com.apptimemachine.core.database.AppTimeMachineDatabase
import com.apptimemachine.core.database.MIGRATION_1_2
import com.apptimemachine.core.database.MIGRATION_2_3
import com.apptimemachine.data.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppTimeMachineDatabase {
        return Room.databaseBuilder(
            context,
            AppTimeMachineDatabase::class.java,
            AppTimeMachineDatabase.DATABASE_NAME
        )
            // No fallbackToDestructiveMigration in release — Part 4.0 Database
            // Rules: "No destructive migrations in release builds." Future
            // schema bumps must add an explicit Migration() here.
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
    }

    @Provides
    fun provideInstalledAppDao(db: AppTimeMachineDatabase): InstalledAppDao = db.installedAppDao()

    @Provides
    fun provideTimelineEventDao(db: AppTimeMachineDatabase): TimelineEventDao = db.timelineEventDao()

    @Provides
    fun provideStorageHistoryDao(db: AppTimeMachineDatabase): StorageHistoryDao = db.storageHistoryDao()

    @Provides
    fun provideDailyUsageDao(db: AppTimeMachineDatabase): DailyUsageDao = db.dailyUsageDao()

    @Provides
    fun provideUsageSessionDao(db: AppTimeMachineDatabase): UsageSessionDao = db.usageSessionDao()

    @Provides
    fun provideVersionHistoryDao(db: AppTimeMachineDatabase): VersionHistoryDao = db.versionHistoryDao()

    @Provides
    fun providePermissionHistoryDao(db: AppTimeMachineDatabase): PermissionHistoryDao = db.permissionHistoryDao()

    @Provides
    fun provideNotificationHistoryDao(db: AppTimeMachineDatabase): NotificationHistoryDao = db.notificationHistoryDao()

    @Provides
    fun provideBatteryHistoryDao(db: AppTimeMachineDatabase): BatteryHistoryDao = db.batteryHistoryDao()

    @Provides
    fun provideNetworkHistoryDao(db: AppTimeMachineDatabase): NetworkHistoryDao = db.networkHistoryDao()

    @Provides
    fun provideScanHistoryDao(db: AppTimeMachineDatabase): ScanHistoryDao = db.scanHistoryDao()

    @Provides
    fun provideExportHistoryDao(db: AppTimeMachineDatabase): ExportHistoryDao = db.exportHistoryDao()

    @Provides
    fun provideBackupHistoryDao(db: AppTimeMachineDatabase): BackupHistoryDao = db.backupHistoryDao()

    @Provides
    fun provideReportDao(db: AppTimeMachineDatabase): ReportDao = db.reportDao()

    @Provides
    fun provideBookmarkDao(db: AppTimeMachineDatabase): BookmarkDao = db.bookmarkDao()

    @Provides
    fun provideSearchDao(db: AppTimeMachineDatabase): SearchDao = db.searchDao()

    @Provides
    fun provideInsightDao(db: AppTimeMachineDatabase): InsightDao = db.insightDao()

    @Provides
    fun provideBatteryUsageDao(db: AppTimeMachineDatabase): BatteryUsageDao = db.batteryUsageDao()
}
