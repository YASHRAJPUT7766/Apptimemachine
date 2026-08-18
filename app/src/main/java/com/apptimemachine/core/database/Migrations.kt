package com.apptimemachine.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 -> v2: adds the Per-App Battery feature, plus two new columns on
 * notification_history (OTP flag, user-clear flag). Per Part 4.0 Database
 * Rules ("no destructive migrations in release builds"), this is additive
 * only — every existing table and row is untouched, so a device already
 * being monitored keeps its full history across the update.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `battery_usage` (
                `batteryUsageId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `appId` INTEGER NOT NULL,
                `dateEpochDay` INTEGER NOT NULL,
                `proxySharePercent` REAL NOT NULL,
                `foregroundMs` INTEGER NOT NULL,
                `deviceBatteryDropPercent` INTEGER,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`appId`) REFERENCES `installed_apps`(`appId`) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_battery_usage_appId_dateEpochDay` ON `battery_usage` (`appId`, `dateEpochDay`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_battery_usage_dateEpochDay` ON `battery_usage` (`dateEpochDay`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_battery_usage_proxySharePercent` ON `battery_usage` (`proxySharePercent`)")

        db.execSQL("ALTER TABLE `notification_history` ADD COLUMN `isOtp` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `notification_history` ADD COLUMN `clearedByUser` INTEGER NOT NULL DEFAULT 0")
    }
}
