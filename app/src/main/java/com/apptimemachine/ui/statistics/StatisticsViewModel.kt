package com.apptimemachine.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptimemachine.data.entities.EventCategory
import com.apptimemachine.data.repository.AppRepository
import com.apptimemachine.data.repository.StorageRepository
import com.apptimemachine.data.repository.TimelineRepository
import com.apptimemachine.data.repository.VersionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val dailyEventCounts: List<Float> = emptyList(),  // last 7 days, oldest -> newest
    val dailyEventLabels: List<String> = emptyList(),
    val calendarHeatmap: List<Int> = emptyList(),      // last 30 days
    val totalEvents: Int = 0,
    val mostChangedAppName: String? = null,
    val largestStorageGrowthAppName: String? = null,
    val totalUpdatesThisWeek: Int = 0
)

/**
 * Part 2.8 Statistics & Analytics Engine. Every number here is derived
 * from stored history — no chart is rendered from insufficient data
 * without the caller explicitly checking [StatisticsUiState.totalEvents]
 * first (Part 2.8 Failure Handling: "If data is insufficient, display
 * friendly explanation. Do not generate misleading charts.").
 *
 * Battery-drain-proxy and network-usage data used to live here too; moved
 * to [com.apptimemachine.core.monitoring.MonitoringStatsProvider] since
 * Dashboard, Timeline, and App Details all need the same per-day snapshot
 * data and Statistics is trend charts only, not day snapshots.
 */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val timelineRepository: TimelineRepository,
    private val appRepository: AppRepository,
    private val storageRepository: StorageRepository,
    private val versionRepository: VersionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val zone = ZoneOffset.systemDefault()
            val today = LocalDate.now(zone)

            // Last 7 days event counts
            val dailyCounts = mutableListOf<Float>()
            val dailyLabels = mutableListOf<String>()
            for (i in 6 downTo 0) {
                val day = today.minusDays(i.toLong())
                val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val count = timelineRepository.getEventsBetween(start, end).size
                dailyCounts += count.toFloat()
                dailyLabels += day.dayOfWeek.name.take(3)
            }

            // Last 30 days heatmap
            val heatmap = mutableListOf<Int>()
            for (i in 29 downTo 0) {
                val day = today.minusDays(i.toLong())
                val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                heatmap += timelineRepository.getEventsBetween(start, end).size
            }

            val largestStorageApp = storageRepository.getFastestGrowthSince(
                today.minusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()
            )?.let { entry -> appRepository.findById(entry.appId)?.appName }

            val startOfWeek = today.minusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()
            val weekEvents = timelineRepository.getEventsBetween(startOfWeek, System.currentTimeMillis())
            val updatesThisWeek = weekEvents.count { it.eventCategory == EventCategory.VERSION }

            _uiState.value = StatisticsUiState(
                isLoading = false,
                dailyEventCounts = dailyCounts,
                dailyEventLabels = dailyLabels,
                calendarHeatmap = heatmap,
                totalEvents = dailyCounts.sum().toInt(),
                mostChangedAppName = weekEvents.groupingBy { it.appName }.eachCount().maxByOrNull { it.value }?.key,
                largestStorageGrowthAppName = largestStorageApp,
                totalUpdatesThisWeek = updatesThisWeek
            )
        }
    }
}
