package com.apptimemachine.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.apptimemachine.data.entities.EventCategory
import com.apptimemachine.data.entities.TimelineEventEntity
import com.apptimemachine.data.repository.TimelineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/** Part 3.6 Timeline Engine UI state — filter selection drives which paged query is active. */
@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val timelineRepository: TimelineRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<EventCategory?>(null)
    val selectedCategory: StateFlow<EventCategory?> = _selectedCategory.asStateFlow()

    val pagedEvents: Flow<PagingData<TimelineEventEntity>> = _selectedCategory
        .flatMapLatest { category ->
            if (category == null) timelineRepository.pagedTimeline()
            else timelineRepository.pagedTimelineForCategory(category)
        }
        .cachedIn(viewModelScope)

    fun selectCategory(category: EventCategory?) {
        _selectedCategory.value = category
    }
}
