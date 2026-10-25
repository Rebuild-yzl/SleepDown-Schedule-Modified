package com.xiaomanjun.sleepdownschedule.feature.todo

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaomanjun.sleepdownschedule.model.TodoItemEntity
import com.xiaomanjun.sleepdownschedule.model.TodoQuadrant
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.delay

private val QuadrantAccents = mapOf(
    TodoQuadrant.IMPORTANT_URGENT to Color(0xFFFF453A),
    TodoQuadrant.IMPORTANT_NOT_URGENT to Color(0xFFF5A524),
    TodoQuadrant.URGENT_NOT_IMPORTANT to Color(0xFF2FB344),
    TodoQuadrant.NEITHER to Color(0xFF0A84FF)
)

private fun weekdayLabel(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "一"
    DayOfWeek.TUESDAY -> "二"
    DayOfWeek.WEDNESDAY -> "三"
    DayOfWeek.THURSDAY -> "四"
    DayOfWeek.FRIDAY -> "五"
    DayOfWeek.SATURDAY -> "六"
    DayOfWeek.SUNDAY -> "日"
}

/**
 * 待办页：2×2 象限均分可用高度，格内独立滚动；完成后原地灰字划线、不隐藏、不弹撤销条。
 * 删除才移除并给 3 秒撤销。
 */
