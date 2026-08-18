package com.apptimemachine.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.apptimemachine.core.utils.Formatters
import com.apptimemachine.ui.components.AppIcon
import com.apptimemachine.ui.components.AtmCard
import com.apptimemachine.ui.components.CalendarHeatmap
import com.apptimemachine.ui.components.DonutChart
import com.apptimemachine.ui.components.EmptyState
import com.apptimemachine.ui.components.SectionHeader
import com.apptimemachine.ui.components.SimpleBarChart

/** Part 2.8 Statistics & Analytics Engine UI. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Statistics") }) }) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                CircularProgressIndicator(Modifier.align(androidx.compose.ui.Alignment.Center))
            }
            return@Scaffold
        }

        if (state.totalEvents == 0) {
            EmptyState(
                title = "Not enough monitoring data yet",
                description = "Statistics will appear once monitoring has been running for a while.",
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AtmCard {
                    Text("This Week's Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    SimpleBarChart(values = state.dailyEventCounts, labels = state.dailyEventLabels)
                }
            }

            item {
                AtmCard {
                    Text("30-Day Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    CalendarHeatmap(dailyCounts = state.calendarHeatmap, modifier = Modifier.fillMaxWidth())
                }
            }

            item {
                AtmCard {
                    Text("Highlights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    HighlightRow("Most Changed App", state.mostChangedAppName ?: "—")
                    HighlightRow("Fastest Growing (Storage)", state.largestStorageGrowthAppName ?: "—")
                    HighlightRow("Updates This Week", state.totalUpdatesThisWeek.toString())
                }
            }

            item {
                BatteryDrainCard(
                    apps = state.batteryProxyToday,
                    deviceDropPercent = state.deviceBatteryDropToday
                )
            }

            item {
                NetworkUsageCard(
                    apps = state.networkToday,
                    wifiTotalBytes = state.wifiTotalTodayBytes,
                    mobileTotalBytes = state.mobileTotalTodayBytes
                )
            }
        }
    }
}

@Composable
private fun HighlightRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

/**
 * Battery-drain PROXY card — labeled explicitly as an estimate derived
 * from usage time, never presented as measured battery %. See
 * [com.apptimemachine.data.entities.BatteryUsageEntity] doc for why a
 * real per-app battery percentage isn't obtainable on a non-rooted
 * device: the platform API that reports it is restricted to system apps.
 */
@Composable
private fun BatteryDrainCard(apps: List<AppShareStat>, deviceDropPercent: Int?) {
    AtmCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Battery Drain (Estimated)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Text(
            "Estimated from screen-on time per app — Android doesn't expose exact per-app battery % to apps like this one.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))

        if (apps.isEmpty()) {
            Text(
                "No usage recorded yet today",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val palette = listOf(
                    Color(0xFF7C4DFF), Color(0xFF00BFA5), Color(0xFFFF6D00),
                    Color(0xFFD500F9), Color(0xFF2979FF), Color(0xFFFFAB00),
                    Color(0xFF00C853), Color(0xFFFF1744)
                )
                val slices = apps.mapIndexed { i, a -> a.sharePercent to palette[i % palette.size] }
                DonutChart(
                    slices = slices,
                    modifier = Modifier.size(96.dp),
                    strokeWidthDp = 16.dp,
                    centerContent = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                apps.firstOrNull()?.let { "${it.sharePercent.toInt()}%" } ?: "—",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text("top app", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    apps.take(4).forEachIndexed { i, app ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            listOf(
                                                Color(0xFF7C4DFF), Color(0xFF00BFA5), Color(0xFFFF6D00), Color(0xFFD500F9)
                                            )[i % 4],
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(app.appName, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                            Text(
                                "${app.sharePercent.toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (deviceDropPercent != null) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(Modifier.height(10.dp))
                Text(
                    "Phone battery dropped $deviceDropPercent% today (while unplugged)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Today's per-app network usage — real data via NetworkStatsManager, wifi + mobile split. */
@Composable
private fun NetworkUsageCard(apps: List<AppNetworkStat>, wifiTotalBytes: Long, mobileTotalBytes: Long) {
    AtmCard {
        Text("Network Usage Today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Row {
            Text(
                "Wi-Fi: ${Formatters.bytes(wifiTotalBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(16.dp))
            Text(
                "Mobile: ${Formatters.bytes(mobileTotalBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(14.dp))

        if (apps.isEmpty()) {
            Text(
                "No network activity recorded yet today",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val maxBytes = apps.maxOf { it.totalBytes }.coerceAtLeast(1)
            apps.forEach { app ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon(packageName = app.packageName, size = 28.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(app.appName, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        Spacer(Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress = { app.totalBytes.toFloat() / maxBytes.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        Formatters.bytes(app.totalBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
