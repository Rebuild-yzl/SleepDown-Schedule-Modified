package com.xiaomanjun.sleepdownschedule.feature.countdown

import com.xiaomanjun.sleepdownschedule.model.CountdownEventEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.Year
import java.time.temporal.ChronoUnit

internal fun parseTargetDate(event: CountdownEventEntity): LocalDate? =
    event.targetDate.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

internal fun parseIsoTime(value: String?): LocalTime? =
    value?.takeIf { it.isNotBlank() }?.let { runCatching { LocalTime.parse(it) }.getOrNull() }

/**
 * 事件在今天的日历口径下的目标日期。每年重复按「今年的这一天」算，已经过了就顺延到明年；
 * 2 月 29 日在平年落到 2 月 28 日，不抛异常。
 */
internal fun countdownDate(
    event: CountdownEventEntity,
    today: LocalDate,
    now: LocalDateTime? = null
): LocalDate? {
    val base = parseTargetDate(event) ?: return null
    if (!event.repeatYearly) return base
    var candidate = withYearSafely(base, today.year)
    // 今天就是纪念日但当天那个时刻已经过去时，重复事件应顺延到明年，
    // 否则 Hero 会一直停在 00:00:00。
    val momentPassed = now != null && candidate == today &&
        !countdownMoment(event, candidate).isAfter(now)
    if (candidate.isBefore(today) || momentPassed) {
        candidate = withYearSafely(base, today.year + 1)
    }
    return candidate
}

private fun withYearSafely(date: LocalDate, year: Int): LocalDate =
    if (date.month == Month.FEBRUARY && date.dayOfMonth == 29 && !Year.isLeap(year.toLong())) {
        LocalDate.of(year, 2, 28)
    } else {
        date.withYear(year)
    }

internal fun countdownMoment(event: CountdownEventEntity, date: LocalDate): LocalDateTime =
    date.atTime(parseIsoTime(event.targetTime) ?: LocalTime.MIN)

/** 置顶优先；其余未来事件按剩余天数升序，过期事件排最后并按最近过期优先。 */
internal fun sortCountdownEvents(
    events: List<CountdownEventEntity>,
    today: LocalDate,
    now: LocalDateTime? = null
): List<CountdownEventEntity> = events.sortedWith(
    compareByDescending<CountdownEventEntity> { it.pinned }
        .thenBy { event ->
            val date = countdownDate(event, today, now)
            val days = date?.let { ChronoUnit.DAYS.between(today, it) }
            when {
                days == null -> 2
                days < 0L -> 1
                else -> 0
            }
        }
        .thenBy { event ->
            val date = countdownDate(event, today, now)
            val days = date?.let { ChronoUnit.DAYS.between(today, it) } ?: Long.MAX_VALUE
            if (days >= 0L) days else -days
        }
)

/** Hero 只显示最近一个未过期事件；全是过期事件时返回 null，让页面走空态。 */
internal fun heroCountdownEvent(
    events: List<CountdownEventEntity>,
    today: LocalDate,
    now: LocalDateTime? = null
): CountdownEventEntity? =
    events
        .mapNotNull { event -> countdownDate(event, today, now)?.let { date -> event to date } }
        .filter { (_, date) -> !date.isBefore(today) }
        .minByOrNull { (_, date) -> ChronoUnit.DAYS.between(today, date) }
        ?.first

internal fun countdownDays(today: LocalDate, date: LocalDate): Long = ChronoUnit.DAYS.between(today, date)
