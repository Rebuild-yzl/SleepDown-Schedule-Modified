package com.xiaomanjun.sleepdownschedule

import com.xiaomanjun.sleepdownschedule.feature.countdown.countdownDate
import com.xiaomanjun.sleepdownschedule.feature.countdown.heroCountdownEvent
import com.xiaomanjun.sleepdownschedule.feature.countdown.sortCountdownEvents
import com.xiaomanjun.sleepdownschedule.feature.todo.TodoDueLevel
import com.xiaomanjun.sleepdownschedule.feature.todo.groupTodoItems
import com.xiaomanjun.sleepdownschedule.feature.todo.sortTodoItems
import com.xiaomanjun.sleepdownschedule.feature.todo.todoDoneLabel
import com.xiaomanjun.sleepdownschedule.feature.todo.todoDueMeta
import com.xiaomanjun.sleepdownschedule.model.CountdownEventEntity
import com.xiaomanjun.sleepdownschedule.model.TodoItemEntity
import com.xiaomanjun.sleepdownschedule.model.TodoQuadrant
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoCountdownRulesTest {
    private val today = LocalDate.of(2026, 10, 25)

    private fun todo(
        id: Long,
        title: String = "t$id",
        quadrant: TodoQuadrant = TodoQuadrant.IMPORTANT_URGENT,
        due: String? = null,
        doneAt: Long? = null,
        createdAt: Long = id
    ) = TodoItemEntity(
        id = id, title = title, quadrant = quadrant.name, dueDate = due,
        doneAt = doneAt, createdAt = createdAt, updatedAt = createdAt
    )

    private fun event(
        id: Long,
        date: String,
        pinned: Boolean = false,
        repeat: Boolean = false,
        time: String? = null
    ) = CountdownEventEntity(
        id = id, title = "e$id", targetDate = date, targetTime = time,
        repeatYearly = repeat, pinned = pinned, createdAt = id, updatedAt = id
    )

    @Test
    fun todoSortKeepsOverdueFirstAndCompletedLast() {
        val undated = todo(1)
        val future = todo(2, due = "2026-10-30")
        val tomorrow = todo(3, due = "2026-10-26")
        val overdue = todo(4, due = "2026-10-20")
        val done = todo(5, due = "2026-10-01", doneAt = 1L)

        val sorted = sortTodoItems(listOf(done, undated, future, tomorrow, overdue))

        assertEquals(listOf(4L, 3L, 2L, 1L, 5L), sorted.map { it.id })
    }

    @Test
    fun todoGroupsFillAllFourQuadrantsInFixedOrder() {
        val items = listOf(
            todo(1, quadrant = TodoQuadrant.NEITHER),
            todo(2, quadrant = TodoQuadrant.IMPORTANT_URGENT, due = "2026-10-27"),
            todo(3, quadrant = TodoQuadrant.IMPORTANT_URGENT, due = "2026-10-26")
        )

        val grouped = groupTodoItems(items)

        assertEquals(TodoQuadrant.entries, grouped.keys.toList())
        assertEquals(listOf(3L, 2L), grouped.getValue(TodoQuadrant.IMPORTANT_URGENT).map { it.id })
        assertTrue(grouped.getValue(TodoQuadrant.URGENT_NOT_IMPORTANT).isEmpty())
        assertEquals(listOf(1L), grouped.getValue(TodoQuadrant.NEITHER).map { it.id })
    }

    @Test
    fun todoDueMetaCoversOverdueTodayTomorrowAndNone() {
        assertEquals("已逾期 5 天", todoDueMeta(todo(1, due = "2026-10-20"), today)?.text)
        assertEquals(TodoDueLevel.OVERDUE, todoDueMeta(todo(1, due = "2026-10-20"), today)?.level)
        assertEquals("今天到期", todoDueMeta(todo(2, due = "2026-10-25"), today)?.text)
        assertEquals("明天 · 还有 1 天", todoDueMeta(todo(3, due = "2026-10-26"), today)?.text)
        assertEquals("10月30日 · 还有 5 天", todoDueMeta(todo(4, due = "2026-10-30"), today)?.text)
        assertNull(todoDueMeta(todo(5), today))
        assertNull(todoDueMeta(todo(6, due = "2026-10-20", doneAt = 9L), today))
    }

    @Test
    fun todoDoneLabelUsesCalendarDays() {
        val zone = ZoneId.of("Asia/Shanghai")
        val yesterday = LocalDate.of(2026, 10, 24).atTime(22, 0).atZone(zone).toInstant().toEpochMilli()
        val earlier = Instant.parse("2026-10-20T02:00:00Z").toEpochMilli()

        assertEquals("已完成 · 昨天", todoDoneLabel(yesterday, today, zone))
        assertEquals("已完成 · 5 天前", todoDoneLabel(earlier, today, zone))
    }

    @Test
    fun repeatingCountdownRollsToNextYearAndHandlesLeapDay() {
        val yearly = event(1, "2026-03-01", repeat = true)
        assertEquals(LocalDate.of(2027, 3, 1), countdownDate(yearly, today))

        val leap = event(2, "2024-02-29", repeat = true)
        assertEquals(LocalDate.of(2027, 2, 28), countdownDate(leap, LocalDate.of(2027, 1, 1)))

        val fixed = event(3, "2026-10-01")
        assertEquals(LocalDate.of(2026, 10, 1), countdownDate(fixed, today))
    }

    @Test
    fun countdownSortPinsFirstThenFutureThenPast() {
        val past = event(1, "2026-10-20")
        val soon = event(2, "2026-10-27")
        val later = event(3, "2026-11-30")
        val pinned = event(4, "2027-01-01", pinned = true)

        val sorted = sortCountdownEvents(listOf(past, later, soon, pinned), today)

        assertEquals(listOf(4L, 2L, 3L, 1L), sorted.map { it.id })
    }

    @Test
    fun heroSkipsPastEventsAndPrefersNearestFuture() {
        val past = event(1, "2026-10-20")
        val todayEvent = event(2, "2026-10-25")
        val later = event(3, "2026-11-30")

        assertEquals(2L, heroCountdownEvent(listOf(past, later, todayEvent), today)?.id)
        assertNull(heroCountdownEvent(listOf(past), today))
    }

    @Test
    fun repeatingCountdownRollsWhenTodayMomentAlreadyPassed() {
        val yearly = event(1, "2026-10-25", repeat = true, time = "09:00")
        assertEquals(LocalDate.of(2026, 10, 25), countdownDate(yearly, today, LocalDateTime.of(2026, 10, 25, 8, 0)))
        assertEquals(LocalDate.of(2027, 10, 25), countdownDate(yearly, today, LocalDateTime.of(2026, 10, 25, 10, 0)))
        // 全天事件没有时刻，纪念日当天仍然显示当天。
        assertEquals(LocalDate.of(2026, 10, 25), countdownDate(event(2, "2026-10-25", repeat = true), today))
    }
}
