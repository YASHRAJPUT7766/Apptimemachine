package com.apptimemachine.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.apptimemachine.core.utils.Formatters
import com.apptimemachine.ui.components.AtmCard
import com.apptimemachine.ui.components.ShimmerCard
import com.apptimemachine.ui.timeline.TimelineEventRow
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

    Scaffold { padding ->
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
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { DashboardHeader(state, onOpenSearch, onOpenSettings) }
            item { Box(Modifier.padding(horizontal = 20.dp)) { MonitoringStatusCard(state) } }
            item { Box(Modifier.padding(horizontal = 20.dp)) { TodaysSummaryCard(state) } }
            item { Box(Modifier.padding(horizontal = 20.dp)) { MonitoringOverviewCard(state, onOpenApps) } }
            item { Box(Modifier.padding(horizontal = 20.dp)) { QuickActionsGrid(state, viewModel, onOpenStatistics, onOpenCompare, onOpenBackup) } }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Recent Activity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onOpenTimeline) { Text("See all") }
                }
            }

            if (state.recentEvents.isEmpty()) {
                item {
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        AtmCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Monitoring started", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "System monitoring is active",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "Just now",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                items(state.recentEvents, key = { it.eventId }) { event ->
                    Box(Modifier.padding(horizontal = 20.dp)) { TimelineEventRow(event) }
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(state: DashboardUiState, onOpenSearch: () -> Unit, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(greeting(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Row {
                Text(
                    "Monitoring ",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${state.totalApps}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    " Applications",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeaderIconButton(Icons.Default.Search, "Search", onOpenSearch)
            HeaderIconButton(Icons.Default.Settings, "Settings", onOpenSettings)
        }
    }
}

@Composable
private fun HeaderIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = description)
        }
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

/** Gradient hero card — monitoring status, pulse icon, and the 3-stat strip. */
@Composable
private fun MonitoringStatusCard(state: DashboardUiState) {
    val gradient = Brush.linearGradient(
        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(gradient)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MonitorHeart,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CD964))
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Monitoring Running",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Your system is being monitored in real-time",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.16f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CD964))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Active", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .padding(vertical = 14.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                HeroStat(
                    Icons.Default.Schedule,
                    state.lastScan?.finishTime?.let { Formatters.relativeTime(it) } ?: "—",
                    "Last Scan"
                )
                HeroStat(Icons.Filled.TrendingUp, state.totalTimelineEvents.toString(), "Timeline Events")
                HeroStat(Icons.Default.Notifications, state.eventsToday.toString(), "Today's Events")
            }
        }
    }
}

@Composable
private fun HeroStat(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
    }
}

/** Four colored tiles: updated apps, storage growth, permission changes, notifications. */
@Composable
private fun TodaysSummaryCard(state: DashboardUiState) {
    Column {
        Text("Today's Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryTile(
                Icons.Default.GridView,
                state.updatesToday.toString(),
                "Updated Apps",
                Color(0xFF2E7D32),
                Color(0xFFE3F3E5),
                Modifier.weight(1f)
            )
            SummaryTile(
                Icons.Default.Inbox,
                Formatters.signedBytes(state.storageGrowthToday),
                "Storage Growth",
                Color(0xFF1565C0),
                Color(0xFFE4EEFB),
                Modifier.weight(1f)
            )
            SummaryTile(
                Icons.Default.VerifiedUser,
                (state.permissionsGrantedToday + state.permissionsRevokedToday).toString(),
                "Permission Changes",
                Color(0xFFE65100),
                Color(0xFFFCEADB),
                Modifier.weight(1f)
            )
            SummaryTile(
                Icons.Default.Notifications,
                state.notificationsToday.toString(),
                "Notifications",
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryTile(
    icon: ImageVector,
    value: String,
    label: String,
    iconColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .padding(vertical = 16.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/** Installed Apps card — Total / System / User with icon chips and dividers, "View all" header. */
@Composable
private fun MonitoringOverviewCard(state: DashboardUiState, onOpenApps: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Installed Apps", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = onOpenApps) {
                Text("View all")
                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        AtmCard(onClick = onOpenApps) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                OverviewStat(Icons.Default.GridView, state.totalApps.toString(), "Total", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                VerticalDivider()
                OverviewStat(Icons.Default.Settings, state.systemApps.toString(), "System", Color(0xFF1565C0), Color(0xFFE4EEFB))
                VerticalDivider()
                OverviewStat(Icons.Default.Person, state.userApps.toString(), "User", Color(0xFF2E7D32), Color(0xFFE3F3E5))
            }
        }
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .height(48.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    )
}

@Composable
private fun OverviewStat(icon: ImageVector, value: String, label: String, iconColor: Color, bgColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** 2x2 quick-action grid: Statistics, Compare, Backup, and highlighted Scan Now. */
@Composable
private fun QuickActionsGrid(
    state: DashboardUiState,
    viewModel: DashboardViewModel,
    onOpenStatistics: () -> Unit,
    onOpenCompare: () -> Unit,
    onOpenBackup: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            QuickActionTile(
                icon = Icons.Default.BarChart,
                title = "Statistics",
                subtitle = "View insights",
                onClick = onOpenStatistics,
                modifier = Modifier.weight(1f)
            )
            QuickActionTile(
                icon = Icons.Filled.CompareArrows,
                title = "Compare",
                subtitle = "Compare changes",
                onClick = onOpenCompare,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            QuickActionTile(
                icon = Icons.Default.CloudUpload,
                title = "Backup",
                subtitle = "Backup apps",
                onClick = onOpenBackup,
                modifier = Modifier.weight(1f)
            )
            QuickActionTile(
                icon = if (state.isScanning) null else Icons.Default.Refresh,
                title = "Scan Now",
                subtitle = if (state.isScanning) "Scanning…" else "Start new scan",
                onClick = { viewModel.runManualScan() },
                modifier = Modifier.weight(1f),
                highlighted = true,
                isLoading = state.isScanning
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector?,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    isLoading: Boolean = false
) {
    val bgColor = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer
    val contentColor = if (highlighted) Color.White else MaterialTheme.colorScheme.onSurface
    val subColor = if (highlighted) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
    val chipBg = if (highlighted) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    val chipTint = if (highlighted) Color.White else MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(chipBg),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = chipTint)
                } else if (icon != null) {
                    Icon(icon, contentDescription = null, tint = chipTint, modifier = Modifier.size(19.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = contentColor)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = subColor)
            }
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(chipBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = chipTint, modifier = Modifier.size(16.dp))
            }
        }
    }
}


