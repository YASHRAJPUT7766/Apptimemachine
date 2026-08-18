package com.apptimemachine.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.apptimemachine.core.utils.AppLauncher
import com.apptimemachine.core.utils.Formatters
import com.apptimemachine.data.entities.PermissionState
import com.apptimemachine.data.entities.StorageHistoryEntity
import com.apptimemachine.ui.components.AppIcon
import com.apptimemachine.ui.components.AtmCard
import com.apptimemachine.ui.components.DonutChart
import com.apptimemachine.ui.components.RingProgress
import com.apptimemachine.ui.components.SimpleBarChart
import com.apptimemachine.ui.components.SimpleLineChart
import com.apptimemachine.ui.timeline.TimelineEventRow

private val tabs = listOf("Overview", "Timeline", "Storage", "Version", "Permissions")

// Fixed slice colors for the storage donut so App/Data/Cache always mean
// the same color across Overview and Storage tabs, drawn from the app's
// own primary/tertiary family rather than arbitrary chart colors.
private val ColorAppSize = Color(0xFF4A5FE8)   // primary (indigo)
private val ColorDataSize = Color(0xFF7D5296)  // tertiary (purple)
private val ColorCacheSize = Color(0xFFE8A24A) // warm amber, distinct from both
private val ColorVersion = Color(0xFF2E7D96)   // teal, for the Version tab's dial

