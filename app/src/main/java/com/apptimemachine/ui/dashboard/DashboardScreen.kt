package com.apptimemachine.ui.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.apptimemachine.core.utils.Formatters
import com.apptimemachine.ui.components.AtmCard
import com.apptimemachine.ui.components.ShimmerCard
import com.apptimemachine.ui.components.categoryIcons
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
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val deviceSnapshot by viewModel.deviceSnapshot.collectAsState()
    val insights by viewModel.insights.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.padding(padding)
        ) {
            if (state.isLoading) {
                LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(4) { ShimmerCard(Modifier.fillMaxWidth().height(100.dp)) }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item { DashboardHeader(state, onOpenSearch, onOpenSettings) }
                    item { Box(Modifier.padding(horizontal = 20.dp)) { MonitoringStatusCard(state) } }
                    item { Box(Modifier.padding(horizontal = 20.dp)) { TodaysSummaryCard(state) } }
                    item { Box(Modifier.padding(horizontal = 20.dp)) { MonitoringOverviewCard(state, onOpenApps) } }
                    item { Box(Modifier.padding(horizontal = 20.dp)) { QuickActionsGrid(onOpenStatistics, onOpenCompare, onOpenBackup) } }

                    if (insights.isNotEmpty()) {
                        item { Box(Modifier.padding(horizontal = 20.dp)) { InsightsCard(insights) } }
                    }

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
                                Text("Activity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = onOpenTimeline) { Text("See all") }
                        }
                    }

                    // System / Notifications tabs — previously "Recent Activity"
                    // and "Recent Notifications" were two separate always-visible
                    // sections and the same notification event could effectively
                    // show up in both. Splitting into tabs under one "Activity"
                    // header keeps them cleanly separate: exactly one place shows
                    // at a time, each with its own distinct visual style below.
                    item {
                        var selectedTab by rememberSaveable { mutableStateOf(0) }
                        Column {
                            Box(Modifier.padding(horizontal = 20.dp)) {
                                ActivityTabSelector(
                                    selectedTab = selectedTab,
                                    onSelect = { selectedTab = it }
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            if (selectedTab == 0) {
                                if (state.recentEvents.isEmpty()) {
                                    Box(Modifier.padding(horizontal = 20.dp)) {
                                        EmptyActivityCard()
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                        state.recentEvents.forEach { event ->
                                            Box(Modifier.padding(horizontal = 20.dp)) { TimelineEventRow(event) }
                                        }
                                    }
                                }
                            } else {
                                Box(Modifier.padding(horizontal = 20.dp)) {
                                    RecentNotificationsCard(
                                        rows = state.recentNotifications,
                                        onOpenApp = { pkg -> com.apptimemachine.core.utils.AppLauncher.open(context, pkg) },
                                        onDelete = { id -> viewModel.deleteNotification(id) }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Box(Modifier.padding(horizontal = 20.dp)) {
                            com.apptimemachine.ui.components.BatteryDrainCard(
                                apps = deviceSnapshot.batteryProxyToday,
                                deviceDropPercent = deviceSnapshot.deviceBatteryDropToday
                            )
                        }
                    }
                    // Network Usage card intentionally removed from Dashboard —
                    // still available per-app in App Details.
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

/**
 * Glassmorphic neon hero card — dark navy glass, animated cyan pulse ring
 * around the monitor icon, soft glow border, and a pill toggle on the
 * right instead of the old solid green banner. A separate glass strip of
 * 3 stat tiles (Last Update / Timeline Events / Today's Events) sits below,
 * each with its own colored glow (cyan / purple / amber) like distinct
 * glass panels rather than one flat block.
 */
@Composable
private fun MonitoringStatusCard(state: DashboardUiState) {
    val infinite = rememberInfiniteTransition(label = "monitor_pulse")

    // Breathing halo behind the monitor icon — expands/fades on loop.
    val pulseScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )
    // Slow shimmer on the outer border glow, independent of the icon pulse.
    val borderGlow by infinite.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_glow"
    )

    // Theme-aware glass: dark mode keeps the deep-navy neon look; light
    // mode switches to a frosted white/blue-tinted glass instead of
    // reusing the same near-black gradient. Deliberately NOT using
    // isSystemInDarkTheme() here — that reflects the phone's system
    // setting, not the app's own Settings > Theme choice (System/Light/
    // Dark), so if someone picks "Light" inside the app while their
    // phone-wide dark mode is on, isSystemInDarkTheme() would still say
    // true and this card would render dark even though everything else
    // on screen is light. The actually-resolved MaterialTheme background
    // luminance always matches what the rest of the screen is doing.
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val glassBg = if (isDark) {
        Brush.linearGradient(colors = listOf(Color(0xFF0E1B2E), Color(0xFF13233A)))
    } else {
        Brush.linearGradient(colors = listOf(Color(0xFFEAF3FF), Color(0xFFDCEBFF)))
    }
    val cyan = Color(0xFF0F8FCB)
    val onGlassPrimary = if (isDark) Color.White else Color(0xFF0B2540)
    val onGlassSecondary = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF3A5975)

    // Everything (hero card + stat strip) lives inside ONE outer Column so
    // this whole function is a single root composable — the caller wraps
    // it in a Box, and a Box stacks/overlaps multiple root children, which
    // was causing the hero card and stat row to render on top of each other.
    Column(modifier = Modifier.fillMaxWidth()) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(glassBg)
            .border(1.dp, cyan.copy(alpha = if (isDark) borderGlow else borderGlow * 0.6f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer breathing ring
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                        .clip(CircleShape)
                        .background(cyan.copy(alpha = pulseAlpha))
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(cyan.copy(alpha = if (isDark) 0.14f else 0.16f))
                        .border(1.5.dp, cyan.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MonitorHeart,
                        contentDescription = null,
                        tint = if (isDark) Color.White else cyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Text(
                        "Monitoring ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onGlassPrimary
                    )
                    Text(
                        "Active",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cyan
                    )
                }
                Text(
                    "Your system is being monitored in real-time",
                    style = MaterialTheme.typography.bodySmall,
                    color = onGlassSecondary
                )
            }
            // Pill toggle — green dot + "Active" label, glass background.
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isDark) Color(0xFF1A3A2E).copy(alpha = 0.8f)
                        else Color(0xFFDFF5E6)
                    )
                    .border(1.dp, Color(0xFF2E9E5B).copy(alpha = 0.45f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E9E5B))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Active",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isDark) Color.White else Color(0xFF1B5E3A),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    // Separate glass strip below the hero — 3 tiles, each its own subtle
    // color halo (cyan / purple / amber), matching a segmented glass panel
    // rather than the old single translucent block.
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        GlassStatTile(
            icon = Icons.Default.Schedule,
            value = state.lastScan?.finishTime?.let { Formatters.relativeTime(it) } ?: "—",
            label = "Last Update",
            glowColor = Color(0xFF0F8FCB),
            isDark = isDark,
            modifier = Modifier.weight(1f)
        )
        GlassStatTile(
            icon = Icons.Filled.TrendingUp,
            value = state.totalTimelineEvents.toString(),
            label = "Timeline Events",
            glowColor = Color(0xFF9B4FE0),
            isDark = isDark,
            modifier = Modifier.weight(1f)
        )
        GlassStatTile(
            icon = Icons.Default.Notifications,
            value = state.eventsToday.toString(),
            label = "Today's Events",
            glowColor = Color(0xFFD98D1E),
            isDark = isDark,
            modifier = Modifier.weight(1f)
        )
    }
    } // end outer Column
}

@Composable
private fun GlassStatTile(
    icon: ImageVector,
    value: String,
    label: String,
    glowColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "glass_tile_glow")
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(animation = tween(1700, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "glass_tile_glow_alpha"
    )
    val glassBg = if (isDark) {
        Brush.linearGradient(colors = listOf(Color(0xFF11213A), Color(0xFF0C1830)))
    } else {
        Brush.linearGradient(colors = listOf(Color(0xFFF3F8FF), Color(0xFFE7F0FF)))
    }
    val textPrimary = if (isDark) Color.White else Color(0xFF0B2540)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF4A6480)
    val iconTint = if (isDark) Color.White else glowColor

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(glassBg)
            .border(1.dp, glowColor.copy(alpha = if (isDark) 0.28f else 0.35f), RoundedCornerShape(20.dp))
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(glowColor.copy(alpha = glowAlpha))
            )
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(glowColor.copy(alpha = 0.18f))
                    .border(1.dp, glowColor.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textPrimary, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 1
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(2.5.dp)
                .clip(RoundedCornerShape(50))
                .background(glowColor.copy(alpha = 0.7f))
        )
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
                Modifier.weight(1f)
            )
            SummaryTile(
                Icons.Default.PhoneAndroid,
                Formatters.signedBytes(state.storageGrowthToday),
                "Storage Growth",
                Color(0xFF1565C0),
                Modifier.weight(1f)
            )
            SummaryTile(
                Icons.Default.GppGood,
                (state.permissionsGrantedToday + state.permissionsRevokedToday).toString(),
                "Permission Changes",
                Color(0xFFE65100),
                Modifier.weight(1f)
            )
            SummaryTile(
                Icons.Default.Notifications,
                state.notificationsToday.toString(),
                "Notifications",
                Color(0xFF6A1B9A),
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
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    // Entrance pop: tile scales up from 0 with a slight overshoot the first
    // time it appears, instead of the old flat block just being static.
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val entranceScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.6f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "tile_entrance"
    )

    // Slow breathing glow behind the icon — replaces the old solid pastel
    // block with a subtle animated halo instead, on the app's normal
    // adaptive card surface rather than a fixed light-only color.
    val infiniteTransition = rememberInfiniteTransition(label = "tile_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(animation = tween(1600, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "tile_glow_alpha"
    )

    Column(
        modifier = modifier
            .graphicsLayer { scaleX = entranceScale; scaleY = entranceScale }
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, accentColor.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
            .padding(vertical = 16.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = glowAlpha))
            )
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/** Installed Apps card — donut ring + System/User/Disabled breakdown rows + Top Categories panel, "View all" header. */
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
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                InstalledAppsDonut(state)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BreakdownRow(Icons.Default.Settings, "System Apps", state.systemApps, state.totalApps, Color(0xFF1565C0))
                    BreakdownRow(Icons.Default.Person, "User Apps", state.userApps, state.totalApps, Color(0xFF2E7D32))
                    BreakdownRow(Icons.Default.VisibilityOff, "Disabled Apps", state.disabledApps, state.totalApps, Color(0xFF757575))
                }
            }
            if (state.topCategories.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                TopCategoriesPanel(state.topCategories)
            }
        }
    }
}

