package io.github.descomanian.vaultbell.parser

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * Repeat rules supported in v1.1. Occurrences are computed in local time from the anchor (the date written in the note),
 * so 09:00 stays 09:00 across daylight saving changes and monthly series keep their day where the month allows it
 * (31st → 30th/28th/29th, back to the 31st in long months).
 */
enum class Recurrence {
    DAILY,
    WEEKLY,
    MONTHLY,

    /** Monday to Friday. */
    WEEKDAYS;

    /** First occurrence at or after [notBefore]. */
    fun firstOnOrAfter(
        anchor: LocalDateTime,
        notBefore: LocalDateTime
    ): LocalDateTime {
        if (!anchor.isBefore(notBefore)) return if (this == WEEKDAYS) anchor.toWeekday() else anchor
        return when (this) {
            DAILY -> stepDays(anchor, notBefore)
            WEEKDAYS -> stepDays(anchor, notBefore).toWeekday()
            WEEKLY -> {
                val weeks = ChronoUnit.DAYS.between(anchor.toLocalDate(), notBefore.toLocalDate()) / DAYS_PER_WEEK
                var candidate = anchor.plusWeeks(weeks)
                while (candidate.isBefore(notBefore)) candidate = candidate.plusWeeks(1)
                candidate
            }
            MONTHLY -> {
                val months = ChronoUnit.MONTHS.between(YearMonth.from(anchor), YearMonth.from(notBefore))
                val candidate = anchor.plusMonths(months)
                if (candidate.isBefore(notBefore)) anchor.plusMonths(months + 1) else candidate
            }
        }
    }

    /** First occurrence strictly after [after], e.g. the next instance once the current one is done. */
    fun nextAfter(
        anchor: LocalDateTime,
        after: LocalDateTime
    ): LocalDateTime = firstOnOrAfter(anchor, after.plusSeconds(1))

    private fun stepDays(
        anchor: LocalDateTime,
        notBefore: LocalDateTime
    ): LocalDateTime {
        val candidate = anchor.plusDays(ChronoUnit.DAYS.between(anchor.toLocalDate(), notBefore.toLocalDate()))
        return if (candidate.isBefore(notBefore)) candidate.plusDays(1) else candidate
    }

    private fun LocalDateTime.toWeekday(): LocalDateTime =
        when (dayOfWeek) {
            DayOfWeek.SATURDAY -> plusDays(2)
            DayOfWeek.SUNDAY -> plusDays(1)
            else -> this
        }

    private companion object {
        const val DAYS_PER_WEEK = 7
    }
}