@Composable
internal fun TodoPage(
    items: List<TodoItemEntity>,
    bottomInset: Dp,
    modifier: Modifier = Modifier,
    onSave: (TodoItemEntity) -> Unit,
    onToggleDone: (TodoItemEntity) -> Unit,
    onDelete: (TodoItemEntity) -> Unit,
    onClearCompleted: () -> Unit
) {
    val zone = remember { ZoneId.systemDefault() }
    var today by remember { mutableStateOf(LocalDate.now(zone)) }
    LaunchedEffect(Unit) {
        while (true) {
            today = LocalDate.now(zone)
            delay(60_000)
        }
    }
    var editorTarget by remember { mutableStateOf<TodoItemEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var filterDate by remember { mutableStateOf<LocalDate?>(null) }
    var showCompleted by remember { mutableStateOf(false) }
    var undoItem by remember { mutableStateOf<TodoItemEntity?>(null) }

    val visible = remember(items, filterDate) {
        if (filterDate == null) items else items.filter { parseIsoDate(it.dueDate) == filterDate }
    }
    val grouped = remember(visible) { groupTodoItems(visible) }
    val unfinished = unfinishedCount(items)
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("待办", fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showCompleted = true }) { Text("已完成") }
            }
            Text(
                text = "${today.monthValue}月${today.dayOfMonth}日 周${weekdayLabel(today.dayOfWeek)} · 共 $unfinished 项待办",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (offset in 0..6) {
                    val date = weekStart.plusDays(offset.toLong())
                    val selected = date == filterDate
                    val count = items.count { !it.isDone && parseIsoDate(it.dueDate) == date }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                            .clickable { filterDate = if (selected) null else date }
                            .padding(vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            weekdayLabel(date.dayOfWeek), fontSize = 10.5.sp,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${date.dayOfMonth}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            Modifier.size(4.dp).clip(CircleShape).background(
                                if (count == 0) Color.Transparent
                                else if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .padding(top = 10.dp, bottom = bottomInset + 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuadrantCard(
                        TodoQuadrant.IMPORTANT_URGENT, grouped.getValue(TodoQuadrant.IMPORTANT_URGENT),
                        today, zone, Modifier.weight(1f).fillMaxHeight(),
                        onToggleDone, { editorTarget = it }, { onDelete(it); undoItem = it }
                    )
                    QuadrantCard(
                        TodoQuadrant.IMPORTANT_NOT_URGENT, grouped.getValue(TodoQuadrant.IMPORTANT_NOT_URGENT),
                        today, zone, Modifier.weight(1f).fillMaxHeight(),
                        onToggleDone, { editorTarget = it }, { onDelete(it); undoItem = it }
                    )
                }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuadrantCard(
                        TodoQuadrant.URGENT_NOT_IMPORTANT, grouped.getValue(TodoQuadrant.URGENT_NOT_IMPORTANT),
                        today, zone, Modifier.weight(1f).fillMaxHeight(),
                        onToggleDone, { editorTarget = it }, { onDelete(it); undoItem = it }
                    )
                    QuadrantCard(
                        TodoQuadrant.NEITHER, grouped.getValue(TodoQuadrant.NEITHER),
                        today, zone, Modifier.weight(1f).fillMaxHeight(),
                        onToggleDone, { editorTarget = it }, { onDelete(it); undoItem = it }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = bottomInset + 12.dp)
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable { creating = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "新建待办", tint = MaterialTheme.colorScheme.onPrimary)
        }

        undoItem?.let { removed ->
            LaunchedEffect(removed.id) {
                delay(3000)
                undoItem = null
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomInset + 14.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("已删除「${removed.title}」", fontSize = 12.sp)
                Spacer(Modifier.width(12.dp))
                TextButton(onClick = {
                    onSave(removed)
                    undoItem = null
                }) { Text("撤销") }
            }
        }
    }

    val editing = editorTarget
    if (creating || editing != null) {
        TodoEditorDialog(
            initial = editing,
            today = today,
            onDismiss = { creating = false; editorTarget = null },
            onSave = { item ->
                onSave(item)
                creating = false
                editorTarget = null
            }
        )
    }

    if (showCompleted) {
        val completed = items.filter { it.isDone }.sortedByDescending { it.doneAt }
        AlertDialog(
            onDismissRequest = { showCompleted = false },
            title = { Text("已完成") },
            text = {
                if (completed.isEmpty()) {
                    Text("还没有完成的待办")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                        items(completed, key = { it.id }) { item ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        item.title, fontSize = 14.sp, maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textDecoration = TextDecoration.LineThrough,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    item.doneAt?.let {
                                        Text(
                                            todoDoneLabel(it, today, zone), fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                TextButton(onClick = {
                                    onToggleDone(item)
                                    showCompleted = false
                                }) { Text("恢复") }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onClearCompleted()
                    showCompleted = false
                }) { Text("清空已完成") }
            },
            dismissButton = { TextButton(onClick = { showCompleted = false }) { Text("关闭") } }
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuadrantCard(
    quadrant: TodoQuadrant,
    items: List<TodoItemEntity>,
    today: LocalDate,
    zone: ZoneId,
    modifier: Modifier,
    onToggleDone: (TodoItemEntity) -> Unit,
    onEdit: (TodoItemEntity) -> Unit,
    onDelete: (TodoItemEntity) -> Unit
) {
    val accent = QuadrantAccents.getValue(quadrant)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                quadrant.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
            Text(
                "${unfinishedCount(items)}", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.width(24.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(accent))
        Spacer(Modifier.height(8.dp))
        if (items.isEmpty()) {
            Text(
                "暂无待办", fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    TodoRow(item, today, zone, onToggleDone, onEdit, onDelete)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TodoRow(
    item: TodoItemEntity,
    today: LocalDate,
    zone: ZoneId,
    onToggleDone: (TodoItemEntity) -> Unit,
    onEdit: (TodoItemEntity) -> Unit,
    onDelete: (TodoItemEntity) -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    val done = item.isDone
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = { onEdit(item) }, onLongClick = { menu = true }),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (done) muted else Color.Transparent)
                    .border(1.5.dp, if (done) muted else muted.copy(alpha = 0.6f), CircleShape)
                    .clickable { onToggleDone(item) },
                contentAlignment = Alignment.Center
            ) {
                if (done) {
                    Icon(
                        Icons.Rounded.Check, contentDescription = "取消完成",
                        tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                    color = if (done) muted.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurface
                )
                val meta = if (done) {
                    item.doneAt?.let { todoDoneLabel(it, today, zone) }
                } else {
                    todoDueMeta(item, today)?.text
                }
                if (meta != null) {
                    val level = if (done) null else todoDueMeta(item, today)?.level
                    Text(
                        meta, fontSize = 12.sp,
                        color = when (level) {
                            TodoDueLevel.OVERDUE -> Color(0xFFFF453A)
                            TodoDueLevel.TODAY -> MaterialTheme.colorScheme.primary
                            else -> muted
                        }
                    )
                }
            }
            IconButton(onClick = { menu = true }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "更多", tint = muted, modifier = Modifier.size(16.dp))
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("编辑") }, onClick = { menu = false; onEdit(item) })
            DropdownMenuItem(text = { Text("删除") }, onClick = { menu = false; onDelete(item) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoEditorDialog(
    initial: TodoItemEntity?,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (TodoItemEntity) -> Unit
) {
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var note by remember(initial) { mutableStateOf(initial?.note.orEmpty()) }
    var quadrant by remember(initial) { mutableStateOf(initial?.quadrantValue ?: TodoQuadrant.IMPORTANT_URGENT) }
    var dueDate by remember(initial) { mutableStateOf(parseIsoDate(initial?.dueDate)) }
    var pickingDate by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新建待办" else "编辑待办") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { if (it.length <= 80) title = it },
                    label = { Text("标题") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                for (row in TodoQuadrant.entries.chunked(2)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (value in row) {
                            FilterChip(
                                selected = quadrant == value,
                                onClick = { quadrant = value },
                                label = { Text(value.label, fontSize = 10.sp) }
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        dueDate?.let { "${it.monthValue}月${it.dayOfMonth}日" } ?: "无截止",
                        fontSize = 13.sp, modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { dueDate = today }) { Text("今天") }
                    TextButton(onClick = { dueDate = today.plusDays(1) }) { Text("明天") }
                    TextButton(onClick = { dueDate = null }) { Text("清除") }
                }
                TextButton(onClick = { pickingDate = true }) { Text("选择日期") }
                OutlinedTextField(
                    value = note, onValueChange = { if (it.length <= 500) note = it },
                    label = { Text("备注") }, maxLines = 3, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    onSave(
                        (initial ?: TodoItemEntity(title = title)).copy(
                            title = title.trim(),
                            note = note.trim(),
                            quadrant = quadrant.name,
                            dueDate = dueDate?.toString()
                        )
                    )
                }
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )

    if (pickingDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (dueDate ?: today).toEpochDay() * 86_400_000L
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        dueDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    pickingDate = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("取消") } }
        ) { DatePicker(state = state) }
    }
}
