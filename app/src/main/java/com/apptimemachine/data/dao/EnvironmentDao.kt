package com.apptimemachine.data.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.apptimemachine.data.entities.BatteryHistoryEntity
import com.apptimemachine.data.entities.NetworkHistoryEntity
import com.apptimemachine.data.entities.NotificationHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationHistoryDao {
    @Insert
    suspend fun insert(entry: NotificationHistoryEntity): Long

    @Query("SELECT * FROM notification_history WHERE appId = :appId AND clearedByUser = 0 ORDER BY postedAt DESC")
    fun observeForApp(appId: Long): Flow<List<NotificationHistoryEntity>>

    @Query("SELECT COUNT(*) FROM notification_history WHERE postedAt >= :startOfDay AND clearedByUser = 0")
    fun observeCountToday(startOfDay: Long): Flow<Int>

    @Query("""
        SELECT appId, COUNT(*) as cnt FROM notification_history
        WHERE postedAt >= :startOfDay AND clearedByUser = 0 GROUP BY appId ORDER BY cnt DESC LIMIT 1
    """)
    suspend fun getMostActiveAppToday(startOfDay: Long): AppNotificationCount?

    /**
     * Full permanent log joined with app name/icon — powers the
     * Notifications filter tab in Timeline and the Dashboard's recent
     * notifications card. Excludes rows the user cleared from the log
     * (clearedByUser=1) and REMOVED-type rows (those just mark when a
     * POSTED notification left the status bar — showing both would
     * duplicate every entry).
     */
    @Query("""
        SELECT n.*, a.appName as appName, a.packageName as packageName, a.iconCachePath as iconCachePath
        FROM notification_history n
        INNER JOIN installed_apps a ON a.appId = n.appId
        WHERE n.clearedByUser = 0 AND n.eventType != 'REMOVED'
        ORDER BY n.postedAt DESC
        LIMIT :limit
    """)
    fun observeFeed(limit: Int = 500): Flow<List<NotificationFeedRow>>

    @Query("""
        SELECT n.*, a.appName as appName, a.packageName as packageName, a.iconCachePath as iconCachePath
        FROM notification_history n
        INNER JOIN installed_apps a ON a.appId = n.appId
        WHERE n.clearedByUser = 0 AND n.eventType != 'REMOVED'
        ORDER BY n.postedAt DESC
        LIMIT 5
    """)
    fun observeRecentFeed(): Flow<List<NotificationFeedRow>>

    @Query("UPDATE notification_history SET clearedByUser = 1 WHERE notificationHistoryId = :id")
    suspend fun markCleared(id: Long)

    @Query("UPDATE notification_history SET clearedByUser = 1 WHERE notificationHistoryId IN (:ids)")
    suspend fun markClearedBatch(ids: List<Long>)

    @Query("DELETE FROM notification_history WHERE notificationHistoryId = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notification_history WHERE notificationHistoryId IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    // Used when deleting from the Timeline row, which only carries
    // (appId, createdTimestamp) — not the notification_history row's own
    // id — since the Timeline event is a separate lightweight record.
    @Query("DELETE FROM notification_history WHERE appId = :appId AND postedAt = :postedAt AND eventType != 'REMOVED'")
    suspend fun deleteByAppAndPostedAt(appId: Long, postedAt: Long)
}

data class AppNotificationCount(val appId: Long, val cnt: Int)

/** Notification row + the fields Timeline/Dashboard cards need to render without a second lookup. */
data class NotificationFeedRow(
    @Embedded val notification: NotificationHistoryEntity,
    val appName: String,
    val packageName: String,
    val iconCachePath: String?
)

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

    @Query("SELECT * FROM battery_history WHERE startTime >= :since ORDER BY startTime DESC")
    suspend fun getRecentSince(since: Long): List<BatteryHistoryEntity>
}

@Dao
interface NetworkHistoryDao {
    // Upsert (not plain insert): with the unique (appId, dateEpochDay)
    // index, this updates today's existing row on every scan instead of
    // inserting a new one each time — fixes the same app showing up
    // repeated multiple times in the Network Usage list.
    @Upsert
    suspend fun insert(entry: NetworkHistoryEntity): Long

    @Query("SELECT * FROM network_history WHERE appId = :appId ORDER BY dateEpochDay DESC")
    fun observeForApp(appId: Long): Flow<List<NetworkHistoryEntity>>

    @Query("SELECT * FROM network_history WHERE dateEpochDay = :day")
    suspend fun getAllForDay(day: Long): List<NetworkHistoryEntity>

    // Looked up before each upsert so scanNetwork() can carry over today's
    // existing row id — an @Upsert with a fresh autoGenerate id of 0 would
    // otherwise still insert a new row instead of updating the matching one.
    @Query("SELECT * FROM network_history WHERE appId = :appId AND dateEpochDay = :day LIMIT 1")
    suspend fun getForAppAndDay(appId: Long, day: Long): NetworkHistoryEntity?

    @Query("SELECT SUM(wifiRxBytes + wifiTxBytes) FROM network_history WHERE dateEpochDay = :day")
    fun observeWifiTotalForDay(day: Long): Flow<Long?>

    @Query("SELECT SUM(mobileRxBytes + mobileTxBytes) FROM network_history WHERE dateEpochDay = :day")
    fun observeMobileTotalForDay(day: Long): Flow<Long?>
}
