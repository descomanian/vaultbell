package io.github.descomanian.vaultbell.ui.preview

import io.github.descomanian.vaultbell.data.Reminder
import io.github.descomanian.vaultbell.parser.ReminderFormat
import java.time.LocalDateTime

/** Fake data for @Preview and the design prototype. */
object PreviewData {
    val now: LocalDateTime = LocalDateTime.of(2026, 11, 14, 10, 24)

    private fun r(
        id: Long,
        title: String,
        due: LocalDateTime,
        path: String,
        explicit: Boolean = true,
        snoozed: Boolean = false,
        format: ReminderFormat = ReminderFormat.REMINDER_PLUGIN,
    ) = Reminder(
        id = id,
        title = title,
        due = due,
        originalDue = due,
        hasExplicitTime = explicit,
        isSnoozed = snoozed,
        relativePath = path,
        lineNumber = id.toInt() + 6,
        rawLine = "- [ ] $title (@${due.toLocalDate()} ${due.toLocalTime()})",
        format = format,
    )

    val reminders: List<Reminder> =
        listOf(
            r(1, "Rechnung an Kunde schicken", now.minusDays(1).withHour(17).withMinute(0), "Arbeit/simpleIT.md"),
            r(2, "Zahnarzt anrufen", now.minusMinutes(24), "Daily/2026-11-14.md"),
            r(3, "Wäsche abholen", now.withHour(13).withMinute(0), "Daily/2026-11-14.md", explicit = false),
            r(4, "Release 0.1 taggen", now.withHour(18).withMinute(30), "Projekte/Vaultbell.md", format = ReminderFormat.DATAVIEW),
            r(5, "Mama zurückrufen", now.withHour(19).withMinute(15), "Daily/2026-11-14.md", snoozed = true),
            r(6, "Atemschutz-Übung", now.plusDays(1).withHour(19).withMinute(0), "Feuerwehr/Termine.md"),
            r(7, "Screenshots für F-Droid", now.plusDays(3).withHour(10).withMinute(15), "Projekte/Vaultbell.md"),
            r(8, "Coursera Modul 3 abschließen", now.plusDays(6).withHour(20).withMinute(0), "Kurse/Cyber Security.md"),
        )
}
