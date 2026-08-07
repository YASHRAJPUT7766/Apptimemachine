# Part 4.0 Release Checklist: no debug logging/menus in release; keep Room,
# Hilt, and data classes intact so the shrinker doesn't break reflection-based
# wiring or database schema hashing.

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager

# Kotlin coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Data entities (Room reflectively constructs these)
-keep class com.apptimemachine.data.entities.** { *; }

# WorkManager
-keep class androidx.work.impl.WorkManagerInitializer
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker { <init>(...); }

# Coil — custom Fetcher.Factory resolves "package:<name>" model strings via
# runtime type matching. Without this, R8 full mode obfuscates/strips
# AppIconFetcher's structure and icon loading silently fails in release
# builds only (Apps list / Timeline / Search / App Details all show the
# fallback glyph instead of the real icon).
-keep class com.apptimemachine.core.utils.AppIconFetcher { *; }
-keep class com.apptimemachine.core.utils.AppIconFetcher$Factory { *; }
-keep class coil.fetch.Fetcher { *; }
-keep interface coil.fetch.Fetcher$Factory { *; }
-keep class * implements coil.fetch.Fetcher$Factory
