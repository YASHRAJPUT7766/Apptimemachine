package com.apptimemachine.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptimemachine.core.monitoring.MonitoringManager
import com.apptimemachine.data.dao.CategoryCount
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

data class CategoryStat(val label: String, val count: Int)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val totalApps: Int = 0,
    val systemApps: Int = 0,
    val userApps: Int = 0,
    val disabledApps: Int = 0,
    val topCategories: List<CategoryStat> = emptyList(),
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
private data class AppCounts(
    val total: Int,
    val system: Int,
    val user: Int,
    val disabled: Int,
    val categories: List<CategoryCount>
)
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

    // Pull-to-refresh state (Part 1.4A: with the manual "Scan Now" button
    // removed, swiping down is now the only way to force an on-demand scan;
    // the underlying repositories are all live Flows already, so a
    // successful scan is enough to make every card on this screen update
    // itself — no separate reload call needed here).
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                monitoringManager.performScan(ScanType.MANUAL)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private val startOfDay: Long
        get() = LocalDate.now(ZoneOffset.systemDefault())
            .atStartOfDay(ZoneOffset.systemDefault()).toInstant().toEpochMilli()

    private val appCounts = combine(
        appRepository.observeActiveCount(),
        appRepository.observeSystemAppCount(),
        appRepository.observeUserAppCount(),
        appRepository.observeDisabledCount(),
        appRepository.observeCategoryBreakdown()
    ) { total, system, user, disabled, categories -> AppCounts(total, system, user, disabled, categories) }

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
        appCounts, timelineCounts, todayActivity, scanAndGrowth
    ) { apps, timeline, activity, scanGrowth ->
        DashboardUiState(
            isLoading = false,
            totalApps = apps.total,
            systemApps = apps.system,
            userApps = apps.user,
            disabledApps = apps.disabled,
            topCategories = topCategoryStats(apps.categories),
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

    /** Top 4 categories by count, with the remainder folded into "Others" (Dashboard Top Categories panel). */
    private fun topCategoryStats(categories: List<CategoryCount>): List<CategoryStat> {
        if (categories.isEmpty()) return emptyList()
        val top = categories.take(4)
        val rest = categories.drop(4).sumOf { it.count }
        val stats = top.map { CategoryStat(it.category, it.count) }.toMutableList()
        if (rest > 0) stats += CategoryStat("Others", rest)
        return stats
    }
}
