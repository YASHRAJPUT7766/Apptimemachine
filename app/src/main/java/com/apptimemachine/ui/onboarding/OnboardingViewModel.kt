package com.apptimemachine.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptimemachine.core.datastore.UserPreferences
import com.apptimemachine.core.monitoring.MonitoringManager
import com.apptimemachine.core.workers.WorkScheduler
import com.apptimemachine.core.datastore.ScanInterval
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Part 3.4 Onboarding — runs the initial device snapshot (Part 1.1) once
 * the user completes setup, then marks onboarding complete so it's never
 * shown again.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val monitoringManager: MonitoringManager,
    private val userPreferences: UserPreferences,
    private val workScheduler: WorkScheduler
) : ViewModel() {

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanComplete = MutableStateFlow(false)
    val scanComplete: StateFlow<Boolean> = _scanComplete.asStateFlow()

    private val _appsDetected = MutableStateFlow(0)
    val appsDetected: StateFlow<Int> = _appsDetected.asStateFlow()

    fun completeSetup() {
        viewModelScope.launch {
            _isScanning.value = true
            val result = monitoringManager.performInitialScan()
            _appsDetected.value = result.appsScanned
            workScheduler.schedulePeriodicScan(ScanInterval.THIRTY_MIN)
            userPreferences.setOnboardingCompleted(true)

            // Self-heal storage: if Usage Access was granted moments
            // before the scan above ran, AppOpsManager can occasionally
            // still report the old (not-yet-granted) mode for a brief
            // window right after the person returns from the Settings
            // toggle — enough to baseline every app's storage as null on
            // this very first scan. Scheduled via WorkManager rather than
            // a plain delayed coroutine (see StorageRefreshWorker doc) so
            // it still runs even though this ViewModel is about to be
            // cleared when the person lands on the Dashboard.
            workScheduler.enqueueDelayedStorageRefresh()

            _isScanning.value = false
            _scanComplete.value = true
        }
    }
}
