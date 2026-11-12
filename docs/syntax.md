# Reminder syntax

Vaultbell only looks at **open Markdown tasks** (`[ ]`). A task becomes a reminder when it contains a date in one of the supported formats. Everything else in your notes is ignored.

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
- YAML frontmatter at the top of a note
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

### Several dates in one line

The first enabled format in this order wins: Reminder plugin → Dataview → Tasks `⏰` → Tasks `📅`. All recognized date tokens are removed from the displayed title.

## Invalid values

Lines with impossible dates (`2026-02-30`) or times (`25:00`) are skipped silently.

## Marking done

With **Settings → Write done to note** enabled, "Done" changes `- [ ]` to `- [x]` in the note. Before writing, Vaultbell compares a hash of the line with the indexed version. If the line changed (edited on another device, sync in progress), nothing is written and only the notification is dismissed.

## Planned

- Recurrence such as `| weekly`
- Relative dates
