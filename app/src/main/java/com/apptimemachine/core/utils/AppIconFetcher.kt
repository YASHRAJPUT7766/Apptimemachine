package com.apptimemachine.core.utils

import android.content.Context
import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options

/**
 * Resolves "package:<packageName>" model Uris to the app's current
 * launcher icon via PackageManager — live, never cached to a bitmap file
 * on disk (keeps Rule 1: nothing about installed apps is ever stale or
 * fabricated; if the icon changes after an app update, the next load
 * reflects it automatically).
 *
 * IMPORTANT: this is registered as Fetcher.Factory<Uri>, not <String>.
 * Coil's built-in StringMapper intercepts every String model that looks
 * like "scheme:something" and converts it to an android.net.Uri *before*
 * any Fetcher.Factory is consulted. "package:<name>" matches that pattern,
 * so it was always being turned into a Uri first — a Factory<String> here
 * never actually got called, Coil fell through to "no fetcher found", and
 * every single icon rendered as the blank fallback. Matching the Uri type
 * that Coil actually hands us is what makes the fetcher run at all.
 */
class AppIconFetcher(
    private val context: Context,
    private val packageUri: Uri
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        val packageName = packageUri.schemeSpecificPart
        val drawable = runCatching {
            context.packageManager.getApplicationIcon(packageName)
        }.getOrNull() ?: return null

        return DrawableResult(
            drawable = drawable,
            isSampled = false,
            dataSource = DataSource.DISK
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme != "package") return null
            return AppIconFetcher(context, data)
        }
    }
}