/**
 * Part 2.7 Application Details Screen — collapsing header + scrollable
 * tabs. Design rule for this screen: every visual (donut, ring, line, bar)
 * is ALWAYS on screen, in every tab, whether or not data exists yet — a
 * zero-value chart communicates "tracking, nothing to show yet" much
 * better than swapping the whole card for a text-only empty state. Only
 * list-shaped content (history rows) collapses to a text empty-state,
 * since there's no meaningful "zero" shape for a list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailsScreen(onBack: () -> Unit, viewModel: AppDetailsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val timeline by viewModel.timelineState.collectAsState()
    val monitoringSnapshot by viewModel.monitoringSnapshot.collectAsState()
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
                            contentDescription = "Favorite",
                            tint = if (state.app?.isFavorite == true) Color(0xFFFFC107) else LocalContentColor.current
                        )
                    }
                    val packageName = state.app?.packageName
                    IconButton(
                        onClick = { packageName?.let { AppLauncher.open(context, it) } },
                        enabled = packageName != null
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open app")
                    }
                }
            )
        }
    ) { padding ->
        if (state.app == null) {
            Box(Modifier.fillMaxSize().padding(padding)) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
            return@Scaffold
        }
        val app = state.app!!

        Column(modifier = Modifier.padding(padding)) {
            AtmCard(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(packageName = app.packageName, size = 56.dp, cornerRadius = 16.dp)
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
                0 -> OverviewTab(state, monitoringSnapshot)
                1 -> TimelineTab(timeline)
                2 -> StorageTab(state)
                3 -> VersionTab(state)
                4 -> PermissionsTab(state)
            }
        }
    }
}

@Composable
private fun OverviewTab(state: AppDetailsUiState, monitoringSnapshot: com.apptimemachine.core.monitoring.DeviceMonitoringSnapshot) {
    val app = state.app ?: return
    val latestStorage = state.storageHistory.maxByOrNull { it.recordedAt }

    val appSize = (latestStorage?.appSizeBytes ?: 0L).coerceAtLeast(0L)
    val dataSize = (latestStorage?.dataSizeBytes ?: 0L).coerceAtLeast(0L)
    val cacheSize = (latestStorage?.cacheSizeBytes ?: 0L).coerceAtLeast(0L)
    val total = appSize + dataSize + cacheSize

    val todayMs = state.todayUsageMs ?: 0L
    val avgMs = state.averageDailyUsageMs
    val usageReference = (avgMs ?: 1L).coerceAtLeast(1L)
    val usageProgress = if (avgMs != null) todayMs.toFloat() / usageReference.toFloat() else 0f

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
                Spacer(Modifier.height(16.dp))

                if (!state.hasUsageAccess) {
                    UsageAccessNotice()
                    Spacer(Modifier.height(16.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    DonutChart(
                        slices = listOf(
                            appSize.toFloat() to ColorAppSize,
                            dataSize.toFloat() to ColorDataSize,
                            cacheSize.toFloat() to ColorCacheSize
                        ),
                        modifier = Modifier.size(120.dp),
                        centerContent = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    Formatters.bytes(total),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    )
                    Spacer(Modifier.width(20.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LegendRow("App", ColorAppSize, Formatters.bytes(latestStorage?.appSizeBytes))
                        LegendRow("Data", ColorDataSize, Formatters.bytes(latestStorage?.dataSizeBytes))
                        LegendRow("Cache", ColorCacheSize, Formatters.bytes(latestStorage?.cacheSizeBytes))
                    }
                }

                Spacer(Modifier.height(12.dp))
                val todayDelta = latestStorage?.differenceBytes
                Text(
                    when {
                        latestStorage == null -> "Waiting for the first scan to record a baseline."
                        todayDelta == null || todayDelta == 0L -> "No change since last scan."
                        else -> "${if (todayDelta > 0) "Grew" else "Shrank"} by ${Formatters.bytes(kotlin.math.abs(todayDelta))} since last scan"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        todayDelta != null && todayDelta > 0 -> Color(0xFFED6C02)
                        todayDelta != null && todayDelta < 0 -> Color(0xFF2E7D32)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        item {
            AtmCard {
                Text("Today's Usage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))

                if (!state.hasUsageAccess) {
                    UsageAccessNotice()
                    Spacer(Modifier.height(16.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RingProgress(
                        progress = usageProgress,
                        modifier = Modifier.size(96.dp),
                        progressColor = if (usageProgress > 1f) Color(0xFFED6C02) else MaterialTheme.colorScheme.primary,
                        centerContent = {
                            Text(
                                Formatters.duration(todayMs),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    )
                    Spacer(Modifier.width(20.dp))
                    Column {
                        Text("Today", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            when {
                                !state.hasUsageAccess -> "Grant Usage Access to track this"
                                avgMs != null -> "vs ${Formatters.duration(avgMs)} daily average"
                                state.todayUsageMs == null -> "No usage recorded yet today"
                                else -> "No average yet — check back tomorrow"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (monitoringSnapshot.batteryProxyToday.isNotEmpty()) {
            item {
                com.apptimemachine.ui.components.BatteryDrainCard(
                    apps = monitoringSnapshot.batteryProxyToday,
                    deviceDropPercent = monitoringSnapshot.deviceBatteryDropToday
                )
            }
        }

        if (monitoringSnapshot.networkToday.isNotEmpty()) {
            item {
                com.apptimemachine.ui.components.NetworkUsageCard(
                    apps = monitoringSnapshot.networkToday,
                    wifiTotalBytes = monitoringSnapshot.wifiTotalTodayBytes,
                    mobileTotalBytes = monitoringSnapshot.mobileTotalTodayBytes
                )
            }
        }
    }
}

@Composable
private fun TimelineTab(events: List<com.apptimemachine.data.entities.TimelineEventEntity>) {
    if (events.isEmpty()) {
        EmptyStateWithIcon(title = "No history", description = "Nothing has changed for this app yet. Events will appear here as soon as they're detected.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(events, key = { it.eventId }) { TimelineEventRow(it) }
    }
}

@Composable
private fun StorageTab(state: AppDetailsUiState) {
    val sorted = state.storageHistory.sortedBy { it.recordedAt }
    val dailySeries = groupToDailySeries(sorted)
    val latest = sorted.lastOrNull()

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!state.hasUsageAccess) {
            item { UsageAccessNotice(inCard = true) }
        }

        item {
            AtmCard {
                Text("Total size trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (dailySeries.isEmpty()) "No data yet — trend will build up as scans run"
                    else "Last ${dailySeries.size} day${if (dailySeries.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                SimpleLineChart(
                    values = dailySeries.map { (it.second.totalSizeBytes ?: 0L) / 1_048_576f },
                    lineColor = ColorAppSize,
                    fillColor = ColorAppSize.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("MB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            AtmCard {
                Text("Daily change", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "How much this app grew or shrank each day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                val deltas = dailySeries.map { (it.second.differenceBytes ?: 0L) / 1_048_576f }
                val labels = dailySeries.map { Formatters.shortDayLabel(it.second.recordedAt) }
                SimpleBarChart(
                    values = deltas.map { kotlin.math.abs(it) },
                    labels = labels,
                    barColor = ColorDataSize,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("MB change (magnitude)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            AtmCard {
                Text("Current breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                InfoRow("App Size", Formatters.bytes(latest?.appSizeBytes))
                InfoRow("Data", Formatters.bytes(latest?.dataSizeBytes))
                InfoRow("Cache", Formatters.bytes(latest?.cacheSizeBytes))
                InfoRow("Total", Formatters.bytes(latest?.totalSizeBytes))
                InfoRow("Last recorded", latest?.let { Formatters.dateTime(it.recordedAt) } ?: "Not yet scanned")
            }
        }

        item {
            Text("History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        }
        if (sorted.isEmpty()) {
            item {
                EmptyStateWithIcon(title = "No storage history yet", description = "Storage is recorded automatically on every scan — check back after the next one.")
            }
        } else {
            items(sorted.sortedByDescending { it.recordedAt }, key = { it.storageHistoryId }) { entry ->
                AtmCard {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(Formatters.dateTime(entry.recordedAt), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            Formatters.signedBytes(entry.differenceBytes),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = when {
                                (entry.differenceBytes ?: 0) > 0 -> Color(0xFFED6C02)
                                (entry.differenceBytes ?: 0) < 0 -> Color(0xFF2E7D32)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    Text(Formatters.bytes(entry.totalSizeBytes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/**
 * Collapses raw storage-history rows (one per scan, which can be several
 * per day) down to one point per calendar day — the last reading of that
 * day — so the trend/daily-change charts show a clean daily series instead
 * of a jagged multi-scan-per-day line.
 */
