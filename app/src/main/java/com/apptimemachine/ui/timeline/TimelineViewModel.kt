package com.apptimemachine.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.apptimemachine.core.monitoring.MonitoringManager
import com.apptimemachine.core.utils.Formatters
import com.apptimemachine.data.entities.EventCategory
import com.apptimemachine.data.entities.ScanType
import com.apptimemachine.data.entities.TimelineEventEntity
import com.apptimemachine.data.repository.TimelineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A row the Timeline LazyColumn can render — either a real event or a day-section header. */
sealed interface TimelineListItem {
    data class Header(val label: String, val dayKey: Long) : TimelineListItem
    data class Event(val event: TimelineEventEntity) : TimelineListItem
}

/** Only the filter chips shown in the reference design; the entity still supports all 12 categories elsewhere (e.g. Search/Compare). */
val TIMELINE_FILTER_CATEGORIES = listOf(
    EventCategory.INSTALLATION,
    EventCategory.VERSION,
    EventCategory.STORAGE,
    EventCategory.USAGE
)

/** Part 3.6 Timeline Engine UI state — filter selection drives which paged query is active. */
@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val timelineRepository: TimelineRepository,
    private val monitoringManager: MonitoringManager
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<EventCategory?>(null)
    val selectedCategory: StateFlow<EventCategory?> = _selectedCategory.asStateFlow()

    // Pull-to-refresh state (Part 1.4A: no automatic background broadcast
    // reliably fires an update, so the person swipes down to trigger a
    // fresh manual scan on demand — same performScan() path the old
    // "Scan Now" button used, just driven by the gesture instead).
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                monitoringManager.performScan(ScanType.MANUAL)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    val pagedEvents: Flow<PagingData<TimelineListItem>> = _selectedCategory
        .flatMapLatest { category ->
            if (category == null) timelineRepository.pagedTimeline()
            else timelineRepository.pagedTimelineForCategory(category)
        }
        .map { pagingData ->
            pagingData
                .map { TimelineListItem.Event(it) as TimelineListItem }
                .insertSeparators { before, after ->
                    val afterEvent = (after as? TimelineListItem.Event)?.event ?: return@insertSeparators null
                    val afterDayKey = Formatters.dayKey(afterEvent.createdTimestamp)
                    val beforeDayKey = (before as? TimelineListItem.Event)?.event
                        ?.let { Formatters.dayKey(it.createdTimestamp) }
                    if (beforeDayKey == afterDayKey) null
                    else TimelineListItem.Header(Formatters.dayLabel(afterEvent.createdTimestamp), afterDayKey)
                }
        }
        .cachedIn(viewModelScope)

    fun selectCategory(category: EventCategory?) {
        _selectedCategory.value = category
    }
}
