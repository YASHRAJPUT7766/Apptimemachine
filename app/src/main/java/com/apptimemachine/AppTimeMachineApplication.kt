package com.apptimemachine

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
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
class AppTimeMachineApplication : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var batteryMonitor: BatteryMonitor

    @Inject
    lateinit var imageLoader: ImageLoader

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    // Without this override, AsyncImage() everywhere falls back to Coil's
    // default global ImageLoader, which has no idea how to resolve
    // "package:<name>" model strings — that's why every app icon (Apps
    // list, Timeline, everywhere) was rendering blank. Returning our
    // Hilt-provided ImageLoader (registered with AppIconFetcher.Factory in
    // ImageLoaderModule) here makes it the actual default, and its
    // built-in memory+disk cache is what keeps icon-heavy list scrolling
    // smooth instead of re-reading each icon from PackageManager per frame.
    override fun newImageLoader(): ImageLoader = imageLoader

    override fun onCreate() {
        super.onCreate()
        // Dynamic registration (not manifest-declared) so BatteryMonitor
        // can be a proper Hilt @Singleton with an injected repository
        // (Part 2.4 Battery Monitoring Engine).
        batteryMonitor.start()
    }
}
