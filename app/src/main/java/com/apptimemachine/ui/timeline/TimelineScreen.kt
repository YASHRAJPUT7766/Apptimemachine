package com.apptimemachine.ui.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.apptimemachine.data.entities.EventCategory
import com.apptimemachine.ui.components.EmptyState
import com.apptimemachine.ui.components.ShimmerCard
import com.apptimemachine.ui.dashboard.TimelineEventRow

/**
 * Part 3.6 Timeline UI. Uses Paging 3's LazyColumn integration so the
 * database can hold 100,000+ events without loading them all into memory
 * (Part 3.6 Timeline Performance).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(viewModel: TimelineViewModel = hiltViewModel()) {
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val pagingItems = viewModel.pagedEvents.collectAsLazyPagingItems()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Timeline") }) }
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
                items(EventCategory.entries.toList()) { category ->
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
                    key = pagingItems.itemKey { it.eventId }
                ) { index ->
                    val event = pagingItems[index]
                    if (event != null) {
                        TimelineEventRow(event)
                    } else {
                        ShimmerCard(Modifier.fillMaxWidth().height(72.dp))
                    }
                }
            }
        }
    }
}
