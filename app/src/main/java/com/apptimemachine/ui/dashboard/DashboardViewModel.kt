package com.apptimemachine.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptimemachine.core.monitoring.DeviceMonitoringSnapshot
import com.apptimemachine.core.monitoring.MonitoringManager
import com.apptimemachine.core.monitoring.MonitoringStatsProvider
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

/** One item in the Dashboard's Insights card — a single rule-based highlight computed fresh each load. */
data class DashboardInsight(
    val icon: InsightIcon,
    val title: String,
    val subtitle: String
)

enum class InsightIcon { USAGE, INSTALL, NOTIFICATIONS, STORAGE, BATTERY }

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
    val chargingSessionsToday: Int = 0,
    val recentNotifications: List<com.apptimemachine.data.dao.NotificationFeedRow> = emptyList()
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
    private val usageRepository: UsageRepository,
    private val scanRepository: ScanRepository,
    private val monitoringManager: MonitoringManager,
    private val monitoringStatsProvider: MonitoringStatsProvider
) : ViewModel() {

    private val _deviceSnapshot = MutableStateFlow(DeviceMonitoringSnapshot())
    /** Today's battery-drain-proxy and network-usage cards (Dashboard). */
    val deviceSnapshot: StateFlow<DeviceMonitoringSnapshot> = _deviceSnapshot.asStateFlow()

    private val _insights = MutableStateFlow<List<DashboardInsight>>(emptyList())
    /** Rule-based highlights for the Insights card — recomputed on load and on refresh. */
    val insights: StateFlow<List<DashboardInsight>> = _insights.asStateFlow()

    init {
        loadDeviceSnapshot()
        loadInsights()
    }

    private fun loadDeviceSnapshot() = viewModelScope.launch {
        _deviceSnapshot.value = monitoringStatsProvider.getDeviceSnapshotToday()
    }

    /**
     * Computes a handful of simple, rule-based highlights straight from
     * today's data — no ML, no stored "insight" rows, just the same
     * repositories the rest of the Dashboard already reads. Recomputed
     * fresh on every load/refresh so it can never go stale like a cached
     * insight would.
     */
    private fun loadInsights() = viewModelScope.launch {
        val today = startOfDay
        val epochDay = LocalDate.now(ZoneOffset.systemDefault()).toEpochDay()
        val weekAgo = today - (7L * 24 * 60 * 60 * 1000)

        val result = mutableListOf<DashboardInsight>()

        // Most-used app today, by foreground time.
        usageRepository.getMostUsedForDay(epochDay)?.let { usage ->
            if (usage.foregroundTimeMs > 0) {
                val app = appRepository.findById(usage.appId)
                if (app != null) {
                    result += DashboardInsight(
                        icon = InsightIcon.USAGE,
                        title = "${app.appName} used the most today",
                        subtitle = "${com.apptimemachine.core.utils.Formatters.duration(usage.foregroundTimeMs)} of screen time so far"
                    )
                }
            }
        }

        // New installs this week.
        val installsThisWeek = timelineRepository.countByCategorySince(
            com.apptimemachine.data.entities.EventCategory.INSTALLATION, weekAgo
        )
        if (installsThisWeek > 0) {
            result += DashboardInsight(
                icon = InsightIcon.INSTALL,
                title = "$installsThisWeek new ${if (installsThisWeek == 1) "app" else "apps"} installed this week",
                subtitle = "Tap Recent Activity to see what changed"
            )
        }

        // Most notification-heavy app today.
        notificationRepository.observeRecentFeed().first().let { recent ->
            val topByApp = recent.groupBy { it.appName }.maxByOrNull { it.value.size }
            if (topByApp != null && topByApp.value.size >= 3) {
                result += DashboardInsight(
                    icon = InsightIcon.NOTIFICATIONS,
                    title = "${topByApp.key} sent the most notifications",
                    subtitle = "${topByApp.value.size} notifications recently"
                )
            }
        }

        // Storage growth today, only surfaced if it's actually notable.
        val growth = storageRepository.observeTotalGrowthSince(today).first()
        if (growth != null && growth > 50L * 1024 * 1024) { // 50MB+
            result += DashboardInsight(
                icon = InsightIcon.STORAGE,
                title = "Storage grew by ${com.apptimemachine.core.utils.Formatters.bytes(growth)} today",
                subtitle = "Across all monitored apps"
            )
        }

        _insights.value = result.take(3)
    }

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
                loadDeviceSnapshot()
                loadInsights()
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
        timelineRepository.observeRecentExcludingNotifications(10)
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

    private val recentNotifications = notificationRepository.observeRecentFeed()

    val uiState: StateFlow<DashboardUiState> = combine(
        appCounts, timelineCounts, todayActivity, scanAndGrowth, recentNotifications
    ) { apps, timeline, activity, scanGrowth, notifications ->
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
            chargingSessionsToday = activity.charging,
            recentNotifications = notifications
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

    /** Deletes a notification from the in-app log (Dashboard's Recent Notifications card action). */
    fun deleteNotification(id: Long) = viewModelScope.launch {
        notificationRepository.delete(id)
    }
}
