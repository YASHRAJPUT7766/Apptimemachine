package com.apptimemachine.data.repository

import com.apptimemachine.data.dao.*
import com.apptimemachine.data.entities.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository @Inject constructor(private val dao: StorageHistoryDao) {
    fun observeForApp(appId: Long): Flow<List<StorageHistoryEntity>> = dao.observeForApp(appId)
    fun observeTotalGrowthSince(startOfDay: Long): Flow<Long?> = dao.observeTotalGrowthSince(startOfDay)
    suspend fun getLatestForApp(appId: Long): StorageHistoryEntity? = dao.getLatestForApp(appId)
    suspend fun getFastestGrowthSince(since: Long): StorageHistoryEntity? = dao.getFastestGrowthSince(since)
    suspend fun insert(entry: StorageHistoryEntity): Long = dao.insert(entry)
}

@Singleton
class UsageRepository @Inject constructor(
    private val dailyUsageDao: DailyUsageDao,
    private val sessionDao: UsageSessionDao
) {
    fun observeForApp(appId: Long): Flow<List<DailyUsageEntity>> = dailyUsageDao.observeForApp(appId)
    fun observeTotalUsageForDay(day: Long): Flow<Long?> = dailyUsageDao.observeTotalUsageForDay(day)
    suspend fun getForAppAndDay(appId: Long, day: Long): DailyUsageEntity? = dailyUsageDao.getForAppAndDay(appId, day)
    suspend fun getAllForDay(day: Long): List<DailyUsageEntity> = dailyUsageDao.getAllForDay(day)
    suspend fun getMostUsedForDay(day: Long): DailyUsageEntity? = dailyUsageDao.getMostUsedForDay(day)
    suspend fun upsert(usage: DailyUsageEntity) = dailyUsageDao.upsert(usage)

    suspend fun insertSession(session: UsageSessionEntity): Long = sessionDao.insert(session)
    suspend fun updateSession(session: UsageSessionEntity) = sessionDao.update(session)
    suspend fun getOpenSession(appId: Long): UsageSessionEntity? = sessionDao.getOpenSession(appId)
}

@Singleton
class VersionRepository @Inject constructor(private val dao: VersionHistoryDao) {
    fun observeForApp(appId: Long): Flow<List<VersionHistoryEntity>> = dao.observeForApp(appId)
    fun observeUpdatesToday(startOfDay: Long): Flow<Int> = dao.observeUpdatesToday(startOfDay)
    suspend fun getUpdateCount(appId: Long): Int = dao.getUpdateCount(appId)
    suspend fun insert(entry: VersionHistoryEntity): Long = dao.insert(entry)
}

@Singleton
class PermissionRepository @Inject constructor(private val dao: PermissionHistoryDao) {
    fun observeForApp(appId: Long): Flow<List<PermissionHistoryEntity>> = dao.observeForApp(appId)
    fun observeGrantedToday(startOfDay: Long): Flow<Int> = dao.observeGrantedToday(startOfDay)
    fun observeRevokedToday(startOfDay: Long): Flow<Int> = dao.observeRevokedToday(startOfDay)
    suspend fun insertAll(entries: List<PermissionHistoryEntity>): List<Long> = dao.insertAll(entries)
}

@Singleton
class ScanRepository @Inject constructor(private val dao: ScanHistoryDao) {
    fun observeRecent(limit: Int = 20): Flow<List<ScanHistoryEntity>> = dao.observeRecent(limit)
    fun observeLatest(): Flow<ScanHistoryEntity?> = dao.observeLatest()
    suspend fun insert(scan: ScanHistoryEntity): Long = dao.insert(scan)
    suspend fun update(scan: ScanHistoryEntity) = dao.update(scan)
}

@Singleton
class ExportHistoryRepository @Inject constructor(private val dao: ExportHistoryDao) {
    fun observeAll(): Flow<List<ExportHistoryEntity>> = dao.observeAll()
    suspend fun insert(export: ExportHistoryEntity): Long = dao.insert(export)
    suspend fun deleteAll() = dao.deleteAll()
}

@Singleton
class BackupHistoryRepository @Inject constructor(private val dao: BackupHistoryDao) {
    fun observeAll(): Flow<List<BackupHistoryEntity>> = dao.observeAll()
    suspend fun getLatest(): BackupHistoryEntity? = dao.getLatest()
    suspend fun insert(backup: BackupHistoryEntity): Long = dao.insert(backup)
    suspend fun update(backup: BackupHistoryEntity) = dao.update(backup)
    suspend fun deleteAll() = dao.deleteAll()
}

@Singleton
class ReportRepository @Inject constructor(private val dao: ReportDao) {
    fun observeAll(): Flow<List<ReportEntity>> = dao.observeAll()
    fun observeByType(type: ReportType): Flow<List<ReportEntity>> = dao.observeByType(type)
    suspend fun insert(report: ReportEntity): Long = dao.insert(report)
}
