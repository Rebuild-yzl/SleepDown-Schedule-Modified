package com.xiaomanjun.sleepdownschedule.feature.todo

import com.xiaomanjun.sleepdownschedule.model.TodoItemEntity
import com.xiaomanjun.sleepdownschedule.model.TodoQuadrant
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal enum class TodoDueLevel { OVERDUE, TODAY, DATED }

internal data class TodoDueMeta(val text: String, val level: TodoDueLevel)

/**
 * 象限内固定排序：未完成在前，其中逾期 → 有日期（近的在前）→ 无日期，已完成沉到末尾。
 *
 * [settledDoneIds] 为空集时按当前完成态排序；传入「进入页面时已完成」的 id 集合时，本次刚勾选
 * 的行仍按未完成的位置参与排序，避免点完之后自己滑到格尾。是否划线只看 `isDone`。
 */
internal fun sortTodoItems(
    items: List<TodoItemEntity>,
    settledDoneIds: Set<Long>? = null
): List<TodoItemEntity> =
    items.sortedWith(
        compareBy<TodoItemEntity>(
            { item -> if (settledDoneIds == null) item.isDone else item.id in settledDoneIds },
            { it.dueDate == null },
            { it.dueDate.orEmpty() },
            { it.createdAt }
        )
    )

internal fun groupTodoItems(
    items: List<TodoItemEntity>,
    settledDoneIds: Set<Long>? = null
): Map<TodoQuadrant, List<TodoItemEntity>> {
    val grouped = items.groupBy { it.quadrantValue }
    return TodoQuadrant.entries.associateWith {
        sortTodoItems(grouped[it].orEmpty(), settledDoneIds)
    }
}

internal fun parseIsoDate(value: String?): LocalDate? =
    value?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

private val ChineseDayNumbers = listOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九")

/** 1–9 用中文数字，和「还有一天」的写法一致；更长的天数用阿拉伯数字。 */
internal fun todoDayCount(days: Long): String =
    if (days in 1..9) ChineseDayNumbers[days.toInt()] else days.toString()

/** 逾期 / 已完成 这类回顾性描述统一用阿拉伯数字，和「已完成 15天前」一致。 */
private fun elapsedDays(days: Long): String = days.toString()

internal fun todoCreatedDate(item: TodoItemEntity, zone: ZoneId): LocalDate? =
    item.createdAt.takeIf { it > 0L }?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }

/**
 * 待办在某个日期页是否出现：创建日到截止日之间每天出现（已完成也出现）；
 * 逾期未完成的继续每天出现，直到完成；逾期已完成的停在下一次截止日之后。
 */
internal fun todoVisibleOn(item: TodoItemEntity, pageDate: LocalDate, zone: ZoneId): Boolean {
    val created = todoCreatedDate(item, zone)
    if (created != null && pageDate.isBefore(created)) return false
    val due = parseIsoDate(item.dueDate) ?: return true
    if (!pageDate.isAfter(due)) return true
    return !item.isDone
}

/**
 * 某一天页面上的副标题：未完成显示距离截止日还有多久（相对当前页），逾期显示已过天数；
 * 已完成显示距离完成日的真实天数（相对今天）。
 */
internal fun todoDueMeta(item: TodoItemEntity, pageDate: LocalDate): TodoDueMeta? {
    if (item.isDone) return null
    val date = parseIsoDate(item.dueDate) ?: return null
    val days = ChronoUnit.DAYS.between(pageDate, date)
    return when {
        days < 0L -> TodoDueMeta("已逾期 ${elapsedDays(-days)}天", TodoDueLevel.OVERDUE)
        days == 0L -> TodoDueMeta("今天 到期", TodoDueLevel.TODAY)
        days == 1L -> TodoDueMeta("明天 还有一天", TodoDueLevel.DATED)
        days == 2L -> TodoDueMeta("后天 还有两天", TodoDueLevel.DATED)
        days in 3L..6L && sameIsoWeek(pageDate, date) ->
            TodoDueMeta("本周 还有${todoDayCount(days)}天", TodoDueLevel.DATED)
        date.year == pageDate.year && date.month == pageDate.month ->
            TodoDueMeta("本月 还有${todoDayCount(days)}天", TodoDueLevel.DATED)
        date.year == pageDate.plusMonths(1).year && date.month == pageDate.plusMonths(1).month ->
            TodoDueMeta("下个月 还有${todoDayCount(days)}天", TodoDueLevel.DATED)
        else -> TodoDueMeta("${date.monthValue}月 还有${todoDayCount(days)}天", TodoDueLevel.DATED)
    }
}

private fun sameIsoWeek(a: LocalDate, b: LocalDate): Boolean {
    fun weekStart(date: LocalDate) = date.minusDays((date.dayOfWeek.value - 1).toLong())
    return weekStart(a) == weekStart(b)
}

internal fun todoDoneLabel(doneAtMillis: Long, today: LocalDate, zone: ZoneId): String {
    val doneDate = Instant.ofEpochMilli(doneAtMillis).atZone(zone).toLocalDate()
    val days = ChronoUnit.DAYS.between(doneDate, today)
    return when {
        days <= 0L -> "已完成 今天"
        days == 1L -> "已完成 昨天"
        else -> "已完成 ${elapsedDays(days)}天前"
    }
}

internal fun unfinishedCount(items: List<TodoItemEntity>): Int = items.count { !it.isDone }
