package com.apptimemachine.core.monitoring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.apptimemachine.core.workers.AppMonitoringWorker
import dagger.hilt.android.AndroidEntryPoint

/**
 * Listens for ACTION_PACKAGE_ADDED / REMOVED / REPLACED (Part 2.2 Package
 * Change Receiver). Per spec this should "immediately notify
 * MonitoringManager" — but BroadcastReceiver.onReceive runs on the main
 * thread with a short execution budget, so heavy comparison work is
 * delegated to a lightweight one-time WorkManager job rather than done
 * inline (Part 2.2 Performance: "Heavy comparison work must be delegated
 * to WorkManager or background coroutines").
 *
 * The job is expedited (runAsExpeditedWork) so the resulting alert
 * notification fires within seconds of the install/update/removal instead
 * of waiting for normal WorkManager scheduling — this all runs purely on
 * local PackageManager broadcasts, so it fires identically whether the
 * device has mobile data, Wi-Fi, or no network connection at all.
 */
class PackageChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return

        // Ignore our own package's lifecycle broadcasts.
        if (packageName == context.packageName) return

        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                val request = OneTimeWorkRequestBuilder<AppMonitoringWorker>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
                WorkManager.getInstance(context).enqueue(request)
            }
        }
    }
}
