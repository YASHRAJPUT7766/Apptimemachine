package com.apptimemachine.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.apptimemachine.core.utils.Formatters
import com.apptimemachine.data.entities.EventSeverity
import com.apptimemachine.data.entities.TimelineEventEntity
import com.apptimemachine.ui.components.AtmCard
import com.apptimemachine.ui.components.EmptyState
import com.apptimemachine.ui.components.SectionHeader
import com.apptimemachine.ui.components.ShimmerCard
import java.time.LocalTime

/**
 * Part 1.2 Home Dashboard. Renders straight off [DashboardUiState] — all
 * business logic (counts, growth calc, "today" boundaries) lives in the
 * ViewModel; this file is pure layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenTimeline: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenStatistics: () -> Unit = {},
    onOpenCompare: () -> Unit = {},
    onOpenBackup: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(greeting(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Monitoring ${state.totalApps} Applications",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSearch) { Icon(Icons.Default.Search, contentDescription = "Search") }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.runManualScan() },
                icon = {
                    if (state.isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                },
                text = { Text(if (state.isScanning) "Scanning…" else "Scan Now") }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(4) { ShimmerCard(Modifier.fillMaxWidth().height(100.dp)) }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { MonitoringStatusCard(state) }
            item { TodaysSummaryCard(state) }
            item { MonitoringOverviewCard(state, onOpenApps) }
            item { QuickActionsRow(onOpenStatistics, onOpenCompare, onOpenBackup) }

            item {
                SectionHeader("Recent Activity", action = {
                    TextButton(onClick = onOpenTimeline) { Text("See all") }
                })
            }

            if (state.recentEvents.isEmpty()) {
                item {
                    EmptyState(
                        title = "Monitoring has started",
                        description = "Timeline events will appear automatically when supported changes are detected.",
                        icon = Icons.Default.History
                    )
                }
            } else {
                items(state.recentEvents, key = { it.eventId }) { event ->
                    TimelineEventRow(event)
                }
            }

            item { Spacer(Modifier.height(64.dp)) } // room for FAB
        }
    }
}

@Composable
private fun QuickActionsRow(onOpenStatistics: () -> Unit, onOpenCompare: () -> Unit, onOpenBackup: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        QuickActionButton(Icons.Default.BarChart, "Statistics", onOpenStatistics, Modifier.weight(1f))
        QuickActionButton(Icons.Default.CompareArrows, "Compare", onOpenCompare, Modifier.weight(1f))
        QuickActionButton(Icons.Default.Backup, "Backup", onOpenBackup, Modifier.weight(1f))
    }
}

@Composable
private fun QuickActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AtmCard(onClick = onClick, modifier = modifier) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

private fun greeting(): String {
    val hour = LocalTime.now().hour
    return when {
        hour < 12 -> "Good Morning 👋"
        hour < 17 -> "Good Afternoon 👋"
        else -> "Good Evening 👋"
    }
}

@Composable
private fun MonitoringStatusCard(state: DashboardUiState) {
    AtmCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Circle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.width(8.dp))
            Text("Monitoring Running", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LabelValue("Last Scan", state.lastScan?.finishTime?.let { Formatters.relativeTime(it) } ?: "—")
            LabelValue("Timeline Events", state.totalTimelineEvents.toString())
            LabelValue("Today's Events", state.eventsToday.toString())
        }
    }
}

@Composable
private fun TodaysSummaryCard(state: DashboardUiState) {
    AtmCard {
        Text("Today's Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LabelValue("Updated Apps", state.updatesToday.toString())
            LabelValue("Storage Growth", Formatters.signedBytes(state.storageGrowthToday))
            LabelValue("Permission Changes", (state.permissionsGrantedToday + state.permissionsRevokedToday).toString())
            LabelValue("Notifications", state.notificationsToday.toString())
        }
    }
}

@Composable
private fun MonitoringOverviewCard(state: DashboardUiState, onOpenApps: () -> Unit) {
    AtmCard(onClick = onOpenApps) {
        Text("Installed Apps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LabelValue("Total", state.totalApps.toString())
            LabelValue("System", state.systemApps.toString())
            LabelValue("User", state.userApps.toString())
        }
    }
}

@Composable
private fun LabelValue(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TimelineEventRow(event: TimelineEventEntity, modifier: Modifier = Modifier) {
    AtmCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SeverityDot(event.severity)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(
                    event.eventType.replace('_', ' ').lowercase()
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                Formatters.relativeTime(event.createdTimestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SeverityDot(severity: EventSeverity) {
    val color = when (severity) {
        EventSeverity.INFO -> MaterialTheme.colorScheme.tertiary
        EventSeverity.SUCCESS -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
        EventSeverity.WARNING -> androidx.compose.ui.graphics.Color(0xFFED6C02)
        EventSeverity.IMPORTANT -> MaterialTheme.colorScheme.primary
        EventSeverity.CRITICAL -> androidx.compose.ui.graphics.Color(0xFFD32F2F)
    }
    Box(modifier = Modifier.size(10.dp)) {
        Icon(Icons.Default.Circle, contentDescription = null, tint = color, modifier = Modifier.fillMaxSize())
    }
}
