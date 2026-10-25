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
 * 勾选完成时不立即调用它重排，避免刚点完的行自己滑走。
 */
internal fun sortTodoItems(items: List<TodoItemEntity>): List<TodoItemEntity> =
    items.sortedWith(
        compareBy<TodoItemEntity>(
            { it.isDone },
            { it.dueDate == null },
            { it.dueDate.orEmpty() },
            { it.createdAt }
        )
    )

internal fun groupTodoItems(items: List<TodoItemEntity>): Map<TodoQuadrant, List<TodoItemEntity>> {
    val grouped = items.groupBy { it.quadrantValue }
    return TodoQuadrant.entries.associateWith { sortTodoItems(grouped[it].orEmpty()) }
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
