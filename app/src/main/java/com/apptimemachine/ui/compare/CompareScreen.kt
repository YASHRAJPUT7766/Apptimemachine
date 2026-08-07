package com.apptimemachine.ui.compare

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.apptimemachine.core.utils.Formatters
import com.apptimemachine.data.entities.InstalledAppEntity
import com.apptimemachine.ui.components.AtmCard
import com.apptimemachine.ui.components.SectionHeader

/** Part 3.7 Application Comparison Engine UI. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(viewModel: CompareViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var pickingFor by remember { mutableStateOf<Char?>(null) } // 'A' or 'B' while the picker sheet is open

    Scaffold(topBar = { TopAppBar(title = { Text("Compare Apps") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    AppSlot(
                        label = "App A",
                        app = state.selectedA,
                        onClick = { pickingFor = 'A' },
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.align(Alignment.CenterVertically))
                    AppSlot(
                        label = "App B",
                        app = state.selectedB,
                        onClick = { pickingFor = 'B' },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Button(
                    onClick = { viewModel.runComparison() },
                    enabled = state.selectedA != null && state.selectedB != null && !state.isComparing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isComparing) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Compare")
                    }
                }
            }

            state.result?.let { result ->
                item {
                    AtmCard {
                        Text(result.summary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                item { SectionHeader("Storage") }
                item {
                    AtmCard {
                        ComparisonRow(result.appA.appName, Formatters.bytes(result.storageA), result.appB.appName, Formatters.bytes(result.storageB))
                    }
                }
                item { SectionHeader("Version") }
                item {
                    AtmCard {
                        ComparisonRow(result.appA.appName, "${result.updateCountA} updates", result.appB.appName, "${result.updateCountB} updates")
                    }
                }
                item { SectionHeader("Timeline") }
                item {
                    AtmCard {
                        ComparisonRow(result.appA.appName, "${result.eventCountA} events", result.appB.appName, "${result.eventCountB} events")
                    }
                }
                item { SectionHeader("Permissions") }
                item {
                    AtmCard {
                        Text("Only in ${result.appA.appName}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        PermissionList(result.onlyInA)
                        Spacer(Modifier.height(12.dp))
                        Text("Only in ${result.appB.appName}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        PermissionList(result.onlyInB)
                        Spacer(Modifier.height(12.dp))
                        Text("Shared", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        PermissionList(result.sharedPermissions)
                    }
                }
            }
        }
    }

    if (pickingFor != null) {
        AppPickerSheet(
            apps = state.allApps,
            onSelect = { appId ->
                if (pickingFor == 'A') viewModel.selectAppA(appId) else viewModel.selectAppB(appId)
                pickingFor = null
            },
            onDismiss = { pickingFor = null }
        )
    }
}

@Composable
private fun AppSlot(label: String, app: InstalledAppEntity?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AtmCard(onClick = onClick, modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(app?.appName ?: "Select app", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ComparisonRow(labelA: String, valueA: String, labelB: String, valueB: String) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Column { Text(labelA, style = MaterialTheme.typography.labelSmall); Text(valueA, fontWeight = FontWeight.Medium) }
        Column(horizontalAlignment = Alignment.End) { Text(labelB, style = MaterialTheme.typography.labelSmall); Text(valueB, fontWeight = FontWeight.Medium) }
    }
}

@Composable
private fun PermissionList(permissions: Set<String>) {
    if (permissions.isEmpty()) {
        Text("None", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    permissions.forEach {
        Text("• ${it.substringAfterLast('.')}", style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPickerSheet(apps: List<InstalledAppEntity>, onSelect: (Long) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(contentPadding = PaddingValues(16.dp), modifier = Modifier.height(400.dp)) {
            items(apps, key = { it.appId }) { app ->
                ListItem(
                    headlineContent = { Text(app.appName) },
                    supportingContent = { Text(app.packageName) },
                    modifier = Modifier.clickable { onSelect(app.appId) }
                )
            }
        }
    }
}
