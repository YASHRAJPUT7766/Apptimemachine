package com.apptimemachine.data.repository

import com.apptimemachine.data.dao.BatteryHistoryDao
import com.apptimemachine.data.dao.NetworkHistoryDao
import com.apptimemachine.data.dao.NotificationHistoryDao
import com.apptimemachine.data.entities.BatteryHistoryEntity
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
}

@Singleton
class BatteryRepository @Inject constructor(private val dao: BatteryHistoryDao) {
    fun observeRecent(limit: Int = 50): Flow<List<BatteryHistoryEntity>> = dao.observeRecent(limit)
    fun observeChargingSessionsToday(startOfDay: Long): Flow<Int> = dao.observeChargingSessionsToday(startOfDay)
    suspend fun getOpenSession(): BatteryHistoryEntity? = dao.getOpenSession()
    suspend fun insert(entry: BatteryHistoryEntity): Long = dao.insert(entry)
    suspend fun update(entry: BatteryHistoryEntity) = dao.update(entry)
}

@Singleton
class NetworkRepository @Inject constructor(private val dao: NetworkHistoryDao) {
    fun observeForApp(appId: Long): Flow<List<NetworkHistoryEntity>> = dao.observeForApp(appId)
    fun observeWifiTotalForDay(day: Long): Flow<Long?> = dao.observeWifiTotalForDay(day)
    fun observeMobileTotalForDay(day: Long): Flow<Long?> = dao.observeMobileTotalForDay(day)
    suspend fun insert(entry: NetworkHistoryEntity): Long = dao.insert(entry)
}
