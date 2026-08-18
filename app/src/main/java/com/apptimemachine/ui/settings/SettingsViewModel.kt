package com.apptimemachine.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptimemachine.core.datastore.AppTheme
import com.apptimemachine.core.datastore.ScanInterval
import com.apptimemachine.core.datastore.UserPreferences
import com.apptimemachine.core.monitoring.MonitoringManager
import com.apptimemachine.core.workers.WorkScheduler
import com.apptimemachine.data.entities.NotificationPrivacyMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val theme: AppTheme = AppTheme.SYSTEM,
    // Matches the real DataStore default (UserPreferences.dynamicColorEnabled
    // falls back to false) so the Settings toggle doesn't flash "on" for a
    // frame before flipping to "off" once the real value loads.
    val dynamicColor: Boolean = false,
    val amoledMode: Boolean = false,
    val monitoringEnabled: Boolean = false,
    val quickScanInterval: ScanInterval = ScanInterval.THIRTY_MIN,
    val fullScanInterval: ScanInterval = ScanInterval.SIX_HOURS,
    val scanOnBoot: Boolean = true,
    val scanWhileCharging: Boolean = true,
    val notificationPrivacyMode: NotificationPrivacyMode = NotificationPrivacyMode.METADATA_ONLY,
    val autoBackupEnabled: Boolean = false,
    val appLockEnabled: Boolean = false
)

/** Part 3.2 Settings Engine — every toggle writes through UserPreferences (DataStore), takes effect immediately. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val workScheduler: WorkScheduler,
    private val monitoringManager: MonitoringManager
) : ViewModel() {

    private val _isRefreshingStorage = MutableStateFlow(false)
    val isRefreshingStorage: StateFlow<Boolean> = _isRefreshingStorage.asStateFlow()

    private val _storageRefreshedCount = MutableStateFlow<Int?>(null)
    val storageRefreshedCount: StateFlow<Int?> = _storageRefreshedCount.asStateFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        userPreferences.theme,
        userPreferences.dynamicColorEnabled,
        userPreferences.amoledMode,
        userPreferences.monitoringEnabled,
        userPreferences.quickScanInterval,
        userPreferences.fullScanInterval,
        userPreferences.scanOnBoot,
        userPreferences.scanWhileCharging,
        userPreferences.notificationPrivacyMode,
        userPreferences.autoBackupEnabled,
        userPreferences.appLockEnabled
    ) { values ->
        SettingsUiState(
            theme = values[0] as AppTheme,
            dynamicColor = values[1] as Boolean,
            amoledMode = values[2] as Boolean,
            monitoringEnabled = values[3] as Boolean,
            quickScanInterval = values[4] as ScanInterval,
            fullScanInterval = values[5] as ScanInterval,
            scanOnBoot = values[6] as Boolean,
            scanWhileCharging = values[7] as Boolean,
            notificationPrivacyMode = values[8] as NotificationPrivacyMode,
            autoBackupEnabled = values[9] as Boolean,
            appLockEnabled = values[10] as Boolean
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setTheme(theme: AppTheme) = viewModelScope.launch { userPreferences.setTheme(theme) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { userPreferences.setDynamicColorEnabled(enabled) }
    fun setAmoledMode(enabled: Boolean) = viewModelScope.launch { userPreferences.setAmoledMode(enabled) }

    fun setQuickScanInterval(interval: ScanInterval) = viewModelScope.launch {
        userPreferences.setQuickScanInterval(interval)
        workScheduler.schedulePeriodicScan(interval, uiState.value.scanWhileCharging)
    }

    fun setScanOnBoot(enabled: Boolean) = viewModelScope.launch { userPreferences.setScanOnBoot(enabled) }
    fun setScanWhileCharging(enabled: Boolean) = viewModelScope.launch { userPreferences.setScanWhileCharging(enabled) }

    fun setNotificationPrivacyMode(mode: NotificationPrivacyMode) = viewModelScope.launch {
        userPreferences.setNotificationPrivacyMode(mode)
    }

    fun setAutoBackupEnabled(enabled: Boolean) = viewModelScope.launch { userPreferences.setAutoBackupEnabled(enabled) }
    fun setAppLockEnabled(enabled: Boolean) = viewModelScope.launch { userPreferences.setAppLockEnabled(enabled) }

    /**
     * Manual "Refresh Storage" action (Storage fix): re-reads every app's
     * storage size right now, unconditionally — see
     * MonitoringManager.refreshAllStorage() doc for why this exists
     * alongside the normal scan cycle.
     */
    fun refreshStorage() = viewModelScope.launch {
        _isRefreshingStorage.value = true
        val result = monitoringManager.refreshAllStorage()
        _storageRefreshedCount.value = result.appsScanned
        _isRefreshingStorage.value = false
    }

    fun clearStorageRefreshMessage() {
        _storageRefreshedCount.value = null
    }
}
