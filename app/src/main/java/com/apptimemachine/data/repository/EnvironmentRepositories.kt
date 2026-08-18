package com.apptimemachine.data.repository

import com.apptimemachine.data.dao.BatteryHistoryDao
import com.apptimemachine.data.dao.BatteryUsageDao
import com.apptimemachine.data.dao.NetworkHistoryDao
import com.apptimemachine.data.dao.NotificationFeedRow
import com.apptimemachine.data.dao.NotificationHistoryDao
import com.apptimemachine.data.entities.BatteryHistoryEntity
import com.apptimemachine.data.entities.BatteryUsageEntity
import com.apptimemachine.data.entities.NetworkHistoryEntity
import com.apptimemachine.data.entities.NotificationHistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(private val dao: NotificationHistoryDao) {
    fun observeForApp(appId: Long): Flow<List<NotificationHistoryEntity>> = dao.observeForApp(appId)
    fun observeCountToday(startOfDay: Long): Flow<Int> = dao.observeCountToday(startOfDay)
    suspend fun insert(entry: NotificationHistoryEntity): Long = dao.insert(entry)

    /** Full permanent notification log — powers Timeline's Notifications filter and "All". */
    fun observeFeed(limit: Int = 500): Flow<List<NotificationFeedRow>> = dao.observeFeed(limit)

    /** Latest few, for the Dashboard's recent-notifications card. */
    fun observeRecentFeed(): Flow<List<NotificationFeedRow>> = dao.observeRecentFeed()

    /** Removes a notification from the in-app log only — clears it "off the top" without touching the device notification. */
    suspend fun clear(id: Long) = dao.markCleared(id)
    suspend fun clearBatch(ids: List<Long>) = dao.markClearedBatch(ids)

    suspend fun delete(id: Long) = dao.deleteById(id)
    suspend fun deleteBatch(ids: List<Long>) = dao.deleteByIds(ids)
    suspend fun deleteByAppAndPostedAt(appId: Long, postedAt: Long) = dao.deleteByAppAndPostedAt(appId, postedAt)
}

@Singleton
class BatteryRepository @Inject constructor(private val dao: BatteryHistoryDao) {
    fun observeRecent(limit: Int = 50): Flow<List<BatteryHistoryEntity>> = dao.observeRecent(limit)
    fun observeChargingSessionsToday(startOfDay: Long): Flow<Int> = dao.observeChargingSessionsToday(startOfDay)
    suspend fun getOpenSession(): BatteryHistoryEntity? = dao.getOpenSession()
    suspend fun insert(entry: BatteryHistoryEntity): Long = dao.insert(entry)
    suspend fun update(entry: BatteryHistoryEntity) = dao.update(entry)
    suspend fun getRecentSince(since: Long): List<BatteryHistoryEntity> = dao.getRecentSince(since)
}

@Singleton
class NetworkRepository @Inject constructor(private val dao: NetworkHistoryDao) {
    fun observeForApp(appId: Long): Flow<List<NetworkHistoryEntity>> = dao.observeForApp(appId)
    fun observeWifiTotalForDay(day: Long): Flow<Long?> = dao.observeWifiTotalForDay(day)
    fun observeMobileTotalForDay(day: Long): Flow<Long?> = dao.observeMobileTotalForDay(day)
    suspend fun insert(entry: NetworkHistoryEntity): Long = dao.insert(entry)
    suspend fun getAllForDay(day: Long): List<NetworkHistoryEntity> = dao.getAllForDay(day)
    suspend fun getForAppAndDay(appId: Long, day: Long): NetworkHistoryEntity? = dao.getForAppAndDay(appId, day)
}

/** Battery-drain PROXY (see [BatteryUsageEntity] doc) — derived from real usage time, never a claimed measured battery %. */
@Singleton
class BatteryUsageRepository @Inject constructor(private val dao: BatteryUsageDao) {
    fun observeForDay(day: Long): Flow<List<BatteryUsageEntity>> = dao.observeForDay(day)
    suspend fun getTopForDay(day: Long, limit: Int = 10): List<BatteryUsageEntity> = dao.getTopForDay(day, limit)
    fun observeForApp(appId: Long): Flow<List<BatteryUsageEntity>> = dao.observeForApp(appId)
    suspend fun upsert(entry: BatteryUsageEntity): Long = dao.upsert(entry)
    suspend fun upsertAll(entries: List<BatteryUsageEntity>) = dao.upsertAll(entries)
}
