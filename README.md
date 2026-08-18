# App Time Machine

Android app that builds a permanent timeline of installed-app changes —
version updates, storage growth, permission changes, usage, notifications,
and battery/network activity — starting from the moment monitoring begins.
Built per the attached Master Specification v1.0.

## Stack
Kotlin · Jetpack Compose · Material 3 · MVVM · Room · Hilt · WorkManager ·
DataStore · Coroutines/Flow · Paging 3 · Coil. Min SDK 26, offline-first,
no root/hidden APIs.

## Opening the project
1. Open this folder in **Android Studio (Koala/2024.1 or newer)**.
2. Android Studio will offer to generate the Gradle wrapper automatically
   on first sync (this environment had no network access to pre-download
   `gradle-wrapper.jar`, so it isn't bundled) — accept that prompt, or run
   `gradle wrapper --gradle-version 8.7` once if you have Gradle installed
   locally.
3. Let Gradle sync — it will pull all dependencies listed in
   `app/build.gradle.kts` (Room, Hilt, Compose, WorkManager, etc.).
4. Run on a device/emulator with API 26+.

## What's implemented
- **Data layer**: all 18 Room entities from the spec (installed apps,
  timeline events, storage/usage/version/permission/notification/battery/
  network history, scan/export/backup history, reports, bookmarks,
  searches, insights) with DAOs, TypeConverters, and a versioned Database.
- **Monitoring Engine**: `PackageInfoReader` / `StorageStatsReader` /
  `UsageStatsReader` / `NetworkStatsReader` (official APIs only, per Rule 6)
  feed `SnapshotComparator` (pure diff logic, unit-testable) orchestrated
  by `MonitoringManager` — now including network stats in the scan cycle.
  Wired into `AppMonitoringWorker` (WorkManager, Hilt-injected) plus
  `PackageChangeReceiver` / `BootCompletedReceiver` for realtime +
  boot-triggered scans, and `BatteryMonitor` (dynamically-registered Hilt
  singleton) for charging session persistence.
- **Notification Listener**: privacy-mode enforced at the point content is
  captured, not just on export.
- **Export Engine**: CSV, JSON, and PDF report generation — PDF built with
  Android's native `android.graphics.pdf.PdfDocument` (no third-party PDF
  library), shared via a scoped `FileProvider`.
- **Backup & Restore Engine**: raw database file backup with optional
  AES/CBC password-based encryption (key derived via SHA-256, never
  persisted), checksum validation, and a Backup screen with history.
- **Statistics Engine**: custom Compose `Canvas`-based line/bar charts and
  a calendar heatmap (no external charting dependency) — 7-day activity,
  30-day heatmap, and derived highlights (most-changed app, fastest
  storage growth, weekly update count).
- **Search Engine**: real-time debounced search across installed apps with
  a capped recent-searches list (Part 2.9).
- **Comparison Engine**: side-by-side app comparison — storage, version
  update count, timeline event count, and permission set differences
  (only-in-A / only-in-B / shared), with a plain-language summary.
- **Repositories**: one per domain (App, Timeline, Storage, Usage, Version,
  Permission, Notification, Battery, Network, Scan, Export, Backup,
  Report, Search), all Flow-based.
- **UI**: Dashboard (with Quick Actions), Timeline (Paging 3, filterable),
  Apps list (search + favorites), App Details (5 tabs), Settings, Reports,
  Backup, Statistics, Search, Compare, and a 4-page Onboarding flow —
  Material 3 with dynamic color + AMOLED mode support. Bottom nav: five
  tabs (Dashboard/Timeline/Apps/Reports/Settings) per Part 1.2; Search,
  Statistics, Compare, and Backup are reachable from Dashboard.

## What's stubbed / next steps
The spec is extremely large (4.0 parts, dozens of modules). What remains
thin or not yet built:
- **App lock (biometric)**: dependency included, not wired to a gate.
- **Restore flow's app restart**: `BackupEngine.restoreBackup()` writes the
  new database file but doesn't yet trigger a guided "restart the app"
  flow beyond the Snackbar message — Room's live connection to the old
  file needs to be closed first in a production build (documented inline).