@Composable
private fun InstalledAppsDonut(state: DashboardUiState) {
    // Matches the colors used in the BreakdownRow list below it
    // (blue/green/grey) instead of three shades of the same green —
    // each segment should read as its own distinct color, not a monotone ring.
    val slices = listOf(
        state.systemApps.toFloat() to Color(0xFF1565C0),
        state.userApps.toFloat() to Color(0xFF2E7D32),
        state.disabledApps.toFloat() to Color(0xFF9E9E9E)
    )
    com.apptimemachine.ui.components.DonutChart(
        slices = slices,
        modifier = Modifier.size(112.dp),
        strokeWidthDp = 14.dp
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(state.totalApps.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Total Apps",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun BreakdownRow(icon: ImageVector, label: String, value: Int, total: Int, color: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(value.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        val fraction = if (total > 0) (value.toFloat() / total).coerceIn(0f, 1f) else 0f
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

@Composable
private fun TopCategoriesPanel(categories: List<CategoryStat>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
            .padding(14.dp)
    ) {
        Text("Top Categories", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        categories.forEachIndexed { index, stat ->
            val (icon, color) = categoryIcons[stat.label] ?: (Icons.Default.Apps to MaterialTheme.colorScheme.primary)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(stat.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(stat.count.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            if (index != categories.lastIndex) {
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}


/** Quick actions row: Statistics, Compare, Backup. */
@Composable
private fun QuickActionsGrid(
    onOpenStatistics: () -> Unit,
    onOpenCompare: () -> Unit,
    onOpenBackup: () -> Unit
) {
    Column {
        Text("Quick Actions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            QuickActionTile(
                icon = Icons.Default.BarChart,
                title = "Statistics",
                subtitle = "View insights and analytics",
                onClick = onOpenStatistics,
                accentColor = Color(0xFF1565C0),
                modifier = Modifier.weight(1f)
            )
            QuickActionTile(
                icon = Icons.Filled.CompareArrows,
                title = "Compare",
                subtitle = "Compare changes over time",
                onClick = onOpenCompare,
                accentColor = Color(0xFF6A1B9A),
                modifier = Modifier.weight(1f)
            )
            QuickActionTile(
                icon = Icons.Default.CloudUpload,
                title = "Backup",
                subtitle = "Backup your apps securely",
                onClick = onOpenBackup,
                accentColor = Color(0xFFE65100),
                modifier = Modifier.weight(1f)
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
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(19.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2
            )
        }
    }
}



/**
 * Two-pill tab selector for the Activity section — "System" (installs,
 * updates, permissions, storage, usage) vs "Notifications". Keeps the
 * two feeds visually and structurally separate instead of stacking two
 * always-visible sections that both showed notification-like content.
 */
@Composable
private fun ActivityTabSelector(selectedTab: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp)
    ) {
        ActivityTabPill(
            label = "System",
            icon = Icons.Outlined.History,
            selected = selectedTab == 0,
            onClick = { onSelect(0) },
            modifier = Modifier.weight(1f)
        )
        ActivityTabPill(
            label = "Notifications",
            icon = Icons.Outlined.NotificationsActive,
            selected = selectedTab == 1,
            onClick = { onSelect(1) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActivityTabPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyActivityCard() {
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
                    Icons.Outlined.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Monitoring started", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "Pull down to refresh and check for changes",
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

/**
 * Rule-based highlights, one row per insight, visually set apart from
 * every other card on the Dashboard with a soft gradient + glow border
 * so it reads as "smart" content rather than another plain stat card.
 */
@Composable
private fun InsightsCard(insights: List<DashboardInsight>) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val accent = Color(0xFF9B4FE0)
    val glassBg = if (isDark) {
        Brush.linearGradient(colors = listOf(Color(0xFF1E1030), Color(0xFF150C24)))
    } else {
        Brush.linearGradient(colors = listOf(Color(0xFFF5EDFF), Color(0xFFEDE0FF)))
    }
    val textPrimary = if (isDark) Color.White else Color(0xFF2A1245)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF5C3E7A)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Insights", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(glassBg)
                .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .padding(vertical = 6.dp)
        ) {
            insights.forEachIndexed { index, insight ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(accent.copy(alpha = if (isDark) 0.22f else 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            insightIcon(insight.icon),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(insight.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = textPrimary)
                        Text(insight.subtitle, style = MaterialTheme.typography.bodySmall, color = textSecondary)
                    }
                }
                if (index != insights.lastIndex) {
                    HorizontalDivider(color = accent.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

private fun insightIcon(icon: InsightIcon): ImageVector = when (icon) {
    InsightIcon.USAGE -> Icons.Outlined.Timelapse
    InsightIcon.INSTALL -> Icons.Outlined.GetApp
    InsightIcon.NOTIFICATIONS -> Icons.Outlined.NotificationsActive
    InsightIcon.STORAGE -> Icons.Outlined.Storage
    InsightIcon.BATTERY -> Icons.Outlined.BatteryChargingFull
}

/**
 * Compact permanent notification log for the Dashboard — each row shows
 * the source app, a title (or "OTP received" for OTP-flagged rows, never
 * the code itself), and relative time, with a trailing menu to open the
 * source app or delete the entry from this in-app log.
 */
@Composable
private fun RecentNotificationsCard(
    rows: List<com.apptimemachine.data.dao.NotificationFeedRow>,
    onOpenApp: (String) -> Unit,
    onDelete: (Long) -> Unit
) {
    AtmCard {
        if (rows.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.NotificationsNone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "No notifications captured yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            rows.forEachIndexed { index, row ->
                var showMenu by remember(row.notification.notificationHistoryId) { mutableStateOf(false) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.apptimemachine.ui.components.AppIcon(packageName = row.packageName, size = 38.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(row.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            when {
                                row.notification.isOtp -> "OTP received"
                                row.notification.title != null -> row.notification.title
                                else -> "New notification"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    Text(
                        Formatters.relativeTime(row.notification.postedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "More options", modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Open app") },
                                leadingIcon = { Icon(Icons.Outlined.OpenInNew, contentDescription = null) },
                                onClick = { showMenu = false; onOpenApp(row.packageName) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                                onClick = { showMenu = false; onDelete(row.notification.notificationHistoryId) }
                            )
                        }
                    }
                }
                if (index != rows.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}
