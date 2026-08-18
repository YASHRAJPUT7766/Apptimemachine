package com.apptimemachine.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.apptimemachine.data.entities.BatteryUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: BatteryUsageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<BatteryUsageEntity>)

    @Query("SELECT * FROM battery_usage WHERE dateEpochDay = :day ORDER BY proxySharePercent DESC")
    fun observeForDay(day: Long): Flow<List<BatteryUsageEntity>>

    @Query("SELECT * FROM battery_usage WHERE dateEpochDay = :day ORDER BY proxySharePercent DESC LIMIT :limit")
    suspend fun getTopForDay(day: Long, limit: Int = 10): List<BatteryUsageEntity>

    @Query("SELECT * FROM battery_usage WHERE appId = :appId ORDER BY dateEpochDay DESC")
    fun observeForApp(appId: Long): Flow<List<BatteryUsageEntity>>

    @Query("SELECT * FROM battery_usage WHERE appId = :appId AND dateEpochDay = :day LIMIT 1")
    suspend fun getForAppAndDay(appId: Long, day: Long): BatteryUsageEntity?
}
