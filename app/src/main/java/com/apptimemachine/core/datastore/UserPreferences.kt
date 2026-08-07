package com.apptimemachine.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.apptimemachine.data.entities.NotificationPrivacyMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class AppTheme { SYSTEM, LIGHT, DARK }
enum class ScanInterval(val minutes: Long) {
    FIFTEEN_MIN(15), THIRTY_MIN(30), ONE_HOUR(60),
    THREE_HOURS(180), SIX_HOURS(360), TWELVE_HOURS(720), TWENTY_FOUR_HOURS(1440)
}

/**
 * Thin typed wrapper around Jetpack DataStore (Part 3.2 Settings Engine).
 * Every read is a Flow so Compose screens recompose automatically on change,
 * and every write is async — never blocks the UI thread (Part 3.2 Performance).
 */
@Singleton
class UserPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val AMOLED_MODE = booleanPreferencesKey("amoled_mode")

        val MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
        val QUICK_SCAN_INTERVAL = stringPreferencesKey("quick_scan_interval")
        val FULL_SCAN_INTERVAL = stringPreferencesKey("full_scan_interval")
        val SCAN_ON_BOOT = booleanPreferencesKey("scan_on_boot")
        val SCAN_WHILE_CHARGING = booleanPreferencesKey("scan_while_charging")
        val RESPECT_BATTERY_SAVER = booleanPreferencesKey("respect_battery_saver")

        val NOTIFICATION_PRIVACY_MODE = stringPreferencesKey("notification_privacy_mode")
        val SUMMARY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("summary_notifications_enabled")
        val ALERTS_ENABLED = booleanPreferencesKey("alerts_enabled")

        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val BACKUP_ENCRYPTION_ENABLED = booleanPreferencesKey("backup_encryption_enabled")

        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val MONITORING_START_TIMESTAMP = longPreferencesKey("monitoring_start_timestamp")

        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
    }

    val theme: Flow<AppTheme> = dataStore.data.map {
        AppTheme.valueOf(it[Keys.THEME] ?: AppTheme.SYSTEM.name)
    }
    suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { it[Keys.THEME] = theme.name }
    }

    // Defaults to false so the app's designed indigo/purple brand palette
    // shows on first install, instead of Android's per-wallpaper Material
    // You tint (which was making Home look inconsistent/off-brand out of
    // the box). Still user-toggleable in Settings > Appearance.
    val dynamicColorEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.DYNAMIC_COLOR] ?: false }
    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    val amoledMode: Flow<Boolean> = dataStore.data.map { it[Keys.AMOLED_MODE] ?: false }
    suspend fun setAmoledMode(enabled: Boolean) {
        dataStore.edit { it[Keys.AMOLED_MODE] = enabled }
    }

    val monitoringEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.MONITORING_ENABLED] ?: false }
    suspend fun setMonitoringEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.MONITORING_ENABLED] = enabled }
    }

    val quickScanInterval: Flow<ScanInterval> = dataStore.data.map {
        ScanInterval.valueOf(it[Keys.QUICK_SCAN_INTERVAL] ?: ScanInterval.THIRTY_MIN.name)
    }
    suspend fun setQuickScanInterval(interval: ScanInterval) {
        dataStore.edit { it[Keys.QUICK_SCAN_INTERVAL] = interval.name }
    }

    val fullScanInterval: Flow<ScanInterval> = dataStore.data.map {
        ScanInterval.valueOf(it[Keys.FULL_SCAN_INTERVAL] ?: ScanInterval.SIX_HOURS.name)
    }
    suspend fun setFullScanInterval(interval: ScanInterval) {
        dataStore.edit { it[Keys.FULL_SCAN_INTERVAL] = interval.name }
    }

    val scanOnBoot: Flow<Boolean> = dataStore.data.map { it[Keys.SCAN_ON_BOOT] ?: true }
    suspend fun setScanOnBoot(enabled: Boolean) {
        dataStore.edit { it[Keys.SCAN_ON_BOOT] = enabled }
    }

    val scanWhileCharging: Flow<Boolean> = dataStore.data.map { it[Keys.SCAN_WHILE_CHARGING] ?: true }
    suspend fun setScanWhileCharging(enabled: Boolean) {
        dataStore.edit { it[Keys.SCAN_WHILE_CHARGING] = enabled }
    }

    val respectBatterySaver: Flow<Boolean> = dataStore.data.map { it[Keys.RESPECT_BATTERY_SAVER] ?: true }
    suspend fun setRespectBatterySaver(enabled: Boolean) {
        dataStore.edit { it[Keys.RESPECT_BATTERY_SAVER] = enabled }
    }

    val notificationPrivacyMode: Flow<NotificationPrivacyMode> = dataStore.data.map {
        NotificationPrivacyMode.valueOf(it[Keys.NOTIFICATION_PRIVACY_MODE] ?: NotificationPrivacyMode.METADATA_ONLY.name)
    }
    suspend fun setNotificationPrivacyMode(mode: NotificationPrivacyMode) {
        dataStore.edit { it[Keys.NOTIFICATION_PRIVACY_MODE] = mode.name }
    }

    val summaryNotificationsEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.SUMMARY_NOTIFICATIONS_ENABLED] ?: true }
    suspend fun setSummaryNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.SUMMARY_NOTIFICATIONS_ENABLED] = enabled }
    }

    val alertsEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.ALERTS_ENABLED] ?: true }
    suspend fun setAlertsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.ALERTS_ENABLED] = enabled }
    }

    val autoBackupEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.AUTO_BACKUP_ENABLED] ?: false }
    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_BACKUP_ENABLED] = enabled }
    }

    val backupEncryptionEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.BACKUP_ENCRYPTION_ENABLED] ?: false }
    suspend fun setBackupEncryptionEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.BACKUP_ENCRYPTION_ENABLED] = enabled }
    }

    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { it[Keys.ONBOARDING_COMPLETED] ?: false }
    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    val monitoringStartTimestamp: Flow<Long?> = dataStore.data.map { it[Keys.MONITORING_START_TIMESTAMP] }
    suspend fun setMonitoringStartTimestamp(timestamp: Long) {
        dataStore.edit { it[Keys.MONITORING_START_TIMESTAMP] = timestamp }
    }

    val appLockEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.APP_LOCK_ENABLED] ?: false }
    suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.APP_LOCK_ENABLED] = enabled }
    }
}
