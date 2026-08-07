package com.apptimemachine.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.apptimemachine.core.datastore.AppTheme
import com.apptimemachine.core.datastore.ScanInterval
import com.apptimemachine.data.entities.NotificationPrivacyMode
import com.apptimemachine.ui.components.AtmCard
import com.apptimemachine.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionHeader("Appearance") }
            item {
                AtmCard {
                    SettingRow("Dynamic Color", state.dynamicColor, viewModel::setDynamicColor)
                    SettingRow("AMOLED Mode", state.amoledMode, viewModel::setAmoledMode)
                    Spacer(Modifier.height(8.dp))
                    Text("Theme", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppTheme.entries.forEach { theme ->
                            FilterChip(
                                selected = state.theme == theme,
                                onClick = { viewModel.setTheme(theme) },
                                label = { Text(theme.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
            }

            item { SectionHeader("Monitoring") }
            item {
                AtmCard {
                    SettingRow("Scan on Boot", state.scanOnBoot, viewModel::setScanOnBoot)
                    SettingRow("Scan While Charging", state.scanWhileCharging, viewModel::setScanWhileCharging)
                    Spacer(Modifier.height(8.dp))
                    Text("Quick Scan Interval", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    ScanIntervalSelector(state.quickScanInterval, viewModel::setQuickScanInterval)
                }
            }

            item { SectionHeader("Privacy") }
            item {
                AtmCard {
                    Text("Notification Privacy Mode", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Column {
                        NotificationPrivacyMode.entries.forEach { mode ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(privacyModeLabel(mode), style = MaterialTheme.typography.bodyMedium)
                                RadioButton(
                                    selected = state.notificationPrivacyMode == mode,
                                    onClick = { viewModel.setNotificationPrivacyMode(mode) }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    SettingRow("Require App Lock", state.appLockEnabled, viewModel::setAppLockEnabled)
                }
            }

            item { SectionHeader("Backup") }
            item {
                AtmCard {
                    SettingRow("Automatic Backup", state.autoBackupEnabled, viewModel::setAutoBackupEnabled)
                }
            }

            item {
                Text(
                    "App Time Machine works completely offline. No monitoring data is uploaded automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ScanIntervalSelector(selected: ScanInterval, onSelect: (ScanInterval) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ScanInterval.entries.forEach { interval ->
            FilterChip(
                selected = selected == interval,
                onClick = { onSelect(interval) },
                label = { Text(intervalLabel(interval)) }
            )
        }
    }
}

private fun intervalLabel(interval: ScanInterval): String = when (interval) {
    ScanInterval.FIFTEEN_MIN -> "15m"
    ScanInterval.THIRTY_MIN -> "30m"
    ScanInterval.ONE_HOUR -> "1h"
    ScanInterval.THREE_HOURS -> "3h"
    ScanInterval.SIX_HOURS -> "6h"
    ScanInterval.TWELVE_HOURS -> "12h"
    ScanInterval.TWENTY_FOUR_HOURS -> "24h"
}

private fun privacyModeLabel(mode: NotificationPrivacyMode): String = when (mode) {
    NotificationPrivacyMode.METADATA_ONLY -> "Metadata Only (Recommended)"
    NotificationPrivacyMode.METADATA_PLUS_TITLE -> "Metadata + Title"
    NotificationPrivacyMode.FULL -> "Full Notification"
}
