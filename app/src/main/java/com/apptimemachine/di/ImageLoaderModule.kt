package com.apptimemachine.di

import android.content.Context
import coil.ImageLoader
import coil.memory.MemoryCache
import coil.util.DebugLogger
import com.apptimemachine.BuildConfig
import com.apptimemachine.core.utils.AppIconFetcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ImageLoaderModule {

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .components { add(AppIconFetcher.Factory(context)) }
            // App icons don't change on every launch, so a healthy in-memory
            // cache (25% of available app memory) is what keeps Apps list /
            // Timeline scrolling smooth — icons are decoded once per package
            // and reused, instead of hitting PackageManager on every
            // recomposition while scrolling.
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            // Icons are re-derived live from PackageManager (never stale per
            // spec Rule 1), so we skip Coil's disk cache for them — memory
            // cache alone is enough since the fetcher itself is fast.
            .apply { if (BuildConfig.DEBUG) logger(DebugLogger()) }
            .build()
    }
}
