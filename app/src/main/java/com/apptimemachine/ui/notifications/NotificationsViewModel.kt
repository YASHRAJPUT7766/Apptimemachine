package com.apptimemachine.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptimemachine.core.utils.Formatters
import com.apptimemachine.data.dao.NotificationFeedRow
import com.apptimemachine.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One row the Notifications screen can render: a day header, a single notification, or a collapsed same-app group. */
sealed interface NotificationListItem {
    data class DayHeader(val label: String, val dayKey: Long) : NotificationListItem
    data class Single(val row: NotificationFeedRow) : NotificationListItem
    data class Group(val appName: String, val packageName: String, val iconCachePath: String?, val rows: List<NotificationFeedRow>) : NotificationListItem
}

/**
 * State backing the full-detail Dialog — shared by both a tapped group AND
 * a tapped single notification, so both open the exact same popup (just
 * pre-populated with one row in the single case) instead of two different
 * UIs for what is, from the person's point of view, the same action.
 */
data class NotificationDetailState(
    val appName: String,
    val packageName: String,
    val rows: List<NotificationFeedRow>
)

/**
 * Threshold at which same-app, same-day notifications collapse into one
 * group card instead of listing every row — mirrors how a phone's own
 * notification shade groups a busy app's notifications together.
 */
private const val GROUP_THRESHOLD = 3

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _detail = MutableStateFlow<NotificationDetailState?>(null)
    val detail: StateFlow<NotificationDetailState?> = _detail.asStateFlow()

    fun openDetail(appName: String, packageName: String, rows: List<NotificationFeedRow>) {
        _detail.value = NotificationDetailState(appName, packageName, rows)
    }

    fun closeDetail() { _detail.value = null }

    val items: StateFlow<List<NotificationListItem>> = notificationRepository.observeFeed(limit = 1000)
        .map { rows -> buildListItems(rows) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun buildListItems(rows: List<NotificationFeedRow>): List<NotificationListItem> {
        if (rows.isEmpty()) return emptyList()

        val result = mutableListOf<NotificationListItem>()
        // Rows already arrive newest-first from the DAO query. Group
        // consecutive-by-day, then within each day group consecutive-by-app
        // so a group only ever collapses notifications that are actually
        // adjacent in time from the same app — not scattered ones separated
        // by other apps' activity, which would be confusing to expand.
        val byDay = rows.groupBy { Formatters.dayKey(it.notification.postedAt) }
        val orderedDayKeys = byDay.keys.sortedDescending()

        for (dayKey in orderedDayKeys) {
            val dayRows = byDay.getValue(dayKey)
            result += NotificationListItem.DayHeader(Formatters.dayLabel(dayRows.first().notification.postedAt), dayKey)

            var index = 0
            while (index < dayRows.size) {
                val current = dayRows[index]
                var runEnd = index
                while (runEnd + 1 < dayRows.size && dayRows[runEnd + 1].packageName == current.packageName) {
                    runEnd++
                }
                val run = dayRows.subList(index, runEnd + 1)
                if (run.size >= GROUP_THRESHOLD) {
                    result += NotificationListItem.Group(current.appName, current.packageName, current.iconCachePath, run)
                } else {
                    // Below threshold: emit individually rather than as a
                    // one-item group, so a single notification never renders
                    // with "group" chrome around it.
                    run.forEach { result += NotificationListItem.Single(it) }
                }
                index = runEnd + 1
            }
        }
        return result
    }

    fun refresh() {
        // Notifications arrive live via the listener service — there is no
        // separate "notification scan" to trigger, so this is only a brief
        // pull-to-refresh affordance matching the rest of the app's screens.
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            kotlinx.coroutines.delay(400)
            _isRefreshing.value = false
        }
    }

    fun deleteOne(id: Long) = viewModelScope.launch {
        notificationRepository.delete(id)
    }

    fun deleteAll(rows: List<NotificationFeedRow>) = viewModelScope.launch {
        notificationRepository.deleteBatch(rows.map { it.notification.notificationHistoryId })
        closeDetail()
    }
}
