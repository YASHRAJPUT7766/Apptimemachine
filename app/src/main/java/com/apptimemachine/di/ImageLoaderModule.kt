package com.apptimemachine.di

import android.content.Context
import coil.ImageLoader
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
            .build()
    }
}
