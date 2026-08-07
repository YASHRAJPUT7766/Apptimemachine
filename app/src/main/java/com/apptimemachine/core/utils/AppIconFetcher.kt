package com.apptimemachine.core.utils

import android.content.Context
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options

/**
 * Resolves "package:<packageName>" model strings to the app's current
 * launcher icon via PackageManager — live, never cached to a bitmap file
 * on disk (keeps Rule 1: nothing about installed apps is ever stale or
 * fabricated; if the icon changes after an app update, the next load
 * reflects it automatically).
 */
class AppIconFetcher(
    private val context: Context,
    private val packageUri: String
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        val packageName = packageUri.removePrefix("package:")
        val drawable = runCatching {
            context.packageManager.getApplicationIcon(packageName)
        }.getOrNull() ?: return null

        return DrawableResult(
            drawable = drawable,
            isSampled = false,
            dataSource = DataSource.DISK
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<String> {
        override fun create(data: String, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (!data.startsWith("package:")) return null
            return AppIconFetcher(context, data)
        }
    }
}
