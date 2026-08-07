package com.apptimemachine.ui.apps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.apptimemachine.core.utils.Formatters
import com.apptimemachine.data.entities.InstalledAppEntity
import com.apptimemachine.ui.components.AtmCard
import com.apptimemachine.ui.components.EmptyState

/**
 * Part 2.7 / Part 1.4A: large-row app list with icon, name, package,
 * version, storage. App icons are loaded live via PackageManager (Coil's
 * generic Any-model loader resolves a "package:<name>" URI through a
 * custom fetcher registered in ImageLoaderModule) rather than cached to
 * disk, keeping Rule 1 (never fabricate) trivially satisfied for icons too.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsListScreen(
    onOpenAppDetails: (Long) -> Unit,
    viewModel: AppsListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val query by viewModel.query.collectAsState()

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
        if (state.apps.isEmpty() && !state.isLoading) {
            EmptyState(
                title = "No applications found",
                description = "Try a different search term.",
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.apps, key = { it.appId }) { app ->
                AppRow(
                    app = app,
                    onClick = { onOpenAppDetails(app.appId) },
                    onFavoriteClick = { viewModel.toggleFavorite(app.appId, app.isFavorite) }
                )
            }
        }
    }
}

@Composable
private fun AppRow(app: InstalledAppEntity, onClick: () -> Unit, onFavoriteClick: () -> Unit) {
    val context = LocalContext.current
    AtmCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = ImageRequest.Builder(context).data("package:${app.packageName}").build(),
                contentDescription = null,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
            )
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
        }
    }
}
