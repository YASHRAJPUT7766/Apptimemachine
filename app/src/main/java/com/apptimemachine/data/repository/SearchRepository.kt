package com.apptimemachine.data.repository

import com.apptimemachine.data.dao.SearchDao
import com.apptimemachine.data.entities.RecentSearchEntity
import com.apptimemachine.data.entities.SavedSearchEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Part 2.9 Advanced Search Engine — saved searches and the capped-at-20 recent searches list. */
@Singleton
class SearchRepository @Inject constructor(private val dao: SearchDao) {

    fun observeSavedSearches(): Flow<List<SavedSearchEntity>> = dao.observeSavedSearches()
    suspend fun insertSavedSearch(search: SavedSearchEntity): Long = dao.insertSavedSearch(search)
    suspend fun deleteSavedSearch(search: SavedSearchEntity) = dao.deleteSavedSearch(search)

    fun observeRecentSearches(): Flow<List<RecentSearchEntity>> = dao.observeRecentSearches()
    suspend fun insertRecentSearch(search: RecentSearchEntity): Long = dao.insertRecentSearch(search)
    suspend fun trimRecentSearches() = dao.trimRecentSearches()
    suspend fun clearRecentSearches() = dao.clearRecentSearches()
}
