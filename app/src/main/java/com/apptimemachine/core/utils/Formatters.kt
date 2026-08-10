package com.apptimemachine.core.utils

import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/** Central formatting helpers so every screen renders bytes/durations/dates identically (Part 1.4A consistency rule). */
object Formatters {

    fun bytes(value: Long?): String {
        if (value == null) return "Unavailable"
        val abs = abs(value)
        val sign = if (value < 0) "-" else ""
        return when {
            abs >= 1_073_741_824L -> "%s%.2f GB".format(sign, abs / 1_073_741_824.0)
            abs >= 1_048_576L -> "%s%.1f MB".format(sign, abs / 1_048_576.0)
            abs >= 1024L -> "%s%.0f KB".format(sign, abs / 1024.0)
            else -> "$sign$abs B"
        }
    }

    fun signedBytes(delta: Long?): String {
        if (delta == null) return "—"
        val prefix = if (delta >= 0) "+" else ""
        return prefix + bytes(delta)
    }

    fun duration(ms: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(ms)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "<1m"
        }
    }

    fun relativeTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val diff = now - timestamp
        return when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000} minutes ago"
            diff < 86_400_000 -> "${diff / 3_600_000} hours ago"
            diff < 172_800_000 -> "Yesterday"
            diff < 604_800_000 -> "${diff / 86_400_000} days ago"
            else -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timestamp))
        }
    }

    fun dateTime(timestamp: Long): String =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))

    fun time(timestamp: Long): String =
        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(timestamp))

    /**
     * Calendar-day section label for grouped lists (Timeline): "Today",
     * "Yesterday", or the actual date — matches the reference design where
     * events are grouped by real calendar day rather than a rolling
     * relative-time window.
     */
    fun dayLabel(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val eventDay = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance().apply { timeInMillis = now }
        val yesterday = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.DAY_OF_YEAR, -1) }

        return when {
            isSameDay(eventDay, today) -> "Today"
            isSameDay(eventDay, yesterday) -> "Yesterday"
            eventDay.get(Calendar.YEAR) == today.get(Calendar.YEAR) ->
                DateFormat.getDateInstance(DateFormat.LONG).format(Date(timestamp))
                    .substringBefore(", ${eventDay.get(Calendar.YEAR)}")
            else -> DateFormat.getDateInstance(DateFormat.LONG).format(Date(timestamp))
        }
    }

    /** Stable sortable key (yyyyMMdd as Long) for grouping events by calendar day. */
    fun dayKey(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return cal.get(Calendar.YEAR) * 10000L + (cal.get(Calendar.MONTH) + 1) * 100L + cal.get(Calendar.DAY_OF_MONTH)
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    /** Short weekday/date label for chart x-axes, e.g. "Mon" or "12 Aug". */
    fun shortDayLabel(timestamp: Long): String =
        DateFormat.getDateInstance(DateFormat.SHORT).format(Date(timestamp)).substringBeforeLast('/')
}