private fun groupToDailySeries(sorted: List<StorageHistoryEntity>): List<Pair<Long, StorageHistoryEntity>> {
    if (sorted.isEmpty()) return emptyList()
    return sorted
        .groupBy { Formatters.dayKey(it.recordedAt) }
        .toSortedMap()
        .map { (dayKey, entries) -> dayKey to entries.maxByOrNull { it.recordedAt }!! }
}

@Composable
private fun VersionTab(state: AppDetailsUiState) {
    val updateCount = state.versionHistory.size

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            AtmCard {
                Text("Current Version", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Ring here reads as an "update activity" dial: full
                    // circle at a cap of 10 tracked updates, mostly a
                    // stylistic echo of the Overview rings rather than a
                    // literal percentage — this tab has no natural 0..1
                    // quantity, so the ring shows update *count* momentum.
                    RingProgress(
                        progress = (updateCount / 10f).coerceIn(0f, 1f),
                        modifier = Modifier.size(88.dp),
                        progressColor = ColorVersion,
                        centerContent = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(updateCount.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("updates", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    )
                    Spacer(Modifier.width(20.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        InfoRow("Version Name", state.app?.versionName ?: "—")
                        InfoRow("Version Code", state.app?.versionCode?.toString() ?: "—")
                    }
                }
            }
        }
        item {
            Text("History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        }
        if (state.versionHistory.isEmpty()) {
            item {
                EmptyStateWithIcon(title = "No version history yet", description = "Version changes will be recorded as they're detected.")
            }
        } else {
            items(state.versionHistory.sortedByDescending { it.changedAt }, key = { it.versionHistoryId }) { entry ->
                AtmCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .background(ColorVersion, androidx.compose.foundation.shape.CircleShape)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("${entry.oldVersionName ?: "—"} → ${entry.newVersionName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(Formatters.dateTime(entry.changedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PermissionsTab(state: AppDetailsUiState) {
    val grantedCount = state.grantedPermissions.size

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            AtmCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Same "count dial" treatment as Version — caps
                    // visually at 20 granted permissions, just gives the
                    // number a shape rather than implying a target.
                    RingProgress(
                        progress = (grantedCount / 20f).coerceIn(0f, 1f),
                        modifier = Modifier.size(64.dp),
                        strokeWidthDp = 8.dp,
                        progressColor = MaterialTheme.colorScheme.primary,
                        centerContent = {
                            Text(grantedCount.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Currently Granted", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            "$grantedCount permission${if (grantedCount == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (state.grantedPermissions.isEmpty()) {
                    Text(
                        "No permissions currently granted.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.grantedPermissions.forEach { permission ->
                            AssistChip(
                                onClick = {},
                                label = { Text(permission.substringAfterLast('.'), style = MaterialTheme.typography.labelMedium) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }
        }

        item {
            Text("History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        }
        if (state.permissionHistory.isEmpty()) {
            item {
                EmptyStateWithIcon(title = "No permission changes yet", description = "Permission changes will be recorded as they're detected.")
            }
        } else {
            items(state.permissionHistory.sortedByDescending { it.changedAt }, key = { it.permissionHistoryId }) { entry ->
                val granted = entry.currentState == PermissionState.GRANTED
                AtmCard {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.permissionName.substringAfterLast('.'), style = MaterialTheme.typography.bodyMedium)
                        AssistChip(
                            onClick = {},
                            label = { Text(if (granted) "Granted" else "Revoked", style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (granted) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFFD32F2F).copy(alpha = 0.15f),
                                labelColor = if (granted) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                            )
                        )
                    }
                    Text(Formatters.dateTime(entry.changedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun UsageAccessNotice(inCard: Boolean = false) {
    val content: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Usage Access needed", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    "Grant Usage Access in Settings to see real storage and usage numbers here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    if (inCard) AtmCard { content() } else content()
}

/** Small icon + title + description block for genuinely list-shaped empty states (history rows with no natural "zero" visual). */
@Composable
private fun EmptyStateWithIcon(title: String, description: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun LegendRow(label: String, color: Color, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .background(color, androidx.compose.foundation.shape.CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
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
