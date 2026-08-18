package com.apptimemachine.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptimemachine.data.entities.EventCategory
import com.apptimemachine.data.repository.AppRepository
import com.apptimemachine.data.repository.BatteryRepository
import com.apptimemachine.data.repository.BatteryUsageRepository
import com.apptimemachine.data.repository.NetworkRepository
import com.apptimemachine.data.repository.StorageRepository
import com.apptimemachine.data.repository.TimelineRepository
import com.apptimemachine.data.repository.UsageRepository
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
    val totalUpdatesThisWeek: Int = 0,
    val batteryProxyToday: List<AppShareStat> = emptyList(),
    val deviceBatteryDropToday: Int? = null,
    val networkToday: List<AppNetworkStat> = emptyList(),
    val wifiTotalTodayBytes: Long = 0,
    val mobileTotalTodayBytes: Long = 0
)

data class AppShareStat(val appName: String, val packageName: String, val sharePercent: Float, val foregroundMs: Long)
data class AppNetworkStat(val appName: String, val packageName: String, val totalBytes: Long)

/**
 * Part 2.8 Statistics & Analytics Engine. Every number here is derived
 * from stored history — no chart is rendered from insufficient data
 * without the caller explicitly checking [StatisticsUiState.totalEvents]
 * first (Part 2.8 Failure Handling: "If data is insufficient, display
 * friendly explanation. Do not generate misleading charts.").
 */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val timelineRepository: TimelineRepository,
    private val appRepository: AppRepository,
    private val storageRepository: StorageRepository,
    private val versionRepository: VersionRepository,
    private val batteryUsageRepository: BatteryUsageRepository,
    private val networkRepository: NetworkRepository
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
            val todayEpochDay = today.toEpochDay()

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

            // Battery-drain proxy: today's top apps by foreground-time share.
            val batteryRows = batteryUsageRepository.getTopForDay(todayEpochDay, limit = 8)
            val batteryStats = batteryRows.mapNotNull { row ->
                val app = appRepository.findById(row.appId) ?: return@mapNotNull null
                AppShareStat(app.appName, app.packageName, row.proxySharePercent, row.foregroundMs)
            }
            val deviceDrop = batteryRows.firstOrNull()?.deviceBatteryDropPercent

            // Network: today's top apps by total bytes (wifi + mobile).
            val networkRows = networkRepository.getAllForDay(todayEpochDay)
            val networkStats = networkRows
                .mapNotNull { row ->
                    val app = appRepository.findById(row.appId) ?: return@mapNotNull null
                    val total = (row.wifiRxBytes ?: 0) + (row.wifiTxBytes ?: 0) + (row.mobileRxBytes ?: 0) + (row.mobileTxBytes ?: 0)
                    if (total <= 0) null else AppNetworkStat(app.appName, app.packageName, total)
                }
                .sortedByDescending { it.totalBytes }
                .take(8)
            val wifiTotal = networkRows.sumOf { (it.wifiRxBytes ?: 0) + (it.wifiTxBytes ?: 0) }
            val mobileTotal = networkRows.sumOf { (it.mobileRxBytes ?: 0) + (it.mobileTxBytes ?: 0) }

            _uiState.value = StatisticsUiState(
                isLoading = false,
                dailyEventCounts = dailyCounts,
                dailyEventLabels = dailyLabels,
                calendarHeatmap = heatmap,
                totalEvents = dailyCounts.sum().toInt(),
                mostChangedAppName = weekEvents.groupingBy { it.appName }.eachCount().maxByOrNull { it.value }?.key,
                largestStorageGrowthAppName = largestStorageApp,
                totalUpdatesThisWeek = updatesThisWeek,
                batteryProxyToday = batteryStats,
                deviceBatteryDropToday = deviceDrop,
                networkToday = networkStats,
                wifiTotalTodayBytes = wifiTotal,
                mobileTotalTodayBytes = mobileTotal
            )
        }
    }

}
