package com.apptimemachine.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.apptimemachine.data.entities.EventCategory
import com.apptimemachine.data.entities.EventSeverity
import com.apptimemachine.data.entities.TimelineEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimelineEventDao {

    @Insert
    suspend fun insert(event: TimelineEventEntity): Long

    @Insert
    suspend fun insertAll(events: List<TimelineEventEntity>): List<Long>

    // Paged, newest-first — required to support 100,000+ events without
    // loading the whole table into memory (Part 3.6 Timeline Performance).
    @Query("SELECT * FROM timeline_events ORDER BY createdTimestamp DESC")
    fun pagingSourceAll(): PagingSource<Int, TimelineEventEntity>

    @Query("""
        SELECT * FROM timeline_events
        WHERE eventCategory = :category
        ORDER BY createdTimestamp DESC
    """)
    fun pagingSourceByCategory(category: EventCategory): PagingSource<Int, TimelineEventEntity>

    @Query("SELECT * FROM timeline_events WHERE appId = :appId ORDER BY createdTimestamp DESC")
    fun pagingSourceForApp(appId: Long): PagingSource<Int, TimelineEventEntity>

    @Query("""
        SELECT * FROM timeline_events
        WHERE (appName LIKE '%' || :query || '%'
            OR packageName LIKE '%' || :query || '%'
            OR eventType LIKE '%' || :query || '%')
        ORDER BY createdTimestamp DESC
    """)
    fun pagingSourceSearch(query: String): PagingSource<Int, TimelineEventEntity>

    // Small, unpaginated queries for the Dashboard's "latest 10" widget.
    @Query("SELECT * FROM timeline_events ORDER BY createdTimestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 10): Flow<List<TimelineEventEntity>>

    @Query("SELECT * FROM timeline_events WHERE eventId = :eventId LIMIT 1")
    suspend fun findById(eventId: Long): TimelineEventEntity?

    @Query("SELECT COUNT(*) FROM timeline_events")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM timeline_events WHERE createdTimestamp >= :startOfDay")
    fun observeCountSince(startOfDay: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM timeline_events
        WHERE eventCategory = :category AND createdTimestamp >= :startOfDay
    """)
    suspend fun countByCategorySince(category: EventCategory, startOfDay: Long): Int

    @Query("""
        SELECT * FROM timeline_events
        WHERE createdTimestamp BETWEEN :start AND :end
        ORDER BY createdTimestamp DESC
    """)
    suspend fun getEventsBetween(start: Long, end: Long): List<TimelineEventEntity>

    @Query("SELECT * FROM timeline_events WHERE appId = :appId ORDER BY createdTimestamp DESC")
    suspend fun getAllForApp(appId: Long): List<TimelineEventEntity>

    @Query("SELECT * FROM timeline_events WHERE severity = :severity ORDER BY createdTimestamp DESC LIMIT :limit")
    suspend fun getBySeverity(severity: EventSeverity, limit: Int): List<TimelineEventEntity>

    @Query("DELETE FROM timeline_events")
    suspend fun deleteAll()

    @Query("DELETE FROM timeline_events WHERE appId = :appId")
    suspend fun deleteForApp(appId: Long)

    // Duplicate-prevention check (Part 1.3 Duplicate Event Prevention):
    // only used for event types where the same old->new pair should not be
    // logged twice in a row for the same app.
    @Query("""
        SELECT COUNT(*) FROM timeline_events
        WHERE appId = :appId AND eventType = :eventType AND newValue = :newValue
        ORDER BY createdTimestamp DESC LIMIT 1
    """)
    suspend fun hasIdenticalLatestEvent(appId: Long, eventType: String, newValue: String?): Int
}
