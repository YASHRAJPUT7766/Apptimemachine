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
    val dailyUsage: List<com.apptimemachine.data.entities.DailyUsageEntity> = emptyList(),
    val timeline: List<TimelineEventEntity> = emptyList(),
    val hasUsageAccess: Boolean = true,
    val isLoading: Boolean = true
) {
    /** Currently-granted permissions, newest-relevant-first, parsed from the live snapshot (not history). */
    val grantedPermissions: List<String>
        get() = app?.grantedPermissionsSnapshot
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.sorted()
            ?: emptyList()

    /** Today's foreground time for this app, or null if usage access isn't granted / no data yet. */
    val todayUsageMs: Long?
        get() = dailyUsage.maxByOrNull { it.dateEpochDay }?.foregroundTimeMs

    /** Average daily foreground time over the tracked window, used as the ring's reference point. */
    val averageDailyUsageMs: Long?
        get() = dailyUsage.filter { it.foregroundTimeMs > 0 }
            .takeIf { it.isNotEmpty() }
            ?.let { list -> list.sumOf { it.foregroundTimeMs } / list.size }
}

/** Part 2.7 Application Details — Overview/Timeline/Storage/Version/Permissions tabs share this one ViewModel. */
@HiltViewModel
class AppDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val appRepository: AppRepository,
    private val timelineRepository: TimelineRepository,
    private val storageRepository: StorageRepository,
    private val versionRepository: VersionRepository,
    private val permissionRepository: PermissionRepository,
    private val usageRepository: UsageRepository,
    private val usageStatsReader: com.apptimemachine.core.monitoring.UsageStatsReader
) : ViewModel() {

    private val appId: Long = checkNotNull(savedStateHandle["appId"])

    val uiState: StateFlow<AppDetailsUiState> = combine(
        appRepository.observeById(appId),
        storageRepository.observeForApp(appId),
        versionRepository.observeForApp(appId),
        permissionRepository.observeForApp(appId),
        usageRepository.observeForApp(appId)
    ) { app, storage, version, permissions, usage ->
        AppDetailsUiState(
            app = app,
            storageHistory = storage,
            versionHistory = version,
            permissionHistory = permissions,
            dailyUsage = usage,
            hasUsageAccess = usageStatsReader.hasUsageAccessPermission(),
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
