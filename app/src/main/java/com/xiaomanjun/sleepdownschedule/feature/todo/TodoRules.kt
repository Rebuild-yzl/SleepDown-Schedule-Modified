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

/** 完成态不再提示逾期，元信息换成完成时间。 */
internal fun todoDueMeta(item: TodoItemEntity, today: LocalDate): TodoDueMeta? {
    if (item.isDone) return null
    val date = parseIsoDate(item.dueDate) ?: return null
    val days = ChronoUnit.DAYS.between(today, date)
    return when {
        days < 0L -> TodoDueMeta("已逾期 ${-days} 天", TodoDueLevel.OVERDUE)
        days == 0L -> TodoDueMeta("今天到期", TodoDueLevel.TODAY)
        days == 1L -> TodoDueMeta("明天 · 还有 1 天", TodoDueLevel.DATED)
        else -> TodoDueMeta("${date.monthValue}月${date.dayOfMonth}日 · 还有 $days 天", TodoDueLevel.DATED)
    }
}

internal fun todoDoneLabel(doneAtMillis: Long, today: LocalDate, zone: ZoneId): String {
    val doneDate = Instant.ofEpochMilli(doneAtMillis).atZone(zone).toLocalDate()
    val days = ChronoUnit.DAYS.between(doneDate, today)
    return when {
        days <= 0L -> "已完成 · 今天"
        days == 1L -> "已完成 · 昨天"
        else -> "已完成 · $days 天前"
    }
}

internal fun unfinishedCount(items: List<TodoItemEntity>): Int = items.count { !it.isDone }
