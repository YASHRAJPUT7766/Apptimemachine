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
 * Threshold at which same-app, same-day notifications collapse into one
 * group card instead of listing every row — mirrors how a phone's own
 * notification shade groups a busy app's notifications together (Part:
 * discussed as "if WhatsApp sends 3+, show one card that expands").
 */
private const val GROUP_THRESHOLD = 3

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /**
     * Bottom sheet state: which group (if any) is currently expanded to
     * show every individual notification inside it, in full detail.
     */
    private val _expandedGroup = MutableStateFlow<NotificationListItem.Group?>(null)
    val expandedGroup: StateFlow<NotificationListItem.Group?> = _expandedGroup.asStateFlow()

    fun openGroup(group: NotificationListItem.Group) { _expandedGroup.value = group }
    fun closeGroup() { _expandedGroup.value = null }

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
                result += if (run.size >= GROUP_THRESHOLD) {
                    NotificationListItem.Group(current.appName, current.packageName, current.iconCachePath, run)
                } else {
                    // Below threshold: emit individually rather than as a
                    // one-item group, so a single notification never renders
                    // with "group" chrome around it.
                    run.forEach { result += NotificationListItem.Single(it) }
                    index = runEnd + 1
                    continue
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

    fun delete(id: Long) = viewModelScope.launch {
        notificationRepository.delete(id)
    }

    fun deleteGroup(rows: List<NotificationFeedRow>) = viewModelScope.launch {
        notificationRepository.deleteBatch(rows.map { it.notification.notificationHistoryId })
        closeGroup()
    }
}
