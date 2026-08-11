package com.apptimemachine.data.repository

import com.apptimemachine.data.dao.CategoryCount
import com.apptimemachine.data.dao.InstalledAppDao
import com.apptimemachine.data.entities.InstalledAppEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstracts InstalledAppDao from ViewModels (Part 3.9 Repository
 * Responsibilities: "Hide implementation details from ViewModels").
 * ViewModels must never touch the DAO directly.
 */
@Singleton
class AppRepository @Inject constructor(
    private val dao: InstalledAppDao
) {
    fun observeAllActive(): Flow<List<InstalledAppEntity>> = dao.observeAllActive()
    fun observeById(appId: Long): Flow<InstalledAppEntity?> = dao.observeById(appId)
    fun observeActiveCount(): Flow<Int> = dao.observeActiveCount()
    fun observeSystemAppCount(): Flow<Int> = dao.observeSystemAppCount()
    fun observeUserAppCount(): Flow<Int> = dao.observeUserAppCount()
    fun observeDisabledCount(): Flow<Int> = dao.observeDisabledCount()
    fun observeCategoryBreakdown(): Flow<List<CategoryCount>> = dao.observeCategoryBreakdown()
    fun observeFavorites(): Flow<List<InstalledAppEntity>> = dao.observeFavorites()
    fun search(query: String): Flow<List<InstalledAppEntity>> = dao.search(query)

    suspend fun findByPackageName(packageName: String): InstalledAppEntity? =
        dao.findByPackageName(packageName)

    suspend fun findById(appId: Long): InstalledAppEntity? = dao.findById(appId)

    suspend fun insert(app: InstalledAppEntity): Long = dao.insert(app)
    suspend fun update(app: InstalledAppEntity) = dao.update(app)

    suspend fun setFavorite(appId: Long, isFavorite: Boolean) = dao.setFavorite(appId, isFavorite)
    suspend fun setIgnored(appId: Long, isIgnored: Boolean) = dao.setIgnored(appId, isIgnored)

    suspend fun markRemoved(packageName: String, removedAt: Long) =
        dao.markRemoved(packageName, removedAt)

    suspend fun markReinstalled(packageName: String) = dao.markReinstalled(packageName)

    suspend fun getLargestByStorage(limit: Int = 10): List<InstalledAppEntity> =
        dao.getLargestByStorage(limit)

    suspend fun getAllIncludingRemoved(): List<InstalledAppEntity> = dao.getAllIncludingRemoved()
}