- **Export/Report history UI polish**: `ExportHistoryRepository` and
  `ReportRepository` exist and are wired into the Reports flow's history
  logging, but there's no dedicated "past exports" browsing screen yet.
- **Saved searches UI**: the repository/DAO support saved (named) queries
  (Part 2.9), but only recent searches are surfaced in the Search screen.
- **NetworkStatsManager subscriberId**: mobile-data queries pass `null`
  for `subscriberId`, which works on most modern devices without
  `READ_PHONE_STATE`, but some OEM/API-level combinations may need it —
  falls back to "unavailable" rather than crashing either way.

## Notes on fidelity to the spec
- Rule 1–2 (never fabricate, no history before monitoring): enforced
  throughout — every reader returns `null`/empty on missing data rather
  than estimating, and the initial scan writes baseline snapshots with
  zero timeline events.
- Rule 6 (no root/hidden APIs): only `PackageManager`,
  `StorageStatsManager`, `UsageStatsManager`, `NetworkStatsManager`,
  `BatteryManager`, and `NotificationListenerService` are used.
- Offline-first: `network_security_config.xml` disables cleartext traffic
  entirely; there is no networking code anywhere in the app.

## Changelog: deep monitoring pass (Notifications, Battery, Storage fix)

### Storage "Unavailable" — root cause and fix
Some apps stayed permanently "Unavailable" for storage size after
onboarding. Root cause: `performInitialScan()` ran immediately after the
onboarding flow, sometimes in the same moment Usage Access was granted —
`AppOpsManager` can take a brief window to report the new grant, so every
app got baselined with `null` storage. Nothing forced a full re-read
afterward, so it only healed opportunistically. Fixed by:
- `MonitoringManager.refreshAllStorage()` — an unconditional (not
  diff-gated) re-read of every app's storage.
- `StorageRefreshWorker`, scheduled 5s after onboarding completes via
  WorkManager (not a plain delayed coroutine, since the onboarding
  ViewModel is cleared almost immediately once the person reaches the
  Dashboard).
- A manual "Refresh Storage" button in Settings > Data & Storage, for the
  same fix on demand.

### Notification log — permanent, with OTP handling and actions
Notifications now write both to the existing detailed
`NotificationHistoryEntity` log AND a lightweight `TimelineEventEntity`
(`EventCategory.NOTIFICATIONS`), so they show up in Timeline's "All" feed,
a new "Notifications" filter chip, and the Dashboard's Recent Activity —
previously notifications were captured but never surfaced in any of
those. Each entry supports "Open app" (via the existing `AppLauncher`) and
"Delete" (removes the entry from this in-app log only; does not touch the
notification on the device).

OTP detection (`OtpDetector.kt`) runs at capture time, before the
notification's privacy mode is even applied: if a notification looks like
a one-time code, its title/body are never stored at all — only an
`isOtp` flag — regardless of which `NotificationPrivacyMode` is active.
The UI shows "OTP received" for these.

### Battery per-app — real API is system-only; replaced with an honest proxy
Investigated `BatteryStatsManager.getBatteryUsageStats()`, the API that
reports real per-app battery %. It's marked `@SystemApi`/`@hide` and
requires the signature-level `BATTERY_STATS` permission — not obtainable
by a third-party app on a non-rooted device, regardless of what the user
grants. Rather than build on a hidden API (against Rule 6) or fabricate
numbers, `BatteryUsageEntity` stores a **usage-time-derived proxy**
(each app's share of total foreground time that day), explicitly labeled
"Estimated" everywhere it's shown, alongside the real device-level
battery drop for context. See the entity's doc comment for the full
reasoning.

### Permission "last active" — dropped; not buildable without root
Investigated `AppOpsManager.getPackagesForOps()` /
`startWatchingActive()` for "which permission did app X last use, and
when" across all installed apps. Both require signature-level permissions
(`GET_APP_OPS_STATS`, `WATCH_APPOPS`) reserved for system apps — this is
the same restriction that stops any regular third-party app from building
a real "who used the camera" dashboard for other apps; it's not a gap
that more user-granted permissions can close. This feature was dropped
entirely rather than shipped with fake or silently-broken data.
