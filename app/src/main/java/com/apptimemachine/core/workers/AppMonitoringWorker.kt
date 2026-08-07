package com.apptimemachine.core.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.apptimemachine.core.monitoring.MonitoringManager
import com.apptimemachine.data.entities.ScanType
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Scheduled background scan (Part 1.4C Background Worker / Part 3.9
 * "Quick Scan Worker" & "Full Scan Worker" — this single worker performs a
 * full comparison pass; the interval user picks in Settings determines how
 * often it fires (Part 1.3 Background Scan Interval).
 *
 * Idempotent by design (Part 3.9): re-running it is always safe since
 * SnapshotComparator only emits events on genuine detected differences.
 */
@HiltWorker
class AppMonitoringWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val monitoringManager: MonitoringManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            monitoringManager.performScan(ScanType.AUTOMATIC)
            Result.success()
        } catch (e: Exception) {
            // Never crash the app because of a failed scan (Part 1.4C
            // Failure Handling) — retry via WorkManager's backoff policy.
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "app_monitoring_periodic_work"
    }
}
