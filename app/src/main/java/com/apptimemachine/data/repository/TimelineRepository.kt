package com.apptimemachine.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.apptimemachine.data.dao.TimelineEventDao
import com.apptimemachine.data.entities.EventCategory
import com.apptimemachine.data.entities.TimelineEventEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimelineRepository @Inject constructor(
    private val dao: TimelineEventDao
) {
    private val pagingConfig = PagingConfig(pageSize = 30, enablePlaceholders = false, prefetchDistance = 10)

    fun pagedTimeline(): Flow<PagingData<TimelineEventEntity>> =
        Pager(pagingConfig) { dao.pagingSourceAll() }.flow

    fun pagedTimelineForCategory(category: EventCategory): Flow<PagingData<TimelineEventEntity>> =
        Pager(pagingConfig) { dao.pagingSourceByCategory(category) }.flow

    fun pagedTimelineForApp(appId: Long): Flow<PagingData<TimelineEventEntity>> =
        Pager(pagingConfig) { dao.pagingSourceForApp(appId) }.flow

    fun pagedSearch(query: String): Flow<PagingData<TimelineEventEntity>> =
        Pager(pagingConfig) { dao.pagingSourceSearch(query) }.flow

    fun observeRecent(limit: Int = 10): Flow<List<TimelineEventEntity>> = dao.observeRecent(limit)
    fun observeTotalCount(): Flow<Int> = dao.observeTotalCount()
    fun observeCountSince(startOfDay: Long): Flow<Int> = dao.observeCountSince(startOfDay)

    suspend fun insert(event: TimelineEventEntity): Long = dao.insert(event)
    suspend fun insertAll(events: List<TimelineEventEntity>): List<Long> = dao.insertAll(events)
    suspend fun getAllForApp(appId: Long): List<TimelineEventEntity> = dao.getAllForApp(appId)
    suspend fun getEventsBetween(start: Long, end: Long): List<TimelineEventEntity> = dao.getEventsBetween(start, end)
    suspend fun countByCategorySince(category: EventCategory, startOfDay: Long): Int =
        dao.countByCategorySince(category, startOfDay)

    suspend fun deleteAll() = dao.deleteAll()
    suspend fun deleteForApp(appId: Long) = dao.deleteForApp(appId)
}
