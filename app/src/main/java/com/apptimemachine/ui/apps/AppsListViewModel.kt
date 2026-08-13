package com.apptimemachine.ui.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptimemachine.data.entities.InstalledAppEntity
import com.apptimemachine.data.repository.AppRepository
import com.apptimemachine.ui.components.CATEGORY_ALL
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One selectable entry in the category filter row, with how many apps currently fall under it. */
data class CategoryFilterOption(
    val label: String,
    val count: Int
)

data class AppsListUiState(
    val apps: List<InstalledAppEntity> = emptyList(),
    val categories: List<CategoryFilterOption> = emptyList(),
    val selectedCategory: String = CATEGORY_ALL,
    val isLoading: Boolean = true
)

@HiltViewModel
class AppsListViewModel @Inject constructor(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedCategory = MutableStateFlow(CATEGORY_ALL)

    // The searched/filtered app list itself: text query narrows first (or
    // "all active" when blank), then the chosen category chip narrows
    // further on top of that — same "All" sentinel category picking
    // pattern as Timeline's event-category filter.
    private val filteredApps: Flow<List<InstalledAppEntity>> = combine(
        _query.debounce(200).flatMapLatest { q ->
            if (q.isBlank()) appRepository.observeAllActive() else appRepository.search(q)
        },
        _selectedCategory
    ) { apps, category ->
        if (category == CATEGORY_ALL) apps
        else apps.filter { (it.category ?: "Uncategorized") == category }
    }

    // Category chips + counts always reflect the FULL active app set (not
    // the current search), so switching categories never hides categories
    // that simply don't match today's search text.
    private val categoryOptions: Flow<List<CategoryFilterOption>> =
        appRepository.observeCategoryBreakdown().map { breakdown ->
            val total = breakdown.sumOf { it.count }
            listOf(CategoryFilterOption(CATEGORY_ALL, total)) +
                breakdown.map { CategoryFilterOption(it.category, it.count) }
        }

    val uiState: StateFlow<AppsListUiState> = combine(
        filteredApps, categoryOptions, _selectedCategory
    ) { apps, categories, selected ->
        AppsListUiState(apps = apps, categories = categories, selectedCategory = selected, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppsListUiState())

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun toggleFavorite(appId: Long, isFavorite: Boolean) {
        viewModelScope.launch { appRepository.setFavorite(appId, !isFavorite) }
    }
}
