package com.apptimemachine.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptimemachine.data.entities.InstalledAppEntity
import com.apptimemachine.data.entities.RecentSearchEntity
import com.apptimemachine.data.entities.TimelineEventEntity
import com.apptimemachine.data.repository.AppRepository
import com.apptimemachine.data.repository.SearchRepository
import com.apptimemachine.data.repository.TimelineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val matchingApps: List<InstalledAppEntity> = emptyList(),
    val recentSearches: List<RecentSearchEntity> = emptyList(),
    val isSearching: Boolean = false
)

/**
 * Part 2.9 Advanced Search Engine — real-time, debounced search across
 * Applications from one query box (Part 2.9 Global Search: "accessible
 * from anywhere ... search without navigating to a specific section").
 * Timeline search reuses the same debounced-query pattern via
 * TimelineRepository.pagedSearch() directly inside TimelineScreen when
 * the user drills into timeline-specific search — kept out of this
 * combined view to avoid duplicating paging state across two screens.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")

    private val appResults: Flow<List<InstalledAppEntity>> = _query
        .debounce(250)
        .flatMapLatest { q -> if (q.isBlank()) flowOf(emptyList()) else appRepository.search(q) }

    val uiState: StateFlow<SearchUiState> = combine(
        _query, appResults, searchRepository.observeRecentSearches()
    ) { query, apps, recents ->
        SearchUiState(query = query, matchingApps = apps, recentSearches = recents, isSearching = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState())

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun commitSearch(query: String, resultCount: Int) {
        if (query.isBlank()) return
        viewModelScope.launch {
            searchRepository.insertRecentSearch(
                RecentSearchEntity(keyword = query, resultCount = resultCount, category = "Applications")
            )
            searchRepository.trimRecentSearches()
        }
    }

    fun useRecentSearch(keyword: String) {
        _query.value = keyword
    }

    fun clearRecentSearches() {
        viewModelScope.launch { searchRepository.clearRecentSearches() }
    }
}
