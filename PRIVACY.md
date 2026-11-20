# Privacy Policy

**Vaultbell collects no data.**

- The app has **no internet permission** and cannot open network connections.
- No account, no analytics, no crash reporting, no advertising, no Google Play Services.
- Access is limited to the one folder you select through Android's folder picker (Storage Access Framework). You can revoke it in Android's settings at any time.
- The reminder index (title, note path, line number, time) and your settings are stored only on your device and are excluded from cloud backups (`allowBackup=false`).
- Notes are only modified if you enable "Write done to note"; then only the checkbox of the one-time task you mark as done is changed. Repeating tasks are never changed; their completed dates (title, note path, date) are kept on the device in the repeat log and only leave the app when you export it to a file you choose. While writing, a copy of the original note is kept in the app's private cache and deleted once the write is verified.
- Notifications show the task title and the note path. Android may show them on the lock screen; *Settings → Hide on lock screen* keeps them off the lock screen.

Permissions: notifications, exact alarms, run after reboot, vibration, and the request to be exempt from battery optimization (so background scans are not delayed). WorkManager additionally declares `ACCESS_NETWORK_STATE`, `WAKE_LOCK` and `FOREGROUND_SERVICE`; none of them allows network access.

Contact: see the GitHub profile of the maintainer.
