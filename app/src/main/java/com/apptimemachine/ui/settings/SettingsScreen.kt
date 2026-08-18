package com.apptimemachine.ui.settings

import android.content.ActivityNotFoundException
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.apptimemachine.core.datastore.AppTheme
import com.apptimemachine.core.datastore.ScanInterval
import com.apptimemachine.core.monitoring.PermissionHelper
import com.apptimemachine.data.entities.NotificationPrivacyMode
import com.apptimemachine.ui.components.AtmCard

/**
 * Settings — restyled as icon-led rows grouped into cards, matching the
 * Dashboard's rounded-card / purple-accent language instead of plain
 * label+switch rows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onOpenReports: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                SettingsSectionHeader("Appearance", Icons.Outlined.Palette)
                AtmCard {
                    IconSettingRow(
                        icon = Icons.Outlined.ColorLens,
                        label = "Dynamic Color",
                        description = "Match colors to your wallpaper",
                        checked = state.dynamicColor,
                        onCheckedChange = viewModel::setDynamicColor
                    )
                    SettingsDivider()
                    IconSettingRow(
                        icon = Icons.Outlined.DarkMode,
                        label = "AMOLED Mode",
                        description = "Pure black backgrounds to save battery",
                        checked = state.amoledMode,
                        onCheckedChange = viewModel::setAmoledMode
                    )
                    SettingsDivider()
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Theme",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppTheme.entries.forEach { theme ->
                            FilterChip(
                                selected = state.theme == theme,
                                onClick = { viewModel.setTheme(theme) },
                                label = { Text(theme.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            item {
                SettingsSectionHeader("Monitoring", Icons.Outlined.Radar)
                AtmCard {
                    IconSettingRow(
                        icon = Icons.Outlined.PowerSettingsNew,
                        label = "Scan on Boot",
                        description = "Run a scan automatically after restart",
                        checked = state.scanOnBoot,
                        onCheckedChange = viewModel::setScanOnBoot
                    )
                    SettingsDivider()
                    IconSettingRow(
                        icon = Icons.Outlined.BatteryChargingFull,
                        label = "Scan While Charging",
                        description = "Prioritize scans during charging sessions",
                        checked = state.scanWhileCharging,
                        onCheckedChange = viewModel::setScanWhileCharging
                    )
                    SettingsDivider()
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingsIconChip(Icons.Outlined.Timer)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Quick Scan Interval",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    ScanIntervalSelector(state.quickScanInterval, viewModel::setQuickScanInterval)
                }
            }

            item {
                SettingsSectionHeader("Reliable Notifications", Icons.Outlined.NotificationsActive)
                ReliableNotificationsCard()
            }

            item {
                SettingsSectionHeader("Privacy", Icons.Outlined.Shield)
                AtmCard {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                    var notificationAccessGranted by remember {
                        mutableStateOf(com.apptimemachine.core.monitoring.PermissionHelper.hasNotificationListenerAccess(context))
                    }
                    // Tracks whether "Grant" has already been tapped once
                    // without success — on Android 13+, sideloaded builds
                    // (GitHub Actions APK, Aptoide) get blocked by a
                    // "Restricted setting" system dialog the first time,
                    // and the toggle screen offers no way around it. Once
                    // that's happened, show the "Allow restricted settings"
                    // instructions instead of just letting the person tap
                    // "Grant" again and hit the same dialog.
                    var grantAttempted by remember { mutableStateOf(false) }
                    DisposableEffect(lifecycleOwner) {
                        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                                notificationAccessGranted = com.apptimemachine.core.monitoring.PermissionHelper.hasNotificationListenerAccess(context)
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingsIconChip(Icons.Outlined.NotificationsActive)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Notification Access", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                if (notificationAccessGranted) "Granted — notifications are being logged"
                                else "Required for the Notifications log to capture anything",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (notificationAccessGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                        if (!notificationAccessGranted) {
                            FilledTonalButton(onClick = {
                                grantAttempted = true
                                context.startActivity(com.apptimemachine.core.monitoring.PermissionHelper.notificationListenerIntent())
                            }) { Text("Grant") }
                        } else {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (!notificationAccessGranted && grantAttempted) {
                        Spacer(Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
                                .padding(12.dp)
                        ) {
                            Text(
                                "Seeing \"Restricted setting — for your security\"? That's Android blocking this because the app wasn't installed from the Play Store.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Fix: open App Info below → tap the ⋮ menu (top right) → \"Allow restricted settings\" → then try Grant again.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            FilledTonalButton(
                                onClick = { context.startActivity(com.apptimemachine.core.monitoring.PermissionHelper.appInfoIntent(context)) },
                                modifier = Modifier.align(Alignment.End)
                            ) { Text("Open App Info") }
                        }
                    }
                    SettingsDivider()

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingsIconChip(Icons.Outlined.VisibilityOff)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Notification Privacy Mode",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(vertical = 4.dp)
                    ) {
                        NotificationPrivacyMode.entries.forEachIndexed { index, mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(privacyModeLabel(mode), style = MaterialTheme.typography.bodyMedium)
                                RadioButton(
                                    selected = state.notificationPrivacyMode == mode,
                                    onClick = { viewModel.setNotificationPrivacyMode(mode) }
                                )
                            }
                            if (index != NotificationPrivacyMode.entries.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            }
                        }
                    }
                    SettingsDivider()
                    IconSettingRow(
                        icon = Icons.Outlined.Lock,
                        label = "Require App Lock",
                        description = "Ask for authentication when opening the app",
                        checked = state.appLockEnabled,
                        onCheckedChange = viewModel::setAppLockEnabled
                    )
                }
            }

            item {
                SettingsSectionHeader("Data & Storage", Icons.Outlined.Storage)
                AtmCard {
                    val isRefreshing by viewModel.isRefreshingStorage.collectAsState()
                    val refreshedCount by viewModel.storageRefreshedCount.collectAsState()

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingsIconChip(Icons.Outlined.Storage)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Refresh Storage", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                when {
                                    isRefreshing -> "Reading storage for every app…"
                                    refreshedCount != null -> "Updated ${refreshedCount} apps"
                                    else -> "Fixes apps stuck showing \"Unavailable\""
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (refreshedCount != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            FilledTonalButton(onClick = {
                                viewModel.clearStorageRefreshMessage()
                                viewModel.refreshStorage()
                            }) { Text("Refresh") }
                        }
                    }
                }
            }

            item {
                SettingsSectionHeader("Backup", Icons.Outlined.CloudUpload)
                AtmCard {
                    IconSettingRow(
                        icon = Icons.Outlined.Backup,
                        label = "Automatic Backup",
                        description = "Keep a local backup up to date",
                        checked = state.autoBackupEnabled,
                        onCheckedChange = viewModel::setAutoBackupEnabled
                    )
                }
            }

            item {
                SettingsSectionHeader("General", Icons.Outlined.Description)
                AtmCard {
                    NavigationSettingRow(
                        icon = Icons.Outlined.Description,
                        label = "Reports",
                        description = "View generated monitoring reports",
                        onClick = onOpenReports
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.OfflineBolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Demoniter works completely offline. No monitoring data is uploaded automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun SettingsIconChip(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun IconSettingRow(
    icon: ImageVector,
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconChip(icon)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun NavigationSettingRow(
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconChip(icon)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsDivider() {
    Spacer(Modifier.height(6.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun ScanIntervalSelector(selected: ScanInterval, onSelect: (ScanInterval) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ScanInterval.entries.forEach { interval ->
            FilterChip(
                selected = selected == interval,
                onClick = { onSelect(interval) },
                label = { Text(intervalLabel(interval)) },
                shape = RoundedCornerShape(12.dp)
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

/**
 * Real-time install/uninstall/permission alerts rely on
 * PackageChangeReceiver waking the app process from a broadcast. Stock
 * Android's battery optimization exemption alone isn't always enough on
 * MIUI/Xiaomi devices — MIUI has its own separate "Autostart" permission
 * that can freeze the app process regardless, which is why notifications
 * would only ever show up after manually opening the app and scanning.
 * This card surfaces both toggles with their live-checked status so the
 * person can fix it once instead of hunting through OEM settings menus.
 */
