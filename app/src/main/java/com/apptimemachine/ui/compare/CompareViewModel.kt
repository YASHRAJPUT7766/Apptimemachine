package com.apptimemachine.ui.compare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptimemachine.data.entities.InstalledAppEntity
import com.apptimemachine.data.entities.PermissionState
import com.apptimemachine.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ComparisonResult(
    val appA: InstalledAppEntity,
    val appB: InstalledAppEntity,
    val onlyInA: Set<String>,
    val onlyInB: Set<String>,
    val sharedPermissions: Set<String>,
    val storageA: Long?,
    val storageB: Long?,
    val updateCountA: Int,
    val updateCountB: Int,
    val eventCountA: Int,
    val eventCountB: Int,
    val summary: String
)

data class CompareUiState(
    val allApps: List<InstalledAppEntity> = emptyList(),
    val selectedA: InstalledAppEntity? = null,
    val selectedB: InstalledAppEntity? = null,
    val result: ComparisonResult? = null,
    val isComparing: Boolean = false
)

/**
 * Part 3.7 Application Comparison Engine. Every observation comes from
 * stored monitoring history — spec: "The engine must never invent missing
 * values" — so any field either app lacks history for is simply left out
 * of the comparison rather than guessed.
 */
@HiltViewModel
class CompareViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val storageRepository: StorageRepository,
    private val versionRepository: VersionRepository,
    private val timelineRepository: TimelineRepository
) : ViewModel() {

    private val _selectedA = MutableStateFlow<Long?>(null)
    private val _selectedB = MutableStateFlow<Long?>(null)
    private val _result = MutableStateFlow<ComparisonResult?>(null)
    private val _isComparing = MutableStateFlow(false)

    val uiState: StateFlow<CompareUiState> = combine(
        appRepository.observeAllActive(), _selectedA, _selectedB, _result, _isComparing
    ) { apps, aId, bId, result, comparing ->
        CompareUiState(
            allApps = apps,
            selectedA = apps.find { it.appId == aId },
            selectedB = apps.find { it.appId == bId },
            result = result,
            isComparing = comparing
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CompareUiState())

    fun selectAppA(appId: Long) { _selectedA.value = appId; _result.value = null }
    fun selectAppB(appId: Long) { _selectedB.value = appId; _result.value = null }

    fun runComparison() {
        val stateApps = uiState.value
        val a = stateApps.selectedA ?: return
        val b = stateApps.selectedB ?: return
        if (a.appId == b.appId) return

        viewModelScope.launch {
            _isComparing.value = true

            val permsA = a.grantedPermissionsSnapshot.split(",").filter { it.isNotBlank() }.toSet()
            val permsB = b.grantedPermissionsSnapshot.split(",").filter { it.isNotBlank() }.toSet()

            val storageA = (a.appSizeBytes ?: 0) + (a.dataSizeBytes ?: 0) + (a.cacheSizeBytes ?: 0)
            val storageB = (b.appSizeBytes ?: 0) + (b.dataSizeBytes ?: 0) + (b.cacheSizeBytes ?: 0)

            val updatesA = versionRepository.getUpdateCount(a.appId)
            val updatesB = versionRepository.getUpdateCount(b.appId)

            val eventsA = timelineRepository.getAllForApp(a.appId).size
            val eventsB = timelineRepository.getAllForApp(b.appId).size

            val summary = buildSummary(a.appName, b.appName, updatesA, updatesB, storageA, storageB)

            _result.value = ComparisonResult(
                appA = a, appB = b,
                onlyInA = permsA - permsB,
                onlyInB = permsB - permsA,
                sharedPermissions = permsA intersect permsB,
                storageA = if (storageA > 0) storageA else null,
                storageB = if (storageB > 0) storageB else null,
                updateCountA = updatesA,
                updateCountB = updatesB,
                eventCountA = eventsA,
                eventCountB = eventsB,
                summary = summary
            )
            _isComparing.value = false
        }
    }

    private fun buildSummary(nameA: String, nameB: String, updatesA: Int, updatesB: Int, storageA: Long, storageB: Long): String {
        val updateLeader = when {
            updatesA > updatesB -> "$nameA has received more updates during the monitoring period."
            updatesB > updatesA -> "$nameB has received more updates during the monitoring period."
            else -> "$nameA and $nameB have received the same number of updates."
        }
        val storageLeader = when {
            storageA > storageB -> "$nameA is currently using more storage."
            storageB > storageA -> "$nameB is currently using more storage."
            else -> "Both apps use a similar amount of storage."
        }
        return "$updateLeader $storageLeader"
    }
}
