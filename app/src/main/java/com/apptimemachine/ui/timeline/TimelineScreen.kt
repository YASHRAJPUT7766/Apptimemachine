package com.apptimemachine.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.apptimemachine.core.utils.AppLauncher
import com.apptimemachine.core.utils.Formatters
import com.apptimemachine.data.entities.EventCategory
import com.apptimemachine.data.entities.EventSeverity
import com.apptimemachine.data.entities.TimelineEventEntity
import com.apptimemachine.ui.components.AppIcon
import com.apptimemachine.ui.components.AtmCard
import com.apptimemachine.ui.components.EmptyState
import com.apptimemachine.ui.components.ShimmerCard

/**
 * Part 3.6 Timeline UI. Uses Paging 3's LazyColumn integration so the
 * database can hold 100,000+ events without loading them all into memory
 * (Part 3.6 Timeline Performance). Events are grouped into day sections
 * (Today / Yesterday / date) via insertSeparators in the ViewModel, and
 * each row shows the app's live icon (Part 1.4A: never a blank icon).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    onOpenSearch: () -> Unit = {},
    onOpenAppDetails: (Long) -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val pagingItems = viewModel.pagedEvents.collectAsLazyPagingItems()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    // System/Notifications tab shown under "All" — System (0) is the
    // default; tapping Notifications navigates away immediately rather
    // than actually switching to a tab index 1, so this only ever needs
    // to represent "System selected".
    var allTab by rememberSaveable { mutableStateOf(0) }

    // Tapping the "Notifications" filter (chip or sheet) jumps straight to
    // the dedicated Notifications screen instead of filtering this list to
    // NOTIFICATIONS-category rows — those rows are lightweight Timeline
    // entries with just a short description, not the full title/body/
    // grouped detail view. Immediately reset the filter back to null so
    // Timeline itself is never left silently stuck on a category whose
    // rows it no longer shows inline.
    fun selectCategoryOrNavigate(category: EventCategory?) {
        if (category == EventCategory.NOTIFICATIONS) {
            onOpenNotifications()
        } else {
            viewModel.selectCategory(category)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timeline", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.padding(padding)
        ) {
            Column {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { viewModel.selectCategory(null) },
                            label = { Text("All") }
                        )
                    }
                    items(TIMELINE_FILTER_CATEGORIES) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectCategoryOrNavigate(category) },
                            label = { Text(category.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                // Under "All" (selectedCategory == null), show the same
                // System / Notifications split as Dashboard's Activity
                // section instead of one flat list that mixes both —
                // System stays inline here; Notifications jumps straight to
                // the dedicated Notifications screen, same as tapping its
                // filter chip above does.
                if (selectedCategory == null) {
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        SystemNotificationsTabSelector(
                            selectedTab = allTab,
                            onSelectSystem = { allTab = 0 },
                            onSelectNotifications = { onOpenNotifications() }
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }

                // Battery-drain-proxy and network-usage fixed cards
                // intentionally removed from Timeline — Timeline now shows
                // only the plain event list, same as before this card was
                // added. Per-app battery/network is still available in
                // App Details.

                if (pagingItems.itemCount == 0) {
                    // Still wrapped by PullToRefreshBox's own scroll container, so
                    // swiping down works even before any event has ever landed.
                    EmptyState(
                        title = "Pull down to refresh",
                        description = "Swipe down anytime to check for new installs, updates, permission and storage changes."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            count = pagingItems.itemCount,
                            key = pagingItems.itemKey { item ->
                                when (item) {
                                    is TimelineListItem.Header -> "header_${item.dayKey}"
                                    is TimelineListItem.Event -> "event_${item.event.eventId}"
                                }
                            },
                            contentType = { index ->
                                when (pagingItems.peek(index)) {
                                    is TimelineListItem.Header -> "header"
                                    else -> "event"
                                }
                            }
                        ) { index ->
                            when (val item = pagingItems[index]) {
                                is TimelineListItem.Header -> DaySectionHeader(item.label)
                                is TimelineListItem.Event -> {
                                    // Under "All" + System tab, skip NOTIFICATIONS-
                                    // category rows entirely — they only ever show
                                    // in the dedicated Notifications screen now.
                                    if (selectedCategory == null && item.event.eventCategory == EventCategory.NOTIFICATIONS) {
                                        // no-op: filtered out of the System view
                                    } else {
                                        TimelineEventRow(
                                            event = item.event,
                                            onClick = { onOpenAppDetails(item.event.appId) },
                                            onDeleteNotification = if (item.event.eventCategory == EventCategory.NOTIFICATIONS) {
                                                { viewModel.deleteNotificationEvent(item.event) }
                                            } else null
                                        )
                                    }
                                }
                                null -> ShimmerCard(Modifier.fillMaxWidth().height(72.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        TimelineFilterSheet(
            selectedCategory = selectedCategory,
            onSelect = { selectCategoryOrNavigate(it) },
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
private fun DaySectionHeader(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

/**
 * Two-pill tab selector shown under "All" — mirrors Dashboard's Activity
 * tabs. "System" stays inline (installs/updates/permissions/storage/
 * usage); "Notifications" is a one-shot navigation trigger straight to
 * the dedicated Notifications screen rather than an actual second tab
 * state, since notification rows are never rendered inline here anymore.
 */
