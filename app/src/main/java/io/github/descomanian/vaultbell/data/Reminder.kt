package io.github.descomanian.vaultbell.data

import io.github.descomanian.vaultbell.data.db.ReminderEntity
import io.github.descomanian.vaultbell.data.db.triggerAt
import io.github.descomanian.vaultbell.parser.ReminderFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** UI-facing reminder. [due] is the effective time (snooze applied). */
data class Reminder(
    val id: Long,
    val title: String,
    val due: LocalDateTime,
    val originalDue: LocalDateTime,
    val hasExplicitTime: Boolean,
    val isSnoozed: Boolean,
    val relativePath: String,
    val lineNumber: Int,
    val rawLine: String,
    val format: ReminderFormat,
) {
    val fileName: String get() = relativePath.substringAfterLast('/').removeSuffix(".md")
    val folder: String get() = relativePath.substringBeforeLast('/', missingDelimiterValue = "")
}

enum class ReminderGroup { OVERDUE, TODAY, TOMORROW, UPCOMING }

fun Reminder.groupAt(now: LocalDateTime): ReminderGroup {
    val today = now.toLocalDate()
    return when {
        due.isBefore(now) -> ReminderGroup.OVERDUE
        due.toLocalDate() == today -> ReminderGroup.TODAY
        due.toLocalDate() == today.plusDays(1) -> ReminderGroup.TOMORROW
        else -> ReminderGroup.UPCOMING
    }
}

fun Long.toLocalDateTime(zone: ZoneId = ZoneId.systemDefault()): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), zone)

fun LocalDateTime.toEpochMillis(zone: ZoneId = ZoneId.systemDefault()): Long = atZone(zone).toInstant().toEpochMilli()

fun ReminderEntity.toModel(): Reminder =
    Reminder(
        id = id,
        title = title,
        due = triggerAt.toLocalDateTime(),
        originalDue = dueAt.toLocalDateTime(),
        hasExplicitTime = hasExplicitTime,
        isSnoozed = snoozedUntil != null,
        relativePath = relativePath,
        lineNumber = lineNumber,
        rawLine = rawLine,
        format = ReminderFormat.entries.firstOrNull { it.name == format } ?: ReminderFormat.REMINDER_PLUGIN,
    )
