# Changelog

All notable changes are documented here. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), versioning: [SemVer](https://semver.org/).

## [Unreleased]

Target: 1.1.0 (version code 10100).

### Fixed
- Crash on Android 8–11 after every scan and after boot (`NoSuchMethodError`): wrong operator precedence when deciding between exact and inexact alarms. The scan heartbeat on Android 12+ is no longer scheduled as an exact alarm by mistake.
- Write-back ("Write done to note") is now strict: only valid UTF-8 notes are edited, only the indexed line changes, mixed line endings and a BOM stay as they were, and the result is read back and compared. On a mismatch the original content is restored; a backup is kept in the app cache until the write is verified.

### Added
- Reminder plan: several early reminders at configurable lead times (default 2 h, 1 h, 30 min, 15 min; off by default). Replaces the single early reminder; an existing early reminder setting is carried over.
- "Keep nudging": repeated reminders on the due day that get denser towards the due time, as editable stages (default: every 2 h from the start of the day, hourly from 2 h before, every 30 min from 1 h before). Only for reminders with an explicit time; stops when done, snoozed, opened or swiped away.
- Active hours (default 08:00–22:00) for early reminders and nudges; the main alarm always rings. Early reminders are normal notifications unless "break through Do Not Disturb" is switched on for them too.
- Note properties as reminders (`reminder`, `due`, `remind`; names configurable; off by default). Done is kept in the app only and resets when the property changes.
- Repeating reminders: `🔁 every day|week|month|weekday` (Tasks plugin) and `| daily|weekly|monthly|weekdays`. Done completes only the current instance; missed instances are skipped, the note is never changed.
- Option "Hide on lock screen": sound and vibration as usual, title and note path appear only after unlocking. When Android hides sensitive lock screen content, reminders now show a short version without title and path.
- Onboarding explains that vaults inside `Android/data` (e.g. the app folder of Obsidian) cannot be opened by other apps.

### Changed
- Early reminders, nudges and the main alarm share one notification per reminder, so a later one replaces the earlier one. Early reminders offer "Done" but no snooze.
- Alarms are chained (only the next early reminder is armed) and reminders more than 14 days away are armed later, which keeps the app far below Android's limit of concurrent alarms.
- Index database schema 2 (migrated, done and snooze states are kept). YAML frontmatter without a closing `---` is now read as normal text.

### Security
- Notes larger than 4 MiB are no longer read.
- Gradle wrapper verifies the distribution checksum (`distributionSha256Sum`).

## [1.0.0] - 2026-11-16

First stable release after field tests on a Nothing Phone (3a) Lite.

### Added
- Home screen widget (Glance) in three sizes: next reminder, next three, scrollable list. Dynamic Color or the chosen accent, refresh button, tap opens the reminder. Updates after every scan, done and snooze.
- Plural-aware texts, Android 12+ data extraction rules (no cloud backup, no device transfer).

### Fixed
- Reminders were swallowed by Do Not Disturb / silent mode. They are now delivered as alarms on a dedicated channel (category alarm, alarm audio stream); can be switched off in settings.
- Background scans were delayed for hours by Doze and OEM battery savers. An inexact allow-while-idle alarm now wakes the app at the scan interval in addition to WorkManager, and the list rescans silently whenever it becomes visible.

### Added (background)
- Onboarding step and settings shortcut for the one-tap battery optimization exemption.
- Hint that Obsidian Sync only updates files on the phone while Obsidian is running.

### Changed
- ktlint and detekt are now blocking in CI (detekt with a baseline for existing findings).
- Version code scheme: `major * 10000 + minor * 100 + patch`.

## [0.1.0] - 2026-11-12

### Added
- Onboarding: vault folder (SAF), `.obsidian` detection, vault name, notification and exact alarm permissions.
- Parser for Reminder plugin (`(@date time)`), Dataview (`[due:: …]`) and Tasks emoji (`⏰`, `📅`) syntax; default time for date-only reminders.
- Incremental scanning (changed files only) on start, pull-to-refresh and periodically via WorkManager.
- Room index with line hash diff; exact alarms with inexact fallback; rescheduling after reboot, update and time-zone change.
- Notifications that open the note in Obsidian, with done and snooze actions; optional early reminder and repeat-if-ignored.
- Optional write-back of `[x]` guarded by a line hash.
- Material 3 UI: grouped list with sticky headers, search, filters, swipe actions, container transform, two-pane layout, settings with status checks, privacy and about pages.
- German and English translations.

[Unreleased]: https://github.com/descomanian/vaultbell/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/descomanian/vaultbell/releases/tag/v1.0.0
[0.1.0]: https://github.com/descomanian/vaultbell/commits/v1.0.0
