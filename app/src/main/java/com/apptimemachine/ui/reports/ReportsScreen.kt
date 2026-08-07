package com.apptimemachine.ui.reports

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.apptimemachine.data.entities.ExportFormat
import com.apptimemachine.ui.components.AtmCard
import com.apptimemachine.ui.components.SectionHeader

/** Part 3.1 Export Engine UI: pick a period + format, generate, then share. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: ReportsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var selectedPeriod by remember { mutableStateOf(ReportPeriod.LAST_7_DAYS) }
    var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }

    LaunchedEffect(state.lastGeneratedFile) {
        state.lastGeneratedFile?.let { file ->
            context.startActivity(
                Intent.createChooser(viewModel.shareIntentFor(file), "Share Report")
            )
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Reports") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            SectionHeader("Time Period")
            AtmCard {
                Column {
                    ReportPeriod.entries.forEach { period ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(periodLabel(period), style = MaterialTheme.typography.bodyMedium)
                            RadioButton(selected = selectedPeriod == period, onClick = { selectedPeriod = period })
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionHeader("Format")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExportFormat.entries.forEach { format ->
                    FilterChip(
                        selected = selectedFormat == format,
                        onClick = { selectedFormat = format },
                        label = { Text(format.name) }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { viewModel.generateReport(selectedPeriod, selectedFormat) },
                enabled = !state.isGenerating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isGenerating) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Generating…")
                } else {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Generate Report")
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Reports are generated entirely on-device and only shared if you choose to.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun periodLabel(period: ReportPeriod): String = when (period) {
    ReportPeriod.TODAY -> "Today"
    ReportPeriod.LAST_7_DAYS -> "Last 7 Days"
    ReportPeriod.LAST_30_DAYS -> "Last 30 Days"
    ReportPeriod.THIS_MONTH -> "This Month"
    ReportPeriod.MONITORING_LIFETIME -> "Monitoring Lifetime"
}
