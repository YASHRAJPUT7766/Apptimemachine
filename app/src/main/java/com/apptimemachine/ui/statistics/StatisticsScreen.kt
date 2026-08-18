package com.apptimemachine.ui.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.apptimemachine.ui.components.AtmCard
import com.apptimemachine.ui.components.CalendarHeatmap
import com.apptimemachine.ui.components.EmptyState
import com.apptimemachine.ui.components.SimpleBarChart

/**
 * Part 2.8 Statistics & Analytics Engine UI — general activity trends
 * only (weekly/monthly event counts, highlights). Battery and Network
 * cards live in [com.apptimemachine.ui.components.BatteryDrainCard] /
 * [com.apptimemachine.ui.components.NetworkUsageCard] instead, shown on
 * Dashboard, Timeline, and App Details — not here, since those are
 * per-day snapshots rather than trend charts.
 */
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
