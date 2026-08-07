package com.apptimemachine.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.apptimemachine.data.entities.RecentSearchEntity
import com.apptimemachine.ui.components.AppIcon
import com.apptimemachine.ui.components.AtmCard
import com.apptimemachine.ui.components.EmptyState
import com.apptimemachine.ui.components.SectionHeader

/** Part 2.9 Advanced Search Engine — real-time global search UI. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(onBack: () -> Unit, onOpenAppDetails: (Long) -> Unit, viewModel: SearchViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = { Text("Search apps, packages, events…") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        keyboardActions = KeyboardActions(
                            onSearch = { viewModel.commitSearch(state.query, state.matchingApps.size) }
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        if (state.query.isBlank()) {
            RecentSearchesView(
                recents = state.recentSearches,
                onSelect = viewModel::useRecentSearch,
                onClear = viewModel::clearRecentSearches,
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        if (state.matchingApps.isEmpty()) {
            EmptyState(
                title = "No results found",
                description = "Try a different keyword or remove filters.",
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { SectionHeader("Applications") }
            items(state.matchingApps, key = { it.appId }) { app ->
                AtmCard(onClick = { onOpenAppDetails(app.appId) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(packageName = app.packageName, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(app.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                            Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentSearchesView(
    recents: List<RecentSearchEntity>,
    onSelect: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (recents.isEmpty()) {
        EmptyState(
            title = "Search everything",
            description = "Find apps, packages, versions, or timeline events.",
            icon = Icons.Default.Search,
            modifier = modifier
        )
        return
    }
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(16.dp)) {
        item {
            SectionHeader("Recent Searches", action = {
                TextButton(onClick = onClear) { Text("Clear") }
            })
        }
        items(recents, key = { it.recentSearchId }) { recent ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(recent.keyword) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text(recent.keyword, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text("${recent.resultCount} results", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
