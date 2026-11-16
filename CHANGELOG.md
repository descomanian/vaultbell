# Changelog

All notable changes are documented here. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), versioning: [SemVer](https://semver.org/).

## [Unreleased]

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
