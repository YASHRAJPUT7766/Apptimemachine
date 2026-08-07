package com.apptimemachine.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Part 3.0 Backup History / Part 1.3 Scan History — one row per completed
 * or attempted background/manual scan.
 */
@Entity(tableName = "scan_history", indices = [Index(value = ["startTime"])])
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val scanId: Long = 0,

    val scanType: ScanType,
    val startTime: Long,
    val finishTime: Long? = null,
    val durationMs: Long? = null,

    val appsScanned: Int = 0,
    val eventsGenerated: Int = 0,
    val errorCount: Int = 0,

    val success: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Part 3.1 Export History.
 */
@Entity(tableName = "export_history", indices = [Index(value = ["createdAt"])])
data class ExportHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val exportId: Long = 0,

    val reportType: String,
    val format: ExportFormat,
    val filePath: String,
    val fileSizeBytes: Long,
    val recordCount: Int,

    val status: ExportStatus,
    val createdAt: Long
)

enum class ExportFormat { PDF, CSV, JSON }
enum class ExportStatus { SUCCESS, FAILED, IN_PROGRESS }

/**
 * Part 3.0 Backup History.
 */
@Entity(tableName = "backup_history", indices = [Index(value = ["createdAt"])])
data class BackupHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val backupId: Long = 0,

    val filePath: String,
    val fileSizeBytes: Long,
    val checksum: String,

    val appVersionAtBackup: String,
    val databaseVersionAtBackup: Int,

    val isEncrypted: Boolean,
    val status: BackupStatus,

    val createdAt: Long,
    val restoredAt: Long? = null
)

enum class BackupStatus { SUCCESS, FAILED, RESTORED, CORRUPTED }

/**
 * Part 2.8/3.1 Reports — cached generated report metadata; underlying data
 * always sourced live from the history tables at generation time.
 */
@Entity(tableName = "reports", indices = [Index(value = ["generatedAt"])])
data class ReportEntity(
    @PrimaryKey(autoGenerate = true)
    val reportId: Long = 0,

    val title: String,
    val reportType: ReportType,

    val periodStart: Long,
    val periodEnd: Long,

    val filePath: String? = null,
    val generatedAt: Long
)

enum class ReportType { DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM }

/**
 * Part 3.6 Bookmarks — separate from InstalledAppEntity.isFavorite, this
 * bookmarks individual timeline events rather than whole apps.
 */
@Entity(tableName = "bookmarks", indices = [Index(value = ["eventId"], unique = true)])
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val bookmarkId: Long = 0,
    val eventId: Long,
    val bookmarkedAt: Long = System.currentTimeMillis()
)

/**
 * Part 2.9 Saved Searches.
 */
@Entity(tableName = "saved_searches", indices = [Index(value = ["createdAt"])])
data class SavedSearchEntity(
    @PrimaryKey(autoGenerate = true)
    val savedSearchId: Long = 0,
    val query: String,
    val label: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Part 2.9 Recent Searches (capped to last 20 in the DAO layer).
 */
@Entity(tableName = "recent_searches", indices = [Index(value = ["searchedAt"])])
data class RecentSearchEntity(
    @PrimaryKey(autoGenerate = true)
    val recentSearchId: Long = 0,
    val keyword: String,
    val resultCount: Int,
    val category: String,
    val searchedAt: Long = System.currentTimeMillis()
)

/**
 * Part 3.8 Insight History.
 */
@Entity(tableName = "insights", indices = [Index(value = ["generatedAt"]), Index(value = ["category"])])
data class InsightEntity(
    @PrimaryKey(autoGenerate = true)
    val insightId: Long = 0,

    val category: String,
    val priority: InsightPriority,
    val title: String,
    val explanation: String,
    val relatedAppId: Long? = null,

    val periodStart: Long,
    val periodEnd: Long,

    val generatedAt: Long
)

enum class InsightPriority { HIGH, MEDIUM, LOW }
