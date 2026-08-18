package com.apptimemachine.core.monitoring

import com.apptimemachine.data.model.AppNetworkStat
import com.apptimemachine.data.model.AppShareStat
import com.apptimemachine.data.repository.AppRepository
import com.apptimemachine.data.repository.BatteryUsageRepository
import com.apptimemachine.data.repository.NetworkRepository
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

data class DeviceMonitoringSnapshot(
    val batteryProxyToday: List<AppShareStat> = emptyList(),
    val deviceBatteryDropToday: Int? = null,
    val networkToday: List<AppNetworkStat> = emptyList(),
    val wifiTotalTodayBytes: Long = 0,
    val mobileTotalTodayBytes: Long = 0
)

/**
 * One-shot reads of today's battery-drain-proxy and network-usage data,
 * shared by Dashboard, Timeline (fixed header cards), and App Details
 * (single-app view) so all three surfaces show identical numbers rather
 * than each recomputing it slightly differently.
 */
@Singleton
class MonitoringStatsProvider @Inject constructor(
    private val appRepository: AppRepository,
    private val batteryUsageRepository: BatteryUsageRepository,
    private val networkRepository: NetworkRepository
) {
    private fun todayEpochDay(): Long = LocalDate.now(ZoneOffset.systemDefault()).toEpochDay()

    /** Whole-device snapshot for today — Dashboard and Timeline's fixed cards. */
    suspend fun getDeviceSnapshotToday(): DeviceMonitoringSnapshot {
        val day = todayEpochDay()

        val batteryRows = batteryUsageRepository.getTopForDay(day, limit = 8)
        val batteryStats = batteryRows.mapNotNull { row ->
            val app = appRepository.findById(row.appId) ?: return@mapNotNull null
            AppShareStat(app.appName, app.packageName, row.proxySharePercent, row.foregroundMs)
        }
        val deviceDrop = batteryRows.firstOrNull()?.deviceBatteryDropPercent

        val networkRows = networkRepository.getAllForDay(day)
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

        return DeviceMonitoringSnapshot(
            batteryProxyToday = batteryStats,
            deviceBatteryDropToday = deviceDrop,
            networkToday = networkStats,
            wifiTotalTodayBytes = wifiTotal,
            mobileTotalTodayBytes = mobileTotal
        )
    }

    /** Single-app snapshot for today — App Details' battery/network cards. */
    suspend fun getAppSnapshotToday(appId: Long): DeviceMonitoringSnapshot {
        val day = todayEpochDay()
        val app = appRepository.findById(appId) ?: return DeviceMonitoringSnapshot()

        val batteryRow = batteryUsageRepository.getTopForDay(day, limit = 500).firstOrNull { it.appId == appId }
        val batteryStats = batteryRow?.let {
            listOf(AppShareStat(app.appName, app.packageName, it.proxySharePercent, it.foregroundMs))
        }.orEmpty()

        val networkRow = networkRepository.getAllForDay(day).firstOrNull { it.appId == appId }
        val total = networkRow?.let {
            (it.wifiRxBytes ?: 0) + (it.wifiTxBytes ?: 0) + (it.mobileRxBytes ?: 0) + (it.mobileTxBytes ?: 0)
        } ?: 0
        val networkStats = if (total > 0) listOf(AppNetworkStat(app.appName, app.packageName, total)) else emptyList()

        return DeviceMonitoringSnapshot(
            batteryProxyToday = batteryStats,
            deviceBatteryDropToday = batteryRow?.deviceBatteryDropPercent,
            networkToday = networkStats,
            wifiTotalTodayBytes = (networkRow?.wifiRxBytes ?: 0) + (networkRow?.wifiTxBytes ?: 0),
            mobileTotalTodayBytes = (networkRow?.mobileRxBytes ?: 0) + (networkRow?.mobileTxBytes ?: 0)
        )
    }
}
