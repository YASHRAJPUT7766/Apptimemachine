package com.apptimemachine.ui.reports

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptimemachine.core.export.ExportEngine
import com.apptimemachine.data.entities.ExportFormat
import com.apptimemachine.data.entities.TimelineEventEntity
import com.apptimemachine.data.repository.ExportHistoryRepository
import com.apptimemachine.data.repository.TimelineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

enum class ReportPeriod { TODAY, LAST_7_DAYS, LAST_30_DAYS, THIS_MONTH, MONITORING_LIFETIME }

data class ReportsUiState(
    val isGenerating: Boolean = false,
    val lastGeneratedFile: File? = null,
    val lastFormat: ExportFormat? = null
)

/** Part 3.1 Export Engine UI state — picks a date range, generates a file, exposes it for sharing. */
@HiltViewModel
class ReportsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timelineRepository: TimelineRepository,
    private val exportEngine: ExportEngine,
    private val exportHistoryRepository: ExportHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    fun generateReport(period: ReportPeriod, format: ExportFormat) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true)

            val (start, end) = periodRange(period)
            val allEvents = withContext(Dispatchers.IO) {
                // Covers all apps for the period (Part 3.1 "Entire Database" / date range export).
                timelineRepository.getEventsBetween(start, end)
            }

            val file = withContext(Dispatchers.IO) {
                when (format) {
                    ExportFormat.CSV -> exportEngine.exportTimelineCsv(allEvents, period.label())
                    ExportFormat.JSON -> exportEngine.exportTimelineJson(allEvents, start, end, period.label())
                    ExportFormat.PDF -> exportEngine.exportTimelinePdf(allEvents, start, end, period.label())
                }
            }

            exportHistoryRepository.insert(
                exportEngine.buildExportHistoryEntry(file, period.label(), format, allEvents.size)
            )

            _uiState.value = ReportsUiState(isGenerating = false, lastGeneratedFile = file, lastFormat = format)
        }
    }

    fun shareIntentFor(file: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = when {
                file.extension == "pdf" -> "application/pdf"
                file.extension == "csv" -> "text/csv"
                else -> "application/json"
            }
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun periodRange(period: ReportPeriod): Pair<Long, Long> {
        val zone = ZoneOffset.systemDefault()
        val now = System.currentTimeMillis()
        val today = LocalDate.now(zone)
        val start = when (period) {
            ReportPeriod.TODAY -> today.atStartOfDay(zone).toInstant().toEpochMilli()
            ReportPeriod.LAST_7_DAYS -> today.minusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()
            ReportPeriod.LAST_30_DAYS -> today.minusDays(30).atStartOfDay(zone).toInstant().toEpochMilli()
            ReportPeriod.THIS_MONTH -> today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
            ReportPeriod.MONITORING_LIFETIME -> 0L
        }
        return start to now
    }

    private fun ReportPeriod.label(): String = when (this) {
        ReportPeriod.TODAY -> "Daily"
        ReportPeriod.LAST_7_DAYS -> "Weekly"
        ReportPeriod.LAST_30_DAYS -> "Monthly"
        ReportPeriod.THIS_MONTH -> "MonthToDate"
        ReportPeriod.MONITORING_LIFETIME -> "Lifetime"
    }
}
