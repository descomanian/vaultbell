# Reminder syntax

Vaultbell looks at **open Markdown tasks** (`[ ]`) and, if switched on, at **note properties**. A task becomes a reminder when it contains a date in one of the supported formats. Everything else in your notes is ignored.

## What counts as a task

```markdown
- [ ] dash
* [ ] star
+ [ ] plus
1. [ ] ordered
3) [ ] ordered with parenthesis
	- [ ] indented (tabs or spaces)
> - [ ] inside a blockquote
```

Ignored:

- done or cancelled tasks: `[x]`, `[X]`, `[-]` and any other status character
- YAML frontmatter at the top of a note, except for the property names below when "Note properties" is on (a block without closing `---` is read as normal text)
- fenced code blocks (```` ``` ```` and `~~~`)
- hidden folders such as `.obsidian`, `.trash`, `.git`

## Formats

Dates are ISO `YYYY-MM-DD`. Times are 24 h `H:mm` or `HH:mm`; `T` instead of a space also works. A date without a time uses the **default time** from the settings (09:00 unless changed).

### Reminder plugin (default on)

Compatible with the Obsidian Reminder plugin.

```markdown
- [ ] Call the dentist (@2026-11-14 18:30)
- [ ] Take out the trash (@2026-11-15)
```

### Dataview inline field (default off)

Field names `due`, `remind` and `reminder` are recognized, in square or round brackets.

```markdown
- [ ] Tag the release [due:: 2026-11-20 18:00]
- [ ] Renew passport (reminder:: 2026-12-01)
```

### Tasks plugin emoji (default off)

`⏰` with an optional time, or the due date `📅` (date only, uses the default time).

```markdown
- [ ] Take screenshots ⏰ 2026-11-21 10:15
- [ ] Submit to F-Droid 📅 2026-11-30
```

### Note properties (default off)

A date property in the frontmatter becomes a reminder for the whole note. Default names are `reminder`, `due` and `remind` (case does not matter); you can change them under *Settings → Detection → Property names*.

```markdown
---
title: Submit the report
reminder: 2026-11-20T18:30
due:
  - 2026-11-27
  - 2026-12-04 09:15
---
```

- Values: a date (`2026-11-20`, uses the default time) or a date with time (`2026-11-20T18:30`, Obsidian's "Date & time" type; a space instead of `T` and seconds also work). Time zones are not supported.
- A list gives one reminder per date, plain or in brackets: `due: [2026-11-27, 2026-12-04]`.
- The title is the `title` property, otherwise the file name.
- A note with properties **and** task lines gives separate reminders.
- Done is kept in the app only, the note is never changed. When the property value changes, the reminder is active again.

### Several dates in one line

The first enabled format in this order wins: Reminder plugin → Dataview → Tasks `⏰` → Tasks `📅`. All recognized date tokens are removed from the displayed title.

## Repeating reminders

Add a repeat rule to any task line, in either syntax:

| Rule | Tasks plugin | Short form |
|---|---|---|
| every day | `🔁 every day` | `\| daily` |
| every week | `🔁 every week` | `\| weekly` |
| every month | `🔁 every month` | `\| monthly` |
| Monday to Friday | `🔁 every weekday` | `\| weekdays` |

```markdown
- [ ] Take out the trash (@2026-11-17 07:00) | weekly
- [ ] Stand-up (@2026-11-17 09:30) | weekdays
- [ ] Water the plants ⏰ 2026-11-18 08:00 🔁 every day
- [ ] Review 📅 2026-11-21 🔁 every week when done
```

- The date in the line is the first occurrence. Vaultbell keeps the **current occurrence** in the app; the note is never changed.
- "Done" completes only the current occurrence, the series continues with the next date (Undo is possible).
- Missed occurrences are skipped, not caught up. An occurrence that is overdue today stays overdue until midnight.
- Times are local: 09:00 stays 09:00 across daylight saving changes. Monthly series keep their day where the month has it (31st → 30th/28th/29th, back to the 31st later).
- Other Tasks rules (`every 2 weeks`, `every Monday`, …) are not supported yet; such a line is a one-off reminder and the rule text is removed from the title.
- If the Tasks plugin itself completes a repeating task in Obsidian, it writes a new line with the next date. Vaultbell sees the new line as a new reminder.

## Early reminders and keeping you on it

These settings are global (*Settings → Reminder plan*), there is no syntax for them in the note.

- **Early reminders:** extra notifications at fixed lead times, default 2 h, 1 h, 30 min and 15 min before the due time. Off by default.
- **Keep nudging:** repeated reminders on the due day that get denser towards the due time, as stages *"from X before: every Y"*. Default: every 2 h from the start of the day, hourly from 2 h before, every 30 min from 1 h before. Each stage is counted backwards from its end, so a reminder due at 20:00 with the start of the day at 09:00 nudges at 10, 12, 14, 16, 18, 19 and 19:30. Only for reminders with an explicit time.
- Points close to each other (under 10 min) count once. Points that are already past are skipped.
- **Active hours** (default 08:00–22:00) apply to early reminders and nudges, never to the main alarm.
- Early reminders and nudges are normal notifications unless "Early reminders break through silence" is on. They replace each other instead of stacking and stop when you open, swipe away, snooze or complete the reminder.

## Invalid values

Lines with impossible dates (`2026-02-30`) or times (`25:00`) are skipped silently.

## Marking done

With **Settings → Write done to note** enabled, "Done" changes `- [ ]` to `- [x]` in the note. Before writing, Vaultbell compares a hash of the line with the indexed version. If the line changed (edited on another device, sync in progress), nothing is written and only the notification is dismissed.

Write-back is careful on purpose:

- Only notes saved as UTF-8 are edited; other encodings are left alone.
- Only the checkbox of that one line changes; line endings (also mixed CRLF/LF) and a BOM stay as they are.
- The note is read back after writing. If it differs, the original content is written back. A copy of the original is kept in the app cache until the write is confirmed.
- Repeating reminders and note properties are never written back.

## Planned

- More repeat rules (`every 2 weeks`, weekdays by name)
- Relative dates
- Per-note early reminder plans
