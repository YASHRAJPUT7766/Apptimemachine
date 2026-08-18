package com.apptimemachine.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.apptimemachine.core.utils.AppLauncher
import com.apptimemachine.core.utils.Formatters
import com.apptimemachine.data.dao.NotificationFeedRow
import com.apptimemachine.data.entities.NotificationHistoryEntity
import com.apptimemachine.data.entities.NotificationPrivacyMode
import com.apptimemachine.ui.components.AppIcon
import com.apptimemachine.ui.components.AtmCard
import com.apptimemachine.ui.components.EmptyState

/**
 * Dedicated, full-detail Notifications screen — day-grouped, with 3+
 * same-app-same-day notifications collapsing into one tappable group card
 * (mirrors the phone's own notification shade). Reachable from Dashboard's
 * Activity > Notifications tab, Timeline's Notifications filter, and
 * Settings > General.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val items by viewModel.items.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val expandedGroup by viewModel.expandedGroup.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            if (items.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.NotificationsNone,
                    title = "No notifications yet",
                    description = "Notifications captured from other apps will show up here, grouped by day."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = items,
                        key = { item ->
                            when (item) {
                                is NotificationListItem.DayHeader -> "header_${item.dayKey}"
                                is NotificationListItem.Single -> "single_${item.row.notification.notificationHistoryId}"
                                is NotificationListItem.Group -> "group_${item.packageName}_${item.rows.first().notification.postedAt}"
                            }
                        }
                    ) { item ->
                        when (item) {
                            is NotificationListItem.DayHeader -> DayHeaderRow(item.label)
                            is NotificationListItem.Single -> NotificationRow(
                                row = item.row,
                                onOpenApp = { AppLauncher.open(context, item.row.packageName) },
                                onDelete = { viewModel.delete(item.row.notification.notificationHistoryId) }
                            )
                            is NotificationListItem.Group -> NotificationGroupRow(
                                group = item,
                                onClick = { viewModel.openGroup(item) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (expandedGroup != null) {
        NotificationGroupDetailSheet(
            group = expandedGroup!!,
            onDismiss = { viewModel.closeGroup() },
            onOpenApp = { pkg -> AppLauncher.open(context, pkg) },
            onDeleteOne = { id -> viewModel.delete(id) },
            onDeleteAll = { rows -> viewModel.deleteGroup(rows) }
        )
    }
}

@Composable
private fun DayHeaderRow(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

/** A single notification — tap opens the source app, trailing menu offers delete. */
@Composable
private fun NotificationRow(
    row: NotificationFeedRow,
    onOpenApp: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember(row.notification.notificationHistoryId) { mutableStateOf(false) }

    AtmCard(onClick = onOpenApp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(packageName = row.packageName, size = 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(row.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    notificationPreviewText(row.notification),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    Formatters.time(row.notification.postedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Open app") },
                            leadingIcon = { Icon(Icons.Outlined.OpenInNew, contentDescription = null) },
                            onClick = { showMenu = false; onOpenApp() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }
        }
    }
}

/** A collapsed same-app group ("WhatsApp • 5 notifications") — tap to expand into full detail. */
@Composable
private fun NotificationGroupRow(group: NotificationListItem.Group, onClick: () -> Unit) {
    AtmCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                AppIcon(packageName = group.packageName, size = 42.dp)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        group.rows.size.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(group.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${group.rows.size} notifications — tap to view all",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                Formatters.time(group.rows.first().notification.postedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Full detail for a group — every individual notification, newest first,
 * each with its own title/body (privacy-mode-aware), time, tap-to-open,
 * and delete. A "Delete all" action clears the whole group at once.
 */
@Composable
private fun NotificationGroupDetailSheet(
    group: NotificationListItem.Group,
    onDismiss: () -> Unit,
    onOpenApp: (String) -> Unit,
    onDeleteOne: (Long) -> Unit,
    onDeleteAll: (List<NotificationFeedRow>) -> Unit
) {
    // Local mutable copy so a delete removes the row from the open sheet
    // immediately instead of waiting for the underlying Flow to catch up
    // (which would otherwise briefly show a stale, already-deleted row).
    var remaining by remember(group) { mutableStateOf(group.rows) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(packageName = group.packageName, size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(group.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${remaining.size} notifications",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = {
                    onDeleteAll(remaining)
                }) {
                    Icon(Icons.Outlined.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete all")
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            if (remaining.isEmpty()) {
                Text(
                    "All notifications in this group were deleted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                remaining.forEachIndexed { index, row ->
                    NotificationDetailRow(
                        row = row,
                        onOpenApp = { onOpenApp(row.packageName) },
                        onDelete = {
                            onDeleteOne(row.notification.notificationHistoryId)
                            remaining = remaining.filterNot { it.notification.notificationHistoryId == row.notification.notificationHistoryId }
                        }
                    )
                    if (index != remaining.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationDetailRow(
    row: NotificationFeedRow,
    onOpenApp: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpenApp)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                notificationDetailTitle(row.notification),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            val body = notificationDetailBody(row.notification)
            if (body != null) {
                Spacer(Modifier.height(2.dp))
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                Formatters.dateTime(row.notification.postedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * All three preview/detail text helpers below respect the privacy mode
 * the notification was actually captured under — title/body are only
 * ever populated by the listener when Settings > Notification Privacy
 * Mode allowed it, and OTP-flagged notifications never carry content at
 * all (Part: OTP content is intentionally never persisted). Nothing here
 * fabricates content that wasn't captured — a missing title/body always
 * falls back to an honest placeholder, never a guess at what it said.
 */
private fun notificationPreviewText(n: NotificationHistoryEntity): String = when {
    n.isOtp -> "OTP received"
    n.body != null -> n.body
    n.title != null -> n.title
    n.privacyModeUsed == NotificationPrivacyMode.METADATA_ONLY -> "New notification (content not captured)"
    else -> "New notification"
}

private fun notificationDetailTitle(n: NotificationHistoryEntity): String = when {
    n.isOtp -> "OTP received"
    n.title != null -> n.title
    n.body != null -> n.body
    else -> "New notification"
}

private fun notificationDetailBody(n: NotificationHistoryEntity): String? = when {
    n.isOtp -> null
    n.body != null && n.body != n.title -> n.body
    n.title == null && n.body == null -> "Content not captured — enable \"Metadata + Title\" or \"Full\" in Settings > Notification Privacy Mode to see this."
    else -> null
}
