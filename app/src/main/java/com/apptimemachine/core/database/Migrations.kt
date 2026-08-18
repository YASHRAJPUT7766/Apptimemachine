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

/**
 * v2 -> v3: fixes network_history having no uniqueness constraint on
 * (appId, dateEpochDay). Every network scan cycle was plain-@Insert-ing a
 * new row instead of updating that day's row, so devices that had already
 * run several scans had multiple rows per app per day sitting in the
 * table — which is why the same app appeared to repeat in the Network
 * Usage card. This migration keeps only the most-recently-recorded row
 * per (appId, dateEpochDay) — everything else about the table, and every
 * other table, is untouched — then adds the unique index so it can never
 * happen again (the DAO now uses @Upsert against this index).
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            DELETE FROM `network_history`
            WHERE `networkHistoryId` NOT IN (
                SELECT MAX(`networkHistoryId`)
                FROM `network_history`
                GROUP BY `appId`, `dateEpochDay`
            )
            """.trimIndent()
        )
        db.execSQL("DROP INDEX IF EXISTS `index_network_history_dateEpochDay`")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_network_history_appId_dateEpochDay` ON `network_history` (`appId`, `dateEpochDay`)")
    }
}
