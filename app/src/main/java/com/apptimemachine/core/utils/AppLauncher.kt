package com.apptimemachine.core.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.widget.Toast

/**
 * Launches another app's main/launcher activity from its package name,
 * the same way tapping its icon on the home screen would.
 *
 * Used by both the Apps list ("Open" button on each row) and App Details
 * ("Open" action in the top bar). Deliberately tolerant of failure: a row
 * in this app can legitimately point at a package that is no longer
 * launchable (background-only system component, since-disabled app,
 * uninstalled-but-not-yet-pruned row), so this never throws — it just
 * reports whether the launch happened via the return value/Toast.
 */
object AppLauncher {

    /**
     * Attempts to open [packageName]. Returns true if an activity was
     * started. On failure, shows a short Toast explaining why instead of
     * silently doing nothing, so a tap always produces visible feedback.
     */
    fun open(context: Context, packageName: String): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent == null) {
            Toast.makeText(context, "This app can't be opened", Toast.LENGTH_SHORT).show()
            return false
        }
        return try {
            context.startActivity(launchIntent)
            true
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "This app can't be opened", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
