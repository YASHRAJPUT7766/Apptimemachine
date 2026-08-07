package com.apptimemachine.ui.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptimemachine.data.entities.InstalledAppEntity
import com.apptimemachine.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppsListUiState(
    val apps: List<InstalledAppEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class AppsListViewModel @Inject constructor(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val uiState: StateFlow<AppsListUiState> = _query
        .debounce(200)
        .flatMapLatest { q ->
            if (q.isBlank()) appRepository.observeAllActive() else appRepository.search(q)
        }
        .map { AppsListUiState(apps = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppsListUiState())

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun toggleFavorite(appId: Long, isFavorite: Boolean) {
        viewModelScope.launch { appRepository.setFavorite(appId, !isFavorite) }
    }
}
