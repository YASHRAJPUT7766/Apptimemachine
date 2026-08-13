package com.apptimemachine.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.apptimemachine.core.utils.AppLauncher
import com.apptimemachine.core.utils.Formatters
import com.apptimemachine.data.entities.InstalledAppEntity
import com.apptimemachine.ui.components.AppIcon
import com.apptimemachine.ui.components.AtmCard
import com.apptimemachine.ui.components.CATEGORY_ALL
import com.apptimemachine.ui.components.EmptyState
import com.apptimemachine.ui.components.categoryStyle

/**
 * Apps tab: search, a horizontal category filter row (mirrors Dashboard's
 * Top Categories so a category always looks the same everywhere), and a
 * "Browse categories" picker sheet for choosing one from a full grid.
 * Selecting a category (from either the row or the sheet) narrows the list
 * below to just that category — e.g. tapping "Game" shows only games.
 *
 * Each row keeps the existing favorite star and adds an "Open" button next
 * to it that launches the app directly via [AppLauncher].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsListScreen(
    onOpenAppDetails: (Long) -> Unit,
    viewModel: AppsListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val query by viewModel.query.collectAsState()
    var showCategoryPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = { Text("Search apps") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            CategoryFilterRow(
                categories = state.categories,
                selected = state.selectedCategory,
                onSelect = viewModel::onCategorySelected,
                onBrowseAll = { showCategoryPicker = true }
            )

            if (state.apps.isEmpty() && !state.isLoading) {
                EmptyState(
                    title = if (state.selectedCategory == CATEGORY_ALL) "No applications found" else "No apps in this category",
                    description = if (state.selectedCategory == CATEGORY_ALL) "Try a different search term." else "Try a different category or search term."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.apps, key = { it.appId }, contentType = { "app_row" }) { app ->
                        AppRow(
                            app = app,
                            onClick = { onOpenAppDetails(app.appId) },
                            onFavoriteClick = { viewModel.toggleFavorite(app.appId, app.isFavorite) }
                        )
                    }
                }
            }
        }
    }

    if (showCategoryPicker) {
        CategoryPickerSheet(
            categories = state.categories,
            selected = state.selectedCategory,
            onSelect = {
                viewModel.onCategorySelected(it)
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false }
        )
    }
}

/**
 * Horizontal chip row, always headed by a "Tune" icon chip that opens the
 * full picker sheet — so the row works as a quick-select for a few
 * categories at a glance, with the sheet as the "show me everything" path.
 */
@Composable
private fun CategoryFilterRow(
    categories: List<CategoryFilterOption>,
    selected: String,
    onSelect: (String) -> Unit,
    onBrowseAll: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "browse_all") {
            FilledIconButton(
                onClick = onBrowseAll,
                shape = RoundedCornerShape(50),
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Icon(Icons.Default.Tune, contentDescription = "Browse all categories", modifier = Modifier.size(18.dp))
            }
        }
        items(categories, key = { it.label }) { option ->
            CategoryChip(
                option = option,
                selected = option.label == selected,
                onClick = { onSelect(option.label) }
            )
        }
    }
}

@Composable
private fun CategoryChip(option: CategoryFilterOption, selected: Boolean, onClick: () -> Unit) {
    val (icon, color) = if (option.label == CATEGORY_ALL) {
        Icons.Default.Tune to MaterialTheme.colorScheme.primary
    } else {
        categoryStyle(option.label)
    }
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text("${option.label} · ${option.count}") },
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = if (selected) LocalContentColor.current else color, modifier = Modifier.size(16.dp))
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.18f),
            selectedLabelColor = color,
            selectedLeadingIconColor = color
        )
    )
}

/**
 * Full category picker, presented as a large bottom sheet with a grid of
 * icon tiles — one tile per category, each showing its icon, name, and how
 * many apps are in it. This is the "choose however you like" category
 * browser: every category the Dashboard already tracks shows up here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPickerSheet(
    categories: List<CategoryFilterOption>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("Browse by category", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Pick a category to see just those apps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
            ) {
                items(categories, key = { it.label }) { option ->
                    CategoryTile(
                        option = option,
                        selected = option.label == selected,
                        onClick = { onSelect(option.label) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun CategoryTile(option: CategoryFilterOption, selected: Boolean, onClick: () -> Unit) {
    val (icon, color) = if (option.label == CATEGORY_ALL) {
        Icons.Default.Tune to MaterialTheme.colorScheme.primary
    } else {
        categoryStyle(option.label)
    }
    AtmCard(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (selected) color.copy(alpha = 0.28f) else color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                option.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1
            )
            Text(
                "${option.count} app${if (option.count == 1) "" else "s"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AppRow(app: InstalledAppEntity, onClick: () -> Unit, onFavoriteClick: () -> Unit) {
    val context = LocalContext.current
    AtmCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(packageName = app.packageName, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(app.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(
                    "${app.packageName} • v${app.versionName ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                val totalStorage = (app.appSizeBytes ?: 0) + (app.dataSizeBytes ?: 0) + (app.cacheSizeBytes ?: 0)
                Text(
                    Formatters.bytes(if (totalStorage > 0) totalStorage else null),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    if (app.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (app.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalIconButton(
                onClick = { AppLauncher.open(context, app.packageName) },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open app", modifier = Modifier.size(18.dp))
            }
        }
    }
}
