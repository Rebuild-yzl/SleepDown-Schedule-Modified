package com.xiaomanjun.sleepdownschedule

import com.xiaomanjun.sleepdownschedule.feature.countdown.countdownDate
import com.xiaomanjun.sleepdownschedule.feature.countdown.heroCountdownEvent
import com.xiaomanjun.sleepdownschedule.feature.countdown.sortCountdownEvents
import com.xiaomanjun.sleepdownschedule.feature.todo.TodoDueLevel
import com.xiaomanjun.sleepdownschedule.feature.todo.groupTodoItems
import com.xiaomanjun.sleepdownschedule.feature.todo.sortTodoItems
import com.xiaomanjun.sleepdownschedule.feature.todo.todoDoneListMeta
import com.xiaomanjun.sleepdownschedule.feature.todo.todoDoneMeta
import com.xiaomanjun.sleepdownschedule.feature.todo.todoDueMeta
import com.xiaomanjun.sleepdownschedule.feature.todo.todoVisibleOn
import com.xiaomanjun.sleepdownschedule.model.CountdownEventEntity
import com.xiaomanjun.sleepdownschedule.model.TodoItemEntity
import com.xiaomanjun.sleepdownschedule.model.TodoQuadrant
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun todoDueMetaCountsDownFromPageDate() {
        assertEquals("已逾期 5天", todoDueMeta(todo(1, due = "2026-10-20"), today)?.text)
        assertEquals(TodoDueLevel.OVERDUE, todoDueMeta(todo(1, due = "2026-10-20"), today)?.level)
        assertEquals("今天 到期", todoDueMeta(todo(2, due = "2026-10-25"), today)?.text)
        assertEquals("明天 还有一天", todoDueMeta(todo(3, due = "2026-10-26"), today)?.text)
        assertEquals("后天 还有两天", todoDueMeta(todo(4, due = "2026-10-27"), today)?.text)
        assertEquals("本月 还有五天", todoDueMeta(todo(5, due = "2026-10-30"), today)?.text)
        assertEquals("下个月 还有26天", todoDueMeta(todo(6, due = "2026-11-20"), today)?.text)
        // 跨年：次年写「明年」，再远写完整年份。
        val dec = LocalDate.of(2026, 12, 20)
        assertEquals("下个月 还有16天", todoDueMeta(todo(9, due = "2027-01-05"), dec)?.text)
        assertEquals("明年3月 还有71天", todoDueMeta(todo(10, due = "2027-03-01"), dec)?.text)
        val far = ChronoUnit.DAYS.between(dec, LocalDate.of(2028, 1, 10))
        assertEquals("2028年1月 还有${far}天", todoDueMeta(todo(11, due = "2028-01-10"), dec)?.text)
        assertNull(todoDueMeta(todo(7), today))
        assertNull(todoDueMeta(todo(8, due = "2026-10-20", doneAt = 9L), today))
    }

    @Test
    fun todoVisibleFromCreationToDueOrTodayWithOverdueCarryOver() {
        val zone = ZoneId.of("Asia/Shanghai")
        fun at(month: Int, day: Int) =
            LocalDate.of(2026, month, day).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()

        val open = todo(1, due = "2026-10-25", createdAt = at(10, 20))
        assertFalse(todoVisibleOn(open, LocalDate.of(2026, 10, 19), today, zone))
        assertTrue(todoVisibleOn(open, LocalDate.of(2026, 10, 20), today, zone))
        assertTrue(todoVisibleOn(open, LocalDate.of(2026, 10, 25), today, zone))
        // 逾期未完成：延续到实际今天，未来的页不显示。
        assertTrue(todoVisibleOn(open, LocalDate.of(2026, 10, 27), LocalDate.of(2026, 10, 27), zone))
        assertFalse(todoVisibleOn(open, LocalDate.of(2026, 10, 28), LocalDate.of(2026, 10, 27), zone))

        // 未到期：创建日到截止日之间都显示，未来页也显示。
        val later = todo(2, due = "2026-10-30", createdAt = at(10, 20))
        assertTrue(todoVisibleOn(later, LocalDate.of(2026, 10, 28), today, zone))

        // 逾期完成：看到完成日为止。
        val doneLate = open.copy(doneAt = at(10, 28))
        assertTrue(todoVisibleOn(doneLate, LocalDate.of(2026, 10, 26), today, zone))
        assertTrue(todoVisibleOn(doneLate, LocalDate.of(2026, 10, 28), today, zone))
        assertFalse(todoVisibleOn(doneLate, LocalDate.of(2026, 10, 29), today, zone))

        // 按期完成：看到截止日为止，之后不再出现。
        val doneOnTime = later.copy(doneAt = at(10, 22))
        assertTrue(todoVisibleOn(doneOnTime, LocalDate.of(2026, 10, 30), today, zone))
        assertFalse(todoVisibleOn(doneOnTime, LocalDate.of(2026, 10, 31), today, zone))

        // 没有截止日期：未完成一直出现，完成后停在完成日。
        val undated = todo(3, createdAt = at(10, 20))
        assertTrue(todoVisibleOn(undated, LocalDate.of(2027, 1, 1), today, zone))
        val undatedDone = undated.copy(doneAt = at(10, 21))
        assertTrue(todoVisibleOn(undatedDone, LocalDate.of(2026, 10, 21), today, zone))
        assertFalse(todoVisibleOn(undatedDone, LocalDate.of(2026, 10, 22), today, zone))
    }

    @Test
    fun todoDoneMetaCountsFromPageDateAndMarksOverdueAtThatPage() {
        val zone = ZoneId.of("Asia/Shanghai")
        fun at(month: Int, day: Int) =
            LocalDate.of(2026, month, day).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()

        // 10/28 截止，10/30 才完成：之前几页写「还有几天才完成」，过截止日的页追「当时逾期」。
        val late = todo(1, due = "2026-10-28", createdAt = at(10, 24), doneAt = at(10, 30))
        assertEquals("已完成 6天后", todoDoneMeta(late, LocalDate.of(2026, 10, 24), zone)?.text)
        assertEquals("已完成 2天后", todoDoneMeta(late, LocalDate.of(2026, 10, 28), zone)?.text)
        assertEquals("已完成 1天后 当时逾期", todoDoneMeta(late, LocalDate.of(2026, 10, 29), zone)?.text)
        assertEquals("已完成 今天 当时逾期", todoDoneMeta(late, LocalDate.of(2026, 10, 30), zone)?.text)
        // 已完成列表以今天为基准，「当时逾期」看完成那一刻。
        assertEquals("已完成 昨天 当时逾期", todoDoneListMeta(late, LocalDate.of(2026, 10, 31), zone)?.text)

        // 按期完成：可见的最后一页是截止日，不加「当时逾期」；之后的页本来就不再显示。
        val onTime = todo(2, due = "2026-10-30", createdAt = at(10, 24), doneAt = at(10, 26))
        assertEquals("已完成 4天后", todoDoneMeta(onTime, LocalDate.of(2026, 10, 22), zone)?.text)
        assertEquals("已完成 今天", todoDoneMeta(onTime, LocalDate.of(2026, 10, 26), zone)?.text)
        assertEquals("已完成 昨天", todoDoneMeta(onTime, LocalDate.of(2026, 10, 27), zone)?.text)
        assertEquals("已完成 4天前", todoDoneMeta(onTime, LocalDate.of(2026, 10, 30), zone)?.text)
        assertFalse(todoVisibleOn(onTime, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1), zone))
        // 列表里也不带后缀：完成得比截止日早。
        assertEquals("已完成 6天前", todoDoneListMeta(onTime, LocalDate.of(2026, 11, 1), zone)?.text)

        // 未完成没有完成态文案。
        assertNull(todoDoneMeta(todo(3, due = "2026-10-30"), LocalDate.of(2026, 10, 30), zone))
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
