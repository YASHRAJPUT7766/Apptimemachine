package com.apptimemachine.core.database

import androidx.room.TypeConverter
import com.apptimemachine.data.entities.*

/**
 * Room can't persist enums natively — store as their name() string.
 * Kept centralized so every entity file above can use enums directly
 * without repeating converter boilerplate.
 */
class Converters {

    @TypeConverter
    fun fromEventCategory(value: EventCategory): String = value.name
    @TypeConverter
    fun toEventCategory(value: String): EventCategory = EventCategory.valueOf(value)

    @TypeConverter
    fun fromEventSeverity(value: EventSeverity): String = value.name
    @TypeConverter
    fun toEventSeverity(value: String): EventSeverity = EventSeverity.valueOf(value)

    @TypeConverter
    fun fromScanType(value: ScanType): String = value.name
    @TypeConverter
    fun toScanType(value: String): ScanType = ScanType.valueOf(value)

    @TypeConverter
    fun fromVersionChangeType(value: VersionChangeType): String = value.name
    @TypeConverter
    fun toVersionChangeType(value: String): VersionChangeType = VersionChangeType.valueOf(value)

    @TypeConverter
    fun fromPermissionState(value: PermissionState): String = value.name
    @TypeConverter
    fun toPermissionState(value: String): PermissionState = PermissionState.valueOf(value)

    @TypeConverter
    fun fromPermissionSensitivity(value: PermissionSensitivity): String = value.name
    @TypeConverter
    fun toPermissionSensitivity(value: String): PermissionSensitivity = PermissionSensitivity.valueOf(value)

    @TypeConverter
    fun fromNotificationEventType(value: NotificationEventType): String = value.name
    @TypeConverter
    fun toNotificationEventType(value: String): NotificationEventType = NotificationEventType.valueOf(value)

    @TypeConverter
    fun fromNotificationPrivacyMode(value: NotificationPrivacyMode): String = value.name
    @TypeConverter
    fun toNotificationPrivacyMode(value: String): NotificationPrivacyMode = NotificationPrivacyMode.valueOf(value)

    @TypeConverter
    fun fromBatteryEventType(value: BatteryEventType): String = value.name
    @TypeConverter
    fun toBatteryEventType(value: String): BatteryEventType = BatteryEventType.valueOf(value)

    @TypeConverter
    fun fromChargingMethod(value: ChargingMethod?): String? = value?.name
    @TypeConverter
    fun toChargingMethod(value: String?): ChargingMethod? = value?.let { ChargingMethod.valueOf(it) }

    @TypeConverter
    fun fromExportFormat(value: ExportFormat): String = value.name
    @TypeConverter
    fun toExportFormat(value: String): ExportFormat = ExportFormat.valueOf(value)

    @TypeConverter
    fun fromExportStatus(value: ExportStatus): String = value.name
    @TypeConverter
    fun toExportStatus(value: String): ExportStatus = ExportStatus.valueOf(value)

    @TypeConverter
    fun fromBackupStatus(value: BackupStatus): String = value.name
    @TypeConverter
    fun toBackupStatus(value: String): BackupStatus = BackupStatus.valueOf(value)

    @TypeConverter
    fun fromReportType(value: ReportType): String = value.name
    @TypeConverter
    fun toReportType(value: String): ReportType = ReportType.valueOf(value)

    @TypeConverter
    fun fromInsightPriority(value: InsightPriority): String = value.name
    @TypeConverter
    fun toInsightPriority(value: String): InsightPriority = InsightPriority.valueOf(value)
}
