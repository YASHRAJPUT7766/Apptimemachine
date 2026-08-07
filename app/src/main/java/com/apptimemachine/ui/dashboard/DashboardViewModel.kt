package com.apptimemachine.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptimemachine.core.monitoring.MonitoringManager
import com.apptimemachine.data.entities.ScanHistoryEntity
import com.apptimemachine.data.entities.ScanType
import com.apptimemachine.data.entities.TimelineEventEntity
import com.apptimemachine.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val isScanning: Boolean = false,
    val totalApps: Int = 0,
    val systemApps: Int = 0,
    val userApps: Int = 0,
    val totalTimelineEvents: Int = 0,
    val eventsToday: Int = 0,
    val recentEvents: List<TimelineEventEntity> = emptyList(),
    val lastScan: ScanHistoryEntity? = null,
    val storageGrowthToday: Long? = null,
    val updatesToday: Int = 0,
    val permissionsGrantedToday: Int = 0,
    val permissionsRevokedToday: Int = 0,
    val notificationsToday: Int = 0,
    val chargingSessionsToday: Int = 0
)

// Small intermediate groupings keep the final combine() call fully
// type-safe (no unchecked casts) while staying under practical arity limits.
private data class AppCounts(val total: Int, val system: Int, val user: Int)
private data class TimelineCounts(val total: Int, val today: Int, val recent: List<TimelineEventEntity>)
private data class TodayActivity(
    val updates: Int, val granted: Int, val revoked: Int, val notifs: Int, val charging: Int
)
private data class ScanAndGrowth(val lastScan: ScanHistoryEntity?, val growth: Long?)

/**
 * Part 3.5 Dashboard Engine. Only talks to repositories, never DAOs
 * directly (Part 3.9 ViewModel rule). Every field is a live Flow so the
 * UI refreshes automatically whenever a scan writes new rows.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val timelineRepository: TimelineRepository,
    private val storageRepository: StorageRepository,
    private val versionRepository: VersionRepository,
    private val permissionRepository: PermissionRepository,
    private val notificationRepository: NotificationRepository,
    private val batteryRepository: BatteryRepository,
    private val scanRepository: ScanRepository,
    private val monitoringManager: MonitoringManager
) : ViewModel() {

    private val startOfDay: Long
        get() = LocalDate.now(ZoneOffset.systemDefault())
            .atStartOfDay(ZoneOffset.systemDefault()).toInstant().toEpochMilli()

    private val _isScanning = MutableStateFlow(false)

    private val appCounts = combine(
        appRepository.observeActiveCount(),
        appRepository.observeSystemAppCount(),
        appRepository.observeUserAppCount()
    ) { total, system, user -> AppCounts(total, system, user) }

    private val timelineCounts = combine(
        timelineRepository.observeTotalCount(),
        timelineRepository.observeCountSince(startOfDay),
        timelineRepository.observeRecent(10)
    ) { total, today, recent -> TimelineCounts(total, today, recent) }

    private val todayActivity = combine(
        versionRepository.observeUpdatesToday(startOfDay),
        permissionRepository.observeGrantedToday(startOfDay),
        permissionRepository.observeRevokedToday(startOfDay),
        notificationRepository.observeCountToday(startOfDay),
        batteryRepository.observeChargingSessionsToday(startOfDay)
    ) { updates, granted, revoked, notifs, charging ->
        TodayActivity(updates, granted, revoked, notifs, charging)
    }

    private val scanAndGrowth = combine(
        scanRepository.observeLatest(),
        storageRepository.observeTotalGrowthSince(startOfDay)
    ) { lastScan, growth -> ScanAndGrowth(lastScan, growth) }

    val uiState: StateFlow<DashboardUiState> = combine(
        appCounts, timelineCounts, todayActivity, scanAndGrowth, _isScanning
    ) { apps, timeline, activity, scanGrowth, scanning ->
        DashboardUiState(
            isLoading = false,
            isScanning = scanning,
            totalApps = apps.total,
            systemApps = apps.system,
            userApps = apps.user,
            totalTimelineEvents = timeline.total,
            eventsToday = timeline.today,
            recentEvents = timeline.recent,
            lastScan = scanGrowth.lastScan,
            storageGrowthToday = scanGrowth.growth,
            updatesToday = activity.updates,
            permissionsGrantedToday = activity.granted,
            permissionsRevokedToday = activity.revoked,
            notificationsToday = activity.notifs,
            chargingSessionsToday = activity.charging
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun runManualScan() {
        viewModelScope.launch {
            _isScanning.value = true
            runCatching { monitoringManager.performScan(ScanType.MANUAL) }
            _isScanning.value = false
        }
    }
}
