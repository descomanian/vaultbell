# Changelog

All notable changes are documented here. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), versioning: [SemVer](https://semver.org/).

## [Unreleased]

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