@Composable
private fun ReliableNotificationsCard() {
    val context = LocalContext.current
    var batteryExempt by remember { mutableStateOf(PermissionHelper.isIgnoringBatteryOptimizations(context)) }

    // Re-check whenever the screen resumes (e.g. coming back from Settings).
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                batteryExempt = PermissionHelper.isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AtmCard {
        Text(
            "So install, uninstall, and permission alerts arrive instantly — without needing to open the app or run a scan first.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingsIconChip(Icons.Outlined.BatteryChargingFull)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Battery Optimization", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    if (batteryExempt) "Exempted — good" else "Not exempted yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (batteryExempt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            if (!batteryExempt) {
                FilledTonalButton(onClick = {
                    context.startActivity(PermissionHelper.batteryOptimizationIntent(context))
                }) { Text("Fix") }
            } else {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (PermissionHelper.isMiui()) {
            SettingsDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingsIconChip(Icons.Outlined.RocketLaunch)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("MIUI Autostart", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(
                        "Xiaomi/Redmi/POCO devices need this turned on separately, or MIUI can freeze the app in the background",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = {
                    try {
                        context.startActivity(PermissionHelper.miuiAutostartIntent())
                    } catch (e: ActivityNotFoundException) {
                        // Some MIUI versions rename/move this screen; fall back
                        // to the general app info page so the person can still
                        // navigate to autostart manually from there.
                        context.startActivity(
                            android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                            }
                        )
                    }
                }) { Text("Open") }
            }
        }
    }
}
