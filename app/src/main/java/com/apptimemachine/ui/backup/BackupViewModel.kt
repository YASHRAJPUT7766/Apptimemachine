package com.apptimemachine.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptimemachine.core.backup.BackupEngine
import com.apptimemachine.core.backup.RestoreResult
import com.apptimemachine.data.entities.BackupHistoryEntity
import com.apptimemachine.data.repository.BackupHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val history: List<BackupHistoryEntity> = emptyList(),
    val isWorking: Boolean = false,
    val lastMessage: String? = null
)

/** Part 3.0 Backup & Restore Engine UI state. */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupEngine: BackupEngine,
    private val backupHistoryRepository: BackupHistoryRepository
) : ViewModel() {

    private val _isWorking = MutableStateFlow(false)
    private val _lastMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BackupUiState> = combine(
        backupHistoryRepository.observeAll(), _isWorking, _lastMessage
    ) { history, working, message ->
        BackupUiState(history = history, isWorking = working, lastMessage = message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BackupUiState())

    fun createBackup(password: String?) {
        viewModelScope.launch {
            _isWorking.value = true
            runCatching {
                val result = backupEngine.createBackup(password)
                backupHistoryRepository.insert(backupEngine.buildHistoryEntry(result, encrypted = password != null))
                _lastMessage.value = "Backup created successfully"
            }.onFailure {
                _lastMessage.value = "Backup failed: ${it.message}"
            }
            _isWorking.value = false
        }
    }

    fun restoreBackup(path: String, password: String?) {
        viewModelScope.launch {
            _isWorking.value = true
            val result = backupEngine.restoreBackup(java.io.File(path), password)
            _lastMessage.value = when (result) {
                is RestoreResult.Success -> "Restore complete. Restart the app to see restored data."
                is RestoreResult.Failed -> "Restore failed: ${result.reason}"
            }
            _isWorking.value = false
        }
    }
}
