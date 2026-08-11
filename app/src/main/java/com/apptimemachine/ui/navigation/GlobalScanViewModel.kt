package com.apptimemachine.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptimemachine.core.monitoring.MonitoringManager
import com.apptimemachine.data.entities.ScanType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the persistent "Scan Now" bar rendered by [AppNavigation] above the
 * bottom nav on every screen (Dashboard, Timeline, Apps, Settings, and any
 * pushed screen), not just Dashboard's own Quick Actions. Scoped to the
 * Activity (created once, outside the NavHost) so scanning state and the
 * result message survive navigating between screens instead of resetting.
 *
 * A scan here runs through the same [MonitoringManager.performScan] used
 * everywhere else, writing real rows to Room — every screen observing that
 * data (Dashboard counts, Timeline, Apps list) updates live via Flow once
 * it finishes, with no separate "refresh" step needed.
 */
@HiltViewModel
class GlobalScanViewModel @Inject constructor(
    private val monitoringManager: MonitoringManager
) : ViewModel() {

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _resultMessage = MutableStateFlow<String?>(null)
    val resultMessage: StateFlow<String?> = _resultMessage.asStateFlow()

    fun runManualScan() {
        if (_isScanning.value) return
        viewModelScope.launch {
            _isScanning.value = true
            val outcome = runCatching { monitoringManager.performScan(ScanType.MANUAL) }
            _isScanning.value = false
            _resultMessage.value = outcome.fold(
                onSuccess = { result ->
                    when {
                        result.errorCount > 0 && result.eventsGenerated == 0 ->
                            "Scan finished with ${result.errorCount} error(s)"
                        result.eventsGenerated == 0 ->
                            "Scan complete — no changes found across ${result.appsScanned} apps"
                        else ->
                            "Scan complete — ${result.eventsGenerated} change" +
                                (if (result.eventsGenerated == 1) "" else "s") +
                                " found across ${result.appsScanned} apps"
                    }
                },
                onFailure = { "Scan failed — please try again" }
            )
        }
    }

    fun consumeResultMessage() {
        _resultMessage.value = null
    }
}
