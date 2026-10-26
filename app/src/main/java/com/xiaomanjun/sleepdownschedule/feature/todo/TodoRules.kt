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

internal fun todoDoneDate(item: TodoItemEntity, zone: ZoneId): LocalDate? =
    item.doneAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }

/**
 * 待办在某个日期页是否出现：起点是创建日，终点分两种。
 *
 * - 未完成：max(截止日, 今天)。当天页显示逾期项，未来页不显示；无截止日则一直显示。
 * - 已完成：max(截止日, 完成日)。按期完成的看到截止日，逾期完成的看到完成日。
 */
internal fun todoVisibleOn(
    item: TodoItemEntity,
    pageDate: LocalDate,
    today: LocalDate,
    zone: ZoneId
): Boolean {
    val created = todoCreatedDate(item, zone)
    if (created != null && pageDate.isBefore(created)) return false
    val due = parseIsoDate(item.dueDate)
    val done = todoDoneDate(item, zone)
    val end = when {
        done != null -> if (due != null) maxOf(due, done) else done
        due != null -> maxOf(due, today)
        else -> return true
    }
    return !pageDate.isAfter(end)
}

/**
 * 未完成项在某一天页面上的副标题：以当页为基准倒数（还有几天 / 今天到期 / 已逾期几天）。
 * 已完成项走 [todoDoneMeta]，两者不会同时出现在同一行。
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
        // 跨年只在次年写「明年」，再远就写完整年份，免得「1月」看不出是哪一年。
        date.year == pageDate.year ->
            TodoDueMeta("${date.monthValue}月 还有${todoDayCount(days)}天", TodoDueLevel.DATED)
        date.year == pageDate.year + 1 ->
            TodoDueMeta("明年${date.monthValue}月 还有${todoDayCount(days)}天", TodoDueLevel.DATED)
        else ->
            TodoDueMeta("${date.year}年${date.monthValue}月 还有${todoDayCount(days)}天", TodoDueLevel.DATED)
    }
}

private fun sameIsoWeek(a: LocalDate, b: LocalDate): Boolean {
    fun weekStart(date: LocalDate) = date.minusDays((date.dayOfWeek.value - 1).toLong())
    return weekStart(a) == weekStart(b)
}

/**
 * 已完成行的副标题：以 [basis] 为基准（日期页传页日期，已完成列表传今天）。
 *
 * e = basis - 完成日：负数表示「还有几天才完成」，0 是完成当天，正数按天前回溯。
 * [overdueMoment] 只影响「当时逾期」后缀——日期页传页日期（那一页是否已过截止日），
 * 已完成列表传完成日（完成那一刻是否已逾期）。
 */
internal fun todoDoneMeta(
    item: TodoItemEntity,
    basis: LocalDate,
    zone: ZoneId,
    overdueMoment: LocalDate = basis
): TodoDueMeta? {
    val done = todoDoneDate(item, zone) ?: return null
    val days = ChronoUnit.DAYS.between(done, basis)
    val text = when {
        days < 0L -> "已完成 ${-days}天后"
        days == 0L -> "已完成 今天"
        days == 1L -> "已完成 昨天"
        else -> "已完成 ${elapsedDays(days)}天前"
    }
    val due = parseIsoDate(item.dueDate)
    val late = due != null && overdueMoment.isAfter(due)
    return TodoDueMeta(
        text = if (late) "$text 当时逾期" else text,
        level = if (late) TodoDueLevel.OVERDUE else TodoDueLevel.DATED
    )
}

/** 已完成列表没有页日期，基准取今天；「当时逾期」看完成那一刻是否已过截止日。 */
internal fun todoDoneListMeta(item: TodoItemEntity, today: LocalDate, zone: ZoneId): TodoDueMeta? =
    todoDoneMeta(item, today, zone, overdueMoment = todoDoneDate(item, zone) ?: today)

internal fun unfinishedCount(items: List<TodoItemEntity>): Int = items.count { !it.isDone }
