package com.apptimemachine.core.monitoring

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * Centralizes every permission check used across onboarding and settings
 * (Part 3.4 Permission Setup, Part 3.2 Permissions Page) so the logic for
 * "is X actually granted" lives in exactly one place.
 */
object PermissionHelper {

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun usageAccessIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    fun hasNotificationListenerAccess(context: Context): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return context.packageName in enabledPackages
    }

    fun notificationListenerIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    /**
     * Android 13+ blocks sensitive toggles (Notification Access included)
     * with a "Restricted setting — for your security, this setting is
     * currently unavailable" dialog for any app installed from outside
     * the Play Store (sideloaded via GitHub Actions APK, Aptoide, etc.).
     * There is no programmatic way around this by design — the person has
     * to open the app's own info screen and tap the 3-dot menu → "Allow
     * restricted settings" once. This just takes them to that info screen
     * so they aren't left hunting for it after the dialog appears.
     */
    fun appInfoIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
        }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun batteryOptimizationIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
        }

    /**
     * MIUI/Xiaomi (and several other OEM skins) have their own "Autostart"
     * permission on top of stock Android's battery optimization system.
     * Without it, MIUI can freeze the app process entirely, which silently
     * blocks the PackageChangeReceiver from waking up on install/uninstall
     * even when standard battery-optimization exemption is already granted
     * — this is why real-time notifications only ever fired after opening
     * the app and running a manual scan. There's no public API for this
     * screen, so it's opened by package/component name and callers should
     * fall back to plain Settings if the launch fails (see isMiui()).
     */
    fun isMiui(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer.contains("xiaomi")
    }

    fun miuiAutostartIntent(): Intent =
        Intent().apply {
            component = android.content.ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        }
}
