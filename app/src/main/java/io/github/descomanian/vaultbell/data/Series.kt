package io.github.descomanian.vaultbell.data

import io.github.descomanian.vaultbell.parser.Recurrence
import java.time.LocalDateTime

/**
 * Index-side rules for repeating reminders. The note is never changed: the current instance lives in the index.
 * Pure functions on local date-times, unit-tested on the JVM.
 */
object Series {
    /**
     * The instance a series should show now.
     * - A stored instance stays as long as it is from today or later (overdue today stays overdue).
     * - Instances before today are skipped, never caught up.
     * - Nothing at or before the last completed instance comes back.
     *
     * Pass [stored] = null when the anchor changed or the series is new.
     */
    fun currentInstance(
        rule: Recurrence,
        anchor: LocalDateTime,
        stored: LocalDateTime?,
        lastDone: LocalDateTime?,
        now: LocalDateTime
    ): LocalDateTime {
        val startOfToday = now.toLocalDate().atStartOfDay()
        val notBefore = if (lastDone != null && !lastDone.isBefore(startOfToday)) lastDone.plusSeconds(1) else startOfToday
        return stored?.takeUnless { it.isBefore(notBefore) } ?: rule.firstOnOrAfter(anchor, notBefore)
    }

    /** Next instance once [instance] is done at [now]: strictly after both, so finishing early skips nothing twice. */
    fun nextAfterDone(
        rule: Recurrence,
        anchor: LocalDateTime,
        instance: LocalDateTime,
        now: LocalDateTime
    ): LocalDateTime = rule.nextAfter(anchor, maxOf(instance, now))
}
