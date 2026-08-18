package com.apptimemachine.data.model

/** Shared across Dashboard, Timeline, and App Details — one app's share of today's battery-drain proxy. */
data class AppShareStat(val appName: String, val packageName: String, val sharePercent: Float, val foregroundMs: Long)

/** Shared across Dashboard, Timeline, and App Details — one app's network usage for a day. */
data class AppNetworkStat(val appName: String, val packageName: String, val totalBytes: Long)
