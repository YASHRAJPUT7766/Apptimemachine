package com.apptimemachine.core.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.apptimemachine.core.monitoring.MonitoringManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Storage self-heal (see MonitoringManager.refreshAllStorage doc). Runs as
 * WorkManager rather than a plain coroutine launched from a ViewModel,
 * because the onboarding ViewModel's scope is very likely to be cleared
 * (the person navigates to the Dashboard right after setup finishes)
 * before a several-second delay would elapse — WorkManager survives that
 * navigation and even process death, so the delayed refresh reliably runs.
 */
@HiltWorker
class StorageRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val monitoringManager: MonitoringManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            monitoringManager.refreshAllStorage()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "storage_refresh_after_onboarding"
    }
}
