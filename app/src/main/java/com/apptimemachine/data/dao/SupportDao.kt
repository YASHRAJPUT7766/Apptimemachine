package com.apptimemachine.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.apptimemachine.data.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {
    @Insert
    suspend fun insert(scan: ScanHistoryEntity): Long

    @Update
    suspend fun update(scan: ScanHistoryEntity)

    @Query("SELECT * FROM scan_history ORDER BY startTime DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history ORDER BY startTime DESC LIMIT 1")
    fun observeLatest(): Flow<ScanHistoryEntity?>

    @Query("SELECT * FROM scan_history WHERE scanId = :scanId LIMIT 1")
    suspend fun getById(scanId: Long): ScanHistoryEntity?
}

@Dao
interface ExportHistoryDao {
    @Insert
    suspend fun insert(export: ExportHistoryEntity): Long

    @Query("SELECT * FROM export_history ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ExportHistoryEntity>>

    @Query("DELETE FROM export_history")
    suspend fun deleteAll()
}

@Dao
interface BackupHistoryDao {
    @Insert
    suspend fun insert(backup: BackupHistoryEntity): Long

    @Update
    suspend fun update(backup: BackupHistoryEntity)

    @Query("SELECT * FROM backup_history ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BackupHistoryEntity>>

    @Query("SELECT * FROM backup_history ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatest(): BackupHistoryEntity?

    @Query("DELETE FROM backup_history")
    suspend fun deleteAll()
}

@Dao
interface ReportDao {
    @Insert
    suspend fun insert(report: ReportEntity): Long

    @Query("SELECT * FROM reports ORDER BY generatedAt DESC")
    fun observeAll(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE reportType = :type ORDER BY generatedAt DESC")
    fun observeByType(type: ReportType): Flow<List<ReportEntity>>
}

@Dao
interface BookmarkDao {
    @Insert
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Delete
    suspend fun delete(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE eventId = :eventId")
    suspend fun deleteByEventId(eventId: Long)

    @Query("SELECT * FROM bookmarks ORDER BY bookmarkedAt DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE eventId = :eventId)")
    fun observeIsBookmarked(eventId: Long): Flow<Boolean>
}

@Dao
interface SearchDao {
    @Insert
    suspend fun insertSavedSearch(search: SavedSearchEntity): Long

    @Delete
    suspend fun deleteSavedSearch(search: SavedSearchEntity)

    @Query("SELECT * FROM saved_searches ORDER BY createdAt DESC")
    fun observeSavedSearches(): Flow<List<SavedSearchEntity>>

    @Insert
    suspend fun insertRecentSearch(search: RecentSearchEntity): Long

    @Query("SELECT * FROM recent_searches ORDER BY searchedAt DESC LIMIT 20")
    fun observeRecentSearches(): Flow<List<RecentSearchEntity>>

    // Keep only the most recent 20 (Part 2.9 Recent Searches maximum).
    @Query("""
        DELETE FROM recent_searches WHERE recentSearchId NOT IN (
            SELECT recentSearchId FROM recent_searches ORDER BY searchedAt DESC LIMIT 20
        )
    """)
    suspend fun trimRecentSearches()

    @Query("DELETE FROM recent_searches")
    suspend fun clearRecentSearches()
}

@Dao
interface InsightDao {
    @Insert
    suspend fun insert(insight: InsightEntity): Long

    @Insert
    suspend fun insertAll(insights: List<InsightEntity>): List<Long>

    @Query("SELECT * FROM insights ORDER BY generatedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<InsightEntity>>

    @Query("SELECT * FROM insights WHERE relatedAppId = :appId ORDER BY generatedAt DESC")
    fun observeForApp(appId: Long): Flow<List<InsightEntity>>

    @Query("DELETE FROM insights")
    suspend fun deleteAll()
}
