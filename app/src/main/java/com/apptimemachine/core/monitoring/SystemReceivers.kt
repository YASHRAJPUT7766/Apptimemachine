package com.apptimemachine.core.monitoring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.apptimemachine.core.workers.AppMonitoringWorker
import com.apptimemachine.data.entities.ChargingMethod

/**
 * Resumes monitoring after reboot if the user enabled Auto Resume
 * (Part 1.3 Boot Completed / Part 3.4 onboarding "Run Scan After Device
 * Boot" toggle). WorkManager's own periodic work survives reboot on its
 * own, but a boot-triggered one-time scan gives an immediate refresh and
 * lets us log a distinct BOOT scan-type event.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val request = OneTimeWorkRequestBuilder<AppMonitoringWorker>().build()
        WorkManager.getInstance(context).enqueue(request)
    }
}

/**
 * Shared helper used by [BatteryMonitor] to classify the charging method
 * from a battery-changed Intent's extras.
 */
internal fun Intent.chargingMethodFromExtras(): ChargingMethod {
    val plugged = getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
    return when (plugged) {
        BatteryManager.BATTERY_PLUGGED_AC -> ChargingMethod.AC
        BatteryManager.BATTERY_PLUGGED_USB -> ChargingMethod.USB
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargingMethod.WIRELESS
        else -> ChargingMethod.UNKNOWN
    }
}
