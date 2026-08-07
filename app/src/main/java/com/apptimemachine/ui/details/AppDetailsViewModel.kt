package com.apptimemachine.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptimemachine.data.entities.InstalledAppEntity
import com.apptimemachine.data.entities.PermissionHistoryEntity
import com.apptimemachine.data.entities.StorageHistoryEntity
import com.apptimemachine.data.entities.TimelineEventEntity
import com.apptimemachine.data.entities.VersionHistoryEntity
import com.apptimemachine.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppDetailsUiState(
    val app: InstalledAppEntity? = null,
    val storageHistory: List<StorageHistoryEntity> = emptyList(),
    val versionHistory: List<VersionHistoryEntity> = emptyList(),
    val permissionHistory: List<PermissionHistoryEntity> = emptyList(),
    val timeline: List<TimelineEventEntity> = emptyList(),
    val isLoading: Boolean = true
)

/** Part 2.7 Application Details — Overview/Timeline/Storage/Version/Permissions tabs share this one ViewModel. */
@HiltViewModel
class AppDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val appRepository: AppRepository,
    private val timelineRepository: TimelineRepository,
    private val storageRepository: StorageRepository,
    private val versionRepository: VersionRepository,
    private val permissionRepository: PermissionRepository
) : ViewModel() {

    private val appId: Long = checkNotNull(savedStateHandle["appId"])

    val uiState: StateFlow<AppDetailsUiState> = combine(
        appRepository.observeById(appId),
        storageRepository.observeForApp(appId),
        versionRepository.observeForApp(appId),
        permissionRepository.observeForApp(appId)
    ) { app, storage, version, permissions ->
        AppDetailsUiState(
            app = app,
            storageHistory = storage,
            versionHistory = version,
            permissionHistory = permissions,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppDetailsUiState())

    init {
        viewModelScope.launch {
            val events = timelineRepository.getAllForApp(appId)
            _timelineState.value = events
        }
    }

    private val _timelineState = MutableStateFlow<List<TimelineEventEntity>>(emptyList())
    val timelineState: StateFlow<List<TimelineEventEntity>> = _timelineState.asStateFlow()

    fun toggleFavorite() {
        val app = uiState.value.app ?: return
        viewModelScope.launch { appRepository.setFavorite(app.appId, !app.isFavorite) }
    }
}
