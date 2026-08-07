package com.apptimemachine.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.apptimemachine.data.entities.PermissionHistoryEntity
import com.apptimemachine.data.entities.PermissionState
import com.apptimemachine.data.entities.VersionHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VersionHistoryDao {
    @Insert
    suspend fun insert(entry: VersionHistoryEntity): Long

    @Query("SELECT * FROM version_history WHERE appId = :appId ORDER BY changedAt DESC")
    fun observeForApp(appId: Long): Flow<List<VersionHistoryEntity>>

    @Query("SELECT COUNT(*) FROM version_history WHERE appId = :appId AND changeType = 'UPDATED'")
    suspend fun getUpdateCount(appId: Long): Int

    @Query("SELECT * FROM version_history WHERE changedAt >= :startOfDay ORDER BY changedAt DESC")
    suspend fun getSince(startOfDay: Long): List<VersionHistoryEntity>

    @Query("SELECT COUNT(*) FROM version_history WHERE changeType = 'UPDATED' AND changedAt >= :startOfDay")
    fun observeUpdatesToday(startOfDay: Long): Flow<Int>
}

@Dao
interface PermissionHistoryDao {
    @Insert
    suspend fun insert(entry: PermissionHistoryEntity): Long

    @Insert
    suspend fun insertAll(entries: List<PermissionHistoryEntity>): List<Long>

    @Query("SELECT * FROM permission_history WHERE appId = :appId ORDER BY changedAt DESC")
    fun observeForApp(appId: Long): Flow<List<PermissionHistoryEntity>>

    @Query("""
        SELECT COUNT(*) FROM permission_history
        WHERE changedAt >= :startOfDay AND currentState = 'GRANTED'
    """)
    fun observeGrantedToday(startOfDay: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM permission_history
        WHERE changedAt >= :startOfDay AND currentState = 'DENIED'
    """)
    fun observeRevokedToday(startOfDay: Long): Flow<Int>

    @Query("SELECT * FROM permission_history WHERE changedAt >= :startOfDay ORDER BY changedAt DESC")
    suspend fun getChangedSince(startOfDay: Long): List<PermissionHistoryEntity>
}
