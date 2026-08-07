package com.apptimemachine

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.apptimemachine.core.monitoring.BatteryMonitor
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point.
 *
 * Rule 5 (spec): the application works completely offline — no network
 * initialization happens here. Rule 6: no root, no hidden APIs, no
 * reflection-based package access — only what Hilt/WorkManager/Room provide.
 */
@HiltAndroidApp
class AppTimeMachineApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var batteryMonitor: BatteryMonitor

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Dynamic registration (not manifest-declared) so BatteryMonitor
        // can be a proper Hilt @Singleton with an injected repository
        // (Part 2.4 Battery Monitoring Engine).
        batteryMonitor.start()
    }
}
