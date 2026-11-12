<div align="center">

# Vaultbell

**Local-first reminders for Markdown vaults.**
Write a task with a date in your notes, get an Android alarm on time.
No account. No server. No tracking. No internet permission.

[![CI](https://github.com/descomanian/vaultbell/actions/workflows/ci.yml/badge.svg)](https://github.com/descomanian/vaultbell/actions/workflows/ci.yml)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

<!-- Screenshots: put PNGs into fastlane/metadata/android/en-US/images/phoneScreenshots/ and reference them here. -->
| Reminders | Detail | Settings | Onboarding |
|---|---|---|---|
| _screenshot_ | _screenshot_ | _screenshot_ | _screenshot_ |

</div>

> Works with [Obsidian](https://obsidian.md). Not affiliated with or endorsed by Obsidian.

## How it works

1. Pick your vault folder once (Android's folder picker, no "all files" permission).
2. Write tasks like this in any note:

   ```markdown
   - [ ] Call the dentist (@2026-11-14 18:30)
   - [ ] Take out the trash (@2026-11-15)
   ```

3. Vaultbell scans the folder (on start, on pull-to-refresh and every 15–240 minutes in the background), indexes open tasks with a date and schedules exact alarms.
4. Tap the notification to open the note in Obsidian. Mark it done or snooze it right from the notification.

Supported syntaxes (each can be switched on/off) are documented in [docs/syntax.md](docs/syntax.md):

| Format | Example | Default |
|---|---|---|
| Reminder plugin | `- [ ] Text (@2026-11-14 18:30)` | on |
| Dataview field | `- [ ] Text [due:: 2026-11-14 18:30]` | off |
| Tasks emoji | `- [ ] Text ⏰ 2026-11-14 18:30` / `📅 2026-11-14` | off |

## Principles

- **No `INTERNET` permission.** The merged release manifest is checked in CI.
- **No proprietary dependencies.** No Google Play Services, Firebase or analytics. F-Droid compatible.
- **Storage Access Framework only.** No `MANAGE_EXTERNAL_STORAGE`; access is limited to the folder you choose.
- **Read-only by default.** Writing `[x]` back into a note is opt-in and guarded by a line hash to avoid sync conflicts.

## Features (0.1)

- Guided onboarding: vault, notifications, exact alarms (Android 14+), with clear explanations.
- Reminder list grouped into Overdue / Today / Tomorrow / Upcoming with sticky headers, search, filters and swipe actions (done / snooze).
- Container transform from list to detail, predictive back, edge-to-edge, haptics.
- Material 3 with Dynamic Color, own fallback palettes (5 accents), light/dark/system and AMOLED black.
- Two-pane layout and navigation rail on tablets and foldables.
- Notification actions: done, snooze short, snooze long. Optional early reminder and repeat-if-ignored.
- Incremental scans (only changed files), alarms restored after reboot, time-zone changes handled.
- German and English.

## Build

Requirements: JDK 17+ (Android Studio's bundled JDK works), Android SDK 35.

```bash
./gradlew build          # unit tests, lint, ktlint/detekt reports, debug + release APKs
./gradlew installDebug   # install on a connected device (USB debugging on)
```

The debug build installs side by side with a release build (`.debug` suffix).

### Rename the app

The name is not final. Change it in **one place**: `gradle.properties` → `vaultbell.appName` (and optionally `vaultbell.applicationId`, `vaultbell.repoUrl`).

### Release signing

Create `keystore.properties` in the project root (never commit it):

```properties
storeFile=release.jks
storePassword=…
keyAlias=vaultbell
keyPassword=…
```

Without it, `assembleRelease` produces an unsigned APK (what F-Droid expects).

## Project structure

| Module | Purpose |
|---|---|
| `core-parser` | Pure Kotlin/JVM. Parses Markdown lines into reminders, edits task lines. No Android dependency, reusable elsewhere. |
| `vault-access` | Storage Access Framework: list Markdown files via `DocumentsContract`, read/write. |
| `scheduler` | Thin `AlarmManager` wrapper with exact/inexact fallback. |
| `designsystem` | Theme, colors, typography, shared Compose components. |
| `app` | UI (Compose), Room index, DataStore settings, WorkManager scan, notifications. |
| `sample-vault` | Fixture vault used by tests and for manual testing on a device. |

Architecture: MVVM with `ViewModel` + `StateFlow`, manual dependency injection (`AppContainer`), no annotation-processing DI.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Please read the [Code of Conduct](CODE_OF_CONDUCT.md). Security issues: [SECURITY.md](SECURITY.md).

## Privacy

Vaultbell collects no data. See [PRIVACY.md](PRIVACY.md).

## License

[GNU General Public License v3.0](LICENSE)
