package com.apptimemachine.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apptimemachine.core.utils.Formatters
import com.apptimemachine.data.model.AppNetworkStat
import com.apptimemachine.data.model.AppShareStat

private val CARD_PALETTE = listOf(
    Color(0xFF7C4DFF), Color(0xFF00BFA5), Color(0xFFFF6D00), Color(0xFFD500F9),
    Color(0xFF2979FF), Color(0xFFFFAB00), Color(0xFF00C853), Color(0xFFFF1744)
)

/**
 * Battery-drain PROXY card — labeled explicitly as an estimate derived
 * from usage time, never presented as measured battery %. See
 * [com.apptimemachine.data.entities.BatteryUsageEntity] doc for why a
 * real per-app battery percentage isn't obtainable on a non-rooted
 * device: the platform API that reports it is restricted to system apps.
 *
 * Shared by Dashboard (all apps today), Timeline (fixed header card),
 * and App Details (single app's history) — same card, different data.
 */
@Composable
fun BatteryDrainCard(apps: List<AppShareStat>, deviceDropPercent: Int?, modifier: Modifier = Modifier) {
    AtmCard(modifier = modifier) {
        Text("Battery Drain (Estimated)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "Estimated from screen-on time per app — Android doesn't expose exact per-app battery % to apps like this one.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))

        if (apps.isEmpty()) {
            Text(
                "No usage recorded yet today",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val slices = apps.mapIndexed { i, a -> a.sharePercent to CARD_PALETTE[i % CARD_PALETTE.size] }
                DonutChart(
                    slices = slices,
                    modifier = Modifier.size(96.dp),
                    strokeWidthDp = 16.dp,
                    centerContent = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                apps.firstOrNull()?.let { "${it.sharePercent.toInt()}%" } ?: "—",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text("top app", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    apps.take(4).forEachIndexed { i, app ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(CARD_PALETTE[i % CARD_PALETTE.size], shape = CircleShape)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(app.appName, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                            Text(
                                "${app.sharePercent.toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (deviceDropPercent != null) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(Modifier.height(10.dp))
                Text(
                    "Phone battery dropped $deviceDropPercent% today (while unplugged)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Today's per-app network usage — real data via NetworkStatsManager,
 * wifi + mobile split. Shared by Dashboard, Timeline, and App Details.
 */
@Composable
fun NetworkUsageCard(apps: List<AppNetworkStat>, wifiTotalBytes: Long, mobileTotalBytes: Long, modifier: Modifier = Modifier) {
    AtmCard(modifier = modifier) {
        Text("Network Usage Today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Row {
            Text(
                "Wi-Fi: ${Formatters.bytes(wifiTotalBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(16.dp))
            Text(
                "Mobile: ${Formatters.bytes(mobileTotalBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(14.dp))

        if (apps.isEmpty()) {
            Text(
                "No network activity recorded yet today",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val maxBytes = apps.maxOf { it.totalBytes }.coerceAtLeast(1)
            apps.forEach { app ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon(packageName = app.packageName, size = 28.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(app.appName, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        Spacer(Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress = { app.totalBytes.toFloat() / maxBytes.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            strokeCap = StrokeCap.Round
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        Formatters.bytes(app.totalBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