@Composable
private fun SystemNotificationsTabSelector(
    selectedTab: Int,
    onSelectSystem: () -> Unit,
    onSelectNotifications: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp)
    ) {
        TimelineTabPill(
            label = "System",
            icon = Icons.Outlined.History,
            selected = selectedTab == 0,
            onClick = onSelectSystem,
            modifier = Modifier.weight(1f)
        )
        TimelineTabPill(
            label = "Notifications",
            icon = Icons.Outlined.NotificationsActive,
            selected = false,
            onClick = onSelectNotifications,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TimelineTabPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

/** Timeline row: live app icon, name, event description, colored severity dot, relative time. Tappable to open App Details when [onClick] is provided (Timeline and App Details' own Timeline tab share this row; the tab passes no onClick since it's already inside that app's details). For NOTIFICATIONS-category events, [onDeleteNotification] adds a trailing menu with "Open app" and "Delete" (delete removes it from this in-app log only — the source notification on the device, if still present, is untouched). */
@Composable
fun TimelineEventRow(
    event: TimelineEventEntity,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onDeleteNotification: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showMenu by remember(event.eventId) { mutableStateOf(false) }

    AtmCard(modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                AppIcon(packageName = event.packageName, size = 44.dp)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(1.dp)
                        .size(10.dp)
                ) {
                    SeverityDot(event.severity)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(
                    if (event.eventCategory == EventCategory.NOTIFICATIONS && event.newValue != null) {
                        event.newValue
                    } else {
                        event.eventType.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Text(
                Formatters.relativeTime(event.createdTimestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (onDeleteNotification != null) {
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "More options", modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Open app") },
                            leadingIcon = { Icon(Icons.Outlined.OpenInNew, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                AppLauncher.open(context, event.packageName)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onDeleteNotification()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SeverityDot(severity: EventSeverity) {
    val color = when (severity) {
        EventSeverity.INFO -> MaterialTheme.colorScheme.tertiary
        EventSeverity.SUCCESS -> Color(0xFF2E7D32)
        EventSeverity.WARNING -> Color(0xFFED6C02)
        EventSeverity.IMPORTANT -> MaterialTheme.colorScheme.primary
        EventSeverity.CRITICAL -> Color(0xFFD32F2F)
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(color = Color.White, radius = size.minDimension / 2)
        drawCircle(color = color, radius = size.minDimension / 2.6f)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimelineFilterSheet(
    selectedCategory: EventCategory?,
    onSelect: (EventCategory?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 24.dp)) {
            Text("Filter by category", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            FilterRow("All", selectedCategory == null) { onSelect(null); onDismiss() }
            EventCategory.entries.forEach { category ->
                FilterRow(
                    category.name.lowercase().replaceFirstChar { it.uppercase() },
                    selectedCategory == category
                ) { onSelect(category); onDismiss() }
            }
        }
    }
}

@Composable
private fun FilterRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        RadioButton(selected = selected, onClick = onClick)
    }
}
