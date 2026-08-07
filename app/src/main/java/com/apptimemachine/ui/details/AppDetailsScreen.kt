package com.apptimemachine.ui.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.apptimemachine.core.utils.Formatters
import com.apptimemachine.data.entities.PermissionState
import com.apptimemachine.ui.components.AtmCard
import com.apptimemachine.ui.components.EmptyState
import com.apptimemachine.ui.components.SectionHeader
import com.apptimemachine.ui.dashboard.TimelineEventRow

private val tabs = listOf("Overview", "Timeline", "Storage", "Version", "Permissions")

/** Part 2.7 Application Details Screen — collapsing header + scrollable tabs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailsScreen(onBack: () -> Unit, viewModel: AppDetailsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val timeline by viewModel.timelineState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.app?.appName ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            if (state.app?.isFavorite == true) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (state.app == null) {
            Box(Modifier.fillMaxSize().padding(padding)) { CircularProgressIndicator(Modifier.align(androidx.compose.ui.Alignment.Center)) }
            return@Scaffold
        }
        val app = state.app!!

        Column(modifier = Modifier.padding(padding)) {
            AtmCard(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data("package:${app.packageName}").build(),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(app.appName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("v${app.versionName ?: "—"} (${app.versionCode})", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            ScrollableTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                }
            }

            when (selectedTab) {
                0 -> OverviewTab(state)
                1 -> TimelineTab(timeline)
                2 -> StorageTab(state)
                3 -> VersionTab(state)
                4 -> PermissionsTab(state)
            }
        }
    }
}

@Composable
private fun OverviewTab(state: AppDetailsUiState) {
    val app = state.app ?: return
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            AtmCard {
                Text("General Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                InfoRow("Target SDK", app.targetSdk.toString())
                InfoRow("Minimum SDK", app.minSdk.toString())
                InfoRow("Type", if (app.isSystemApp) "System App" else "User App")
                InfoRow("Install Date", Formatters.dateTime(app.installTime))
                InfoRow("Last Update", Formatters.dateTime(app.lastUpdateTime))
                InfoRow("Monitoring Since", Formatters.dateTime(app.monitoringStartTimestamp))
            }
        }
        item {
            AtmCard {
                Text("Storage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                val total = (app.appSizeBytes ?: 0) + (app.dataSizeBytes ?: 0) + (app.cacheSizeBytes ?: 0)
                InfoRow("Total", Formatters.bytes(if (total > 0) total else null))
                InfoRow("App Size", Formatters.bytes(app.appSizeBytes))
                InfoRow("Data", Formatters.bytes(app.dataSizeBytes))
                InfoRow("Cache", Formatters.bytes(app.cacheSizeBytes))
            }
        }
    }
}

@Composable
private fun TimelineTab(events: List<com.apptimemachine.data.entities.TimelineEventEntity>) {
    if (events.isEmpty()) {
        EmptyState(title = "No events yet", description = "Changes to this app will appear here once detected.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(events, key = { it.eventId }) { TimelineEventRow(it) }
    }
}

@Composable
private fun StorageTab(state: AppDetailsUiState) {
    if (state.storageHistory.isEmpty()) {
        EmptyState(title = "No storage history yet", description = "Storage changes will be recorded as they're detected.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(state.storageHistory) { entry ->
            AtmCard {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(Formatters.dateTime(entry.recordedAt), style = MaterialTheme.typography.bodyMedium)
                    Text(Formatters.signedBytes(entry.differenceBytes), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
                Text(Formatters.bytes(entry.totalSizeBytes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun VersionTab(state: AppDetailsUiState) {
    if (state.versionHistory.isEmpty()) {
        EmptyState(title = "No version history yet", description = "Version changes will be recorded as they're detected.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(state.versionHistory) { entry ->
            AtmCard {
                Text("${entry.oldVersionName ?: "—"} → ${entry.newVersionName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(Formatters.dateTime(entry.changedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PermissionsTab(state: AppDetailsUiState) {
    if (state.permissionHistory.isEmpty()) {
        EmptyState(title = "No permission changes yet", description = "Permission changes will be recorded as they're detected.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(state.permissionHistory) { entry ->
            AtmCard {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(entry.permissionName.substringAfterLast('.'), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (entry.currentState == PermissionState.GRANTED) "Granted" else "Revoked",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(Formatters.dateTime(entry.changedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
