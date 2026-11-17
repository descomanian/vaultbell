package io.github.descomanian.vaultbell.alarm

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.abs

/** "From [startMinutesBefore] before the due time on: every [everyMinutes]." */
data class NagStage(
    val startMinutesBefore: Int,
    val everyMinutes: Int
)

/** Allowed window for early reminders, e.g. 08:00–22:00. May cross midnight. Not applied to the main alarm. */
data class ActiveHours(
    val from: LocalTime,
    val until: LocalTime
) {
    fun allows(time: LocalTime): Boolean =
        when {
            from == until -> true
            from.isBefore(until) -> !time.isBefore(from) && time.isBefore(until)
            else -> !time.isBefore(from) || time.isBefore(until)
        }
}

private val DEFAULT_DAY_START: LocalTime = LocalTime.of(9, 0)

/** Everything the plan needs from the settings. Empty lists switch a part off. */
data class PlanSettings(
    /** Lead times ("Erinnerungsplan"), e.g. 120, 60, 30, 15 minutes. */
    val offsetsMinutes: List<Int> = emptyList(),
    /** "Dranbleiben": repeated reminders on the due day, denser the closer the due time gets. */
    val nagStages: List<NagStage> = emptyList(),
    /** Earliest time for "Dranbleiben" on the due day. */
    val dayStart: LocalTime = DEFAULT_DAY_START,
    /** Null: no quiet hours. */
    val activeHours: ActiveHours? = null
)

/**
 * Early reminders for one due time (one instance), computed without Android so it can be unit-tested.
 *
 * - Lead times: due − offset.
 * - Stages ("Dranbleiben", only for reminders with an explicit time): stages sorted from the earliest start; each stage
 *   covers [due − start, next stage start) and is rastered backwards from its end in its own interval.
 *   Nothing before [PlanSettings.dayStart] of the due day.
 * - Stage points closer than [DEDUPE_MINUTES] to a lead-time point count once (the lead time wins).
 * - Points outside the active hours are dropped. Result is sorted and strictly before due.
 */
object ReminderPlan {
    const val DEDUPE_MINUTES = 10L

    fun points(
        due: LocalDateTime,
        hasExplicitTime: Boolean,
        p: PlanSettings
    ): List<LocalDateTime> {
        val leads = p.offsetsMinutes.filter { it > 0 }.map { due.minusMinutes(it.toLong()) }.toSortedSet()
        val nag = if (hasExplicitTime) stagePoints(due, p) else emptyList()
        val kept = nag.filter { n -> leads.none { l -> minutesBetween(l, n) < DEDUPE_MINUTES } }
        return (leads + kept)
            .filter { it.isBefore(due) }
            .filter { p.activeHours?.allows(it.toLocalTime()) ?: true }
            .distinct()
            .sorted()
    }

    /** Next point strictly after [after] (now, or the last fired plan point), or null if none is left. */
    fun next(
        due: LocalDateTime,
        hasExplicitTime: Boolean,
        p: PlanSettings,
        after: LocalDateTime
    ): LocalDateTime? = points(due, hasExplicitTime, p).firstOrNull { it.isAfter(after) }

    private fun stagePoints(
        due: LocalDateTime,
        p: PlanSettings
    ): List<LocalDateTime> {
        val stages =
            p.nagStages
                .filter { it.startMinutesBefore > 0 && it.everyMinutes > 0 }
                .sortedByDescending { it.startMinutesBefore }
        val dayStart = due.toLocalDate().atTime(p.dayStart)
        val result = ArrayList<LocalDateTime>()
        stages.forEachIndexed { i, stage ->
            val start = maxOf(due.minusMinutes(stage.startMinutesBefore.toLong()), dayStart)
            val end = stages.getOrNull(i + 1)?.let { due.minusMinutes(it.startMinutesBefore.toLong()) } ?: due
            var t = end.minusMinutes(stage.everyMinutes.toLong())
            while (!t.isBefore(start)) {
                result += t
                t = t.minusMinutes(stage.everyMinutes.toLong())
            }
        }
        return result
    }

    private fun minutesBetween(
        a: LocalDateTime,
        b: LocalDateTime
    ): Long = abs(Duration.between(a, b).toMinutes())
}
