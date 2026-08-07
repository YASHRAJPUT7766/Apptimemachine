package com.apptimemachine.ui.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
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
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val pagingItems = viewModel.pagedEvents.collectAsLazyPagingItems()
    var showFilterSheet by remember { mutableStateOf(false) }

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
        Column(modifier = Modifier.padding(padding)) {
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
                        onClick = { viewModel.selectCategory(category) },
                        label = { Text(category.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            if (pagingItems.itemCount == 0) {
                EmptyState(
                    title = "Monitoring has started",
                    description = "Timeline events will appear automatically when supported changes are detected."
                )
                return@Scaffold
            }

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
                        is TimelineListItem.Event -> TimelineEventRow(item.event)
                        null -> ShimmerCard(Modifier.fillMaxWidth().height(72.dp))
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        TimelineFilterSheet(
            selectedCategory = selectedCategory,
            onSelect = { viewModel.selectCategory(it) },
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

/** Timeline row: live app icon, name, event description, colored severity dot, relative time. */
@Composable
fun TimelineEventRow(event: TimelineEventEntity, modifier: Modifier = Modifier) {
    AtmCard(modifier = modifier) {
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
