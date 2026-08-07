package com.apptimemachine.data.dao

import androidx.room.*
import com.apptimemachine.data.entities.InstalledAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InstalledAppDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(app: InstalledAppEntity): Long

    @Update
    suspend fun update(app: InstalledAppEntity)

    @Query("SELECT * FROM installed_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun findByPackageName(packageName: String): InstalledAppEntity?

    @Query("SELECT * FROM installed_apps WHERE appId = :appId LIMIT 1")
    suspend fun findById(appId: Long): InstalledAppEntity?

    @Query("SELECT * FROM installed_apps WHERE appId = :appId LIMIT 1")
    fun observeById(appId: Long): Flow<InstalledAppEntity?>

    // Only non-removed apps by default — removed apps stay queryable via
    // findByPackageName / getAllIncludingRemoved for reinstall detection (Part 2.2).
    @Query("SELECT * FROM installed_apps WHERE isRemoved = 0 ORDER BY appName ASC")
    fun observeAllActive(): Flow<List<InstalledAppEntity>>

    @Query("SELECT * FROM installed_apps")
    suspend fun getAllIncludingRemoved(): List<InstalledAppEntity>

    @Query("SELECT COUNT(*) FROM installed_apps WHERE isRemoved = 0")
    fun observeActiveCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM installed_apps WHERE isRemoved = 0 AND isSystemApp = 1")
    fun observeSystemAppCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM installed_apps WHERE isRemoved = 0 AND isSystemApp = 0")
    fun observeUserAppCount(): Flow<Int>

    @Query("SELECT * FROM installed_apps WHERE isFavorite = 1 ORDER BY appName ASC LIMIT 10")
    fun observeFavorites(): Flow<List<InstalledAppEntity>>

    @Query("""
        SELECT * FROM installed_apps
        WHERE isRemoved = 0 AND (appName LIKE '%' || :query || '%' OR packageName LIKE '%' || :query || '%')
        ORDER BY appName ASC
    """)
    fun search(query: String): Flow<List<InstalledAppEntity>>

    @Query("UPDATE installed_apps SET isFavorite = :isFavorite WHERE appId = :appId")
    suspend fun setFavorite(appId: Long, isFavorite: Boolean)

    @Query("UPDATE installed_apps SET isIgnored = :isIgnored WHERE appId = :appId")
    suspend fun setIgnored(appId: Long, isIgnored: Boolean)

    @Query("UPDATE installed_apps SET isRemoved = 1, removedAt = :removedAt WHERE packageName = :packageName")
    suspend fun markRemoved(packageName: String, removedAt: Long)

    @Query("UPDATE installed_apps SET isRemoved = 0, removedAt = NULL WHERE packageName = :packageName")
    suspend fun markReinstalled(packageName: String)

    @Query("""
        SELECT * FROM installed_apps
        WHERE isRemoved = 0
        ORDER BY (COALESCE(appSizeBytes,0) + COALESCE(dataSizeBytes,0) + COALESCE(cacheSizeBytes,0)) DESC
        LIMIT :limit
    """)
    suspend fun getLargestByStorage(limit: Int = 10): List<InstalledAppEntity>
}
