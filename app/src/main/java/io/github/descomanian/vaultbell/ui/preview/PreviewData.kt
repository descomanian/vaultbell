package io.github.descomanian.vaultbell.ui.preview

import io.github.descomanian.vaultbell.data.Reminder
import io.github.descomanian.vaultbell.parser.Recurrence
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
        recurrence: Recurrence? = null
    ) = Reminder(
        id = id,
        title = title,
        due = due,
        originalDue = due,
        hasExplicitTime = explicit,
        isSnoozed = snoozed,
        relativePath = path,
        lineNumber = id.toInt() + 6,
        rawLine = "- [ ] $title (@${due.toLocalDate()} ${due.toLocalTime()})" + (recurrence?.let { " | ${it.name.lowercase()}" } ?: ""),
        format = format,
        recurrence = recurrence
    )

    private data class Item(
        val en: String,
        val de: String,
        val pathEn: String,
        val pathDe: String
    )

    private val items =
        listOf(
            Item("Send project proposal", "Projektangebot verschicken", "Work/Projects.md", "Arbeit/Projekte.md"),
            Item("Call the dentist", "Zahnarzt anrufen", "Daily/2026-11-14.md", "Daily/2026-11-14.md"),
            Item("Pick up dry cleaning", "Reinigung abholen", "Daily/2026-11-14.md", "Daily/2026-11-14.md"),
            Item("Water the plants", "Pflanzen gießen", "Home/Chores.md", "Zuhause/Haushalt.md"),
            Item("Book train tickets", "Zugtickets buchen", "Travel/Lisbon trip.md", "Reisen/Lissabon.md"),
            Item("Team retro", "Team-Retro", "Work/Meetings.md", "Arbeit/Meetings.md"),
            Item("Return library books", "Bücher zur Bibliothek bringen", "Errands.md", "Besorgungen.md"),
            Item("Finish chapter 4 notes", "Notizen zu Kapitel 4 fertig machen", "Reading/Book notes.md", "Lesen/Buchnotizen.md")
        )

    /** Neutral dummy reminders for previews, store screenshots and posts. No personal data. */
    fun reminders(german: Boolean = false): List<Reminder> {
        fun t(i: Int) = if (german) items[i].de else items[i].en
        fun p(i: Int) = if (german) items[i].pathDe else items[i].pathEn
        return listOf(
            r(1, t(0), now.minusDays(1).withHour(17).withMinute(0), p(0)),
            r(2, t(1), now.minusMinutes(24), p(1)),
            r(3, t(2), now.withHour(13).withMinute(0), p(2), explicit = false),
            r(4, t(3), now.withHour(18).withMinute(30), p(3), format = ReminderFormat.DATAVIEW, recurrence = Recurrence.DAILY),
            r(5, t(4), now.withHour(19).withMinute(15), p(4), snoozed = true),
            r(6, t(5), now.plusDays(1).withHour(10).withMinute(0), p(5), recurrence = Recurrence.WEEKLY),
            r(7, t(6), now.plusDays(3).withHour(16).withMinute(30), p(6)),
            r(8, t(7), now.plusDays(6).withHour(20).withMinute(0), p(7))
        )
    }

    val reminders: List<Reminder> get() = reminders(german = false)

    const val VAULT_NAME = "My Vault"
}
