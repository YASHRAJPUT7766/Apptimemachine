package com.apptimemachine.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
 * Activity > Notifications tab, Timeline's "All" System/Notifications tabs
 * and Notifications filter chip, and Settings > General.
 *
 * Tapping ANY row — a collapsed group or a single stand-alone notification
 * — opens the same large centered detail Dialog: full-screen scrim behind
 * it, an internally scrollable card so any number of notifications fit,
 * and Open/Delete on every entry.
 */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val items by viewModel.items.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val detail by viewModel.detail.collectAsState()
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
                            // Both a single notification and a group open the
                            // same detail dialog — a single just opens it
                            // pre-populated with its one row.
                            is NotificationListItem.Single -> NotificationRow(
                                row = item.row,
                                onClick = { viewModel.openDetail(item.row.appName, item.row.packageName, listOf(item.row)) }
                            )
                            is NotificationListItem.Group -> NotificationGroupRow(
                                group = item,
                                onClick = { viewModel.openDetail(item.appName, item.packageName, item.rows) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (detail != null) {
        NotificationDetailDialog(
            detail = detail!!,
            onDismiss = { viewModel.closeDetail() },
            onOpenApp = { pkg -> AppLauncher.open(context, pkg) },
            onDeleteOne = { id -> viewModel.deleteOne(id) },
            onDeleteAll = { rows -> viewModel.deleteAll(rows) }
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

/** A single, non-grouped notification — tap opens the full-detail dialog (same as a group). */
@Composable
private fun NotificationRow(row: NotificationFeedRow, onClick: () -> Unit) {
    AtmCard(onClick = onClick) {
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
            Text(
                Formatters.time(row.notification.postedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** A collapsed same-app group ("WhatsApp • 5 notifications") — tap opens the full-detail dialog. */
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
 * Full-detail popup for both a group and a single notification — a large
 * centered card over a dimmed full-screen scrim (Dialog, not a bottom
 * sheet), with its own internal scroll so any number of notifications
 * fit and the rest of the screen never needs to scroll around it. Every
 * entry shows full title/body (privacy-mode-aware) and has its own
 * Open/Delete actions; "Delete all" clears everything at once.
 */
@Composable
private fun NotificationDetailDialog(
    detail: NotificationDetailState,
    onDismiss: () -> Unit,
    onOpenApp: (String) -> Unit,
    onDeleteOne: (Long) -> Unit,
    onDeleteAll: (List<NotificationFeedRow>) -> Unit
) {
    // Local mutable copy so a delete removes the row from the open dialog
    // immediately instead of waiting for the underlying Flow to catch up
    // (which would otherwise briefly show a stale, already-deleted row).
    var remaining by remember(detail) { mutableStateOf(detail.rows) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                // Header — stays fixed while the notification list below scrolls.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon(packageName = detail.packageName, size = 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(detail.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "${remaining.size} notification${if (remaining.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                if (remaining.isEmpty()) {
                    Text(
                        "All notifications here were deleted.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp)
                    )
                } else {
                    // The scrollable region: as many notifications as exist,
                    // capped only by the outer Column's max height above —
                    // this is what fixes the earlier bottom-sheet version
                    // not scrolling when a group had many entries.
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(Modifier.height(4.dp))
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
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }

                    if (remaining.size > 1) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        TextButton(
                            onClick = { onDeleteAll(remaining) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Icon(Icons.Outlined.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Delete all")
                        }
                    }
                }
            }
        }
    }
}

/** One notification's full detail inside the dialog — title/body, time, Open and Delete. */
@Composable
private fun NotificationDetailRow(
    row: NotificationFeedRow,
    onOpenApp: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
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
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onOpenApp, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.OpenInNew, contentDescription = "Open app", modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
            }
        }
    }
}

/**
 * All three preview/detail text helpers below respect the privacy mode
 * the notification was actually captured under — title/body are only
 * ever populated by the listener when Settings > Notification Privacy
 * Mode allowed it, and OTP-flagged notifications never carry content at
 * all (OTP content is intentionally never persisted). Nothing here
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
