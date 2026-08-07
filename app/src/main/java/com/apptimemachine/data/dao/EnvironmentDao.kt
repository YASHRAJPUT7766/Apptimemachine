package com.apptimemachine.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.apptimemachine.data.entities.BatteryHistoryEntity
import com.apptimemachine.data.entities.NetworkHistoryEntity
import com.apptimemachine.data.entities.NotificationHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationHistoryDao {
    @Insert
    suspend fun insert(entry: NotificationHistoryEntity): Long

    @Query("SELECT * FROM notification_history WHERE appId = :appId ORDER BY postedAt DESC")
    fun observeForApp(appId: Long): Flow<List<NotificationHistoryEntity>>

    @Query("SELECT COUNT(*) FROM notification_history WHERE postedAt >= :startOfDay")
    fun observeCountToday(startOfDay: Long): Flow<Int>

    @Query("""
        SELECT appId, COUNT(*) as cnt FROM notification_history
        WHERE postedAt >= :startOfDay GROUP BY appId ORDER BY cnt DESC LIMIT 1
    """)
    suspend fun getMostActiveAppToday(startOfDay: Long): AppNotificationCount?
}

data class AppNotificationCount(val appId: Long, val cnt: Int)

@Dao
interface BatteryHistoryDao {
    @Insert
    suspend fun insert(entry: BatteryHistoryEntity): Long

    @Update
    suspend fun update(entry: BatteryHistoryEntity)

    @Query("SELECT * FROM battery_history ORDER BY startTime DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<BatteryHistoryEntity>>

    @Query("SELECT * FROM battery_history WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getOpenSession(): BatteryHistoryEntity?

    @Query("SELECT COUNT(*) FROM battery_history WHERE eventType = 'CHARGING_STARTED' AND startTime >= :startOfDay")
    fun observeChargingSessionsToday(startOfDay: Long): Flow<Int>
}

@Dao
interface NetworkHistoryDao {
    @Insert
    suspend fun insert(entry: NetworkHistoryEntity): Long

    @Query("SELECT * FROM network_history WHERE appId = :appId ORDER BY dateEpochDay DESC")
    fun observeForApp(appId: Long): Flow<List<NetworkHistoryEntity>>

    @Query("SELECT * FROM network_history WHERE dateEpochDay = :day")
    suspend fun getAllForDay(day: Long): List<NetworkHistoryEntity>

    @Query("SELECT SUM(wifiRxBytes + wifiTxBytes) FROM network_history WHERE dateEpochDay = :day")
    fun observeWifiTotalForDay(day: Long): Flow<Long?>

    @Query("SELECT SUM(mobileRxBytes + mobileTxBytes) FROM network_history WHERE dateEpochDay = :day")
    fun observeMobileTotalForDay(day: Long): Flow<Long?>
}
