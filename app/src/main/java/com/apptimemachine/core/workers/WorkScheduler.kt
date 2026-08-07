package com.apptimemachine.core.workers

import android.content.Context
import androidx.work.*
import com.apptimemachine.core.datastore.ScanInterval
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wires [AppMonitoringWorker] into WorkManager with the user's chosen
 * interval (Part 1.3 Background Scan Interval, Part 3.2 Scan Configuration).
 * Respects battery constraints per Part 4.0 Battery Targets.
 */
@Singleton
class WorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager get() = WorkManager.getInstance(context)

    fun schedulePeriodicScan(interval: ScanInterval, requireCharging: Boolean = false) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .apply { if (requireCharging) setRequiresCharging(true) }
            .build()

        val request = PeriodicWorkRequestBuilder<AppMonitoringWorker>(
            interval.minutes, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            AppMonitoringWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelPeriodicScan() {
        workManager.cancelUniqueWork(AppMonitoringWorker.WORK_NAME)
    }

    fun enqueueOneTimeScan() {
        val request = OneTimeWorkRequestBuilder<AppMonitoringWorker>().build()
        workManager.enqueueUniqueWork(
            "manual_scan_${System.currentTimeMillis()}",
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
