package com.apptimemachine.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.apptimemachine.data.entities.DailyUsageEntity
import com.apptimemachine.data.entities.StorageHistoryEntity
import com.apptimemachine.data.entities.UsageSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StorageHistoryDao {
    @Insert
    suspend fun insert(entry: StorageHistoryEntity): Long

    @Query("SELECT * FROM storage_history WHERE appId = :appId ORDER BY recordedAt DESC")
    fun observeForApp(appId: Long): Flow<List<StorageHistoryEntity>>

    @Query("SELECT * FROM storage_history WHERE appId = :appId ORDER BY recordedAt DESC LIMIT 1")
    suspend fun getLatestForApp(appId: Long): StorageHistoryEntity?

    @Query("""
        SELECT SUM(COALESCE(differenceBytes, 0)) FROM storage_history
        WHERE recordedAt >= :startOfDay
    """)
    fun observeTotalGrowthSince(startOfDay: Long): Flow<Long?>

    @Query("""
        SELECT * FROM storage_history WHERE recordedAt >= :since
        ORDER BY differenceBytes DESC LIMIT 1
    """)
    suspend fun getFastestGrowthSince(since: Long): StorageHistoryEntity?
}

@Dao
interface DailyUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(usage: DailyUsageEntity)

    @Query("SELECT * FROM daily_usage WHERE appId = :appId AND dateEpochDay = :day LIMIT 1")
    suspend fun getForAppAndDay(appId: Long, day: Long): DailyUsageEntity?

    @Query("SELECT * FROM daily_usage WHERE appId = :appId ORDER BY dateEpochDay DESC")
    fun observeForApp(appId: Long): Flow<List<DailyUsageEntity>>

    @Query("SELECT * FROM daily_usage WHERE dateEpochDay = :day ORDER BY foregroundTimeMs DESC")
    suspend fun getAllForDay(day: Long): List<DailyUsageEntity>

    @Query("SELECT SUM(foregroundTimeMs) FROM daily_usage WHERE dateEpochDay = :day")
    fun observeTotalUsageForDay(day: Long): Flow<Long?>

    @Query("""
        SELECT * FROM daily_usage WHERE dateEpochDay = :day
        ORDER BY foregroundTimeMs DESC LIMIT 1
    """)
    suspend fun getMostUsedForDay(day: Long): DailyUsageEntity?

    @Query("SELECT SUM(launchCount) FROM daily_usage WHERE dateEpochDay = :day")
    suspend fun getTotalLaunchesForDay(day: Long): Int?
}

@Dao
interface UsageSessionDao {
    @Insert
    suspend fun insert(session: UsageSessionEntity): Long

    @Update
    suspend fun update(session: UsageSessionEntity)

    @Query("SELECT * FROM usage_sessions WHERE appId = :appId ORDER BY sessionStart DESC LIMIT :limit")
    suspend fun getRecentForApp(appId: Long, limit: Int = 50): List<UsageSessionEntity>

    @Query("SELECT * FROM usage_sessions WHERE sessionEnd IS NULL AND appId = :appId LIMIT 1")
    suspend fun getOpenSession(appId: Long): UsageSessionEntity?
}
