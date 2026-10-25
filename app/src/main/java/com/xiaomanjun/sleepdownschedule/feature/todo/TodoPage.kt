package com.xiaomanjun.sleepdownschedule.feature.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.ScheduleConfigEntity
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.DialogButtonRole
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.DialogCapsuleField
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.DialogLiquidButton
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LiquidAlertAction
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LiquidAlertActionStyle
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LiquidAlertDialog
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalCenteredDialogSceneBackdrop
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.QuickSheetLiquidAction
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.SleepDownDesignTokens
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.SleepDownPickerDialog
import com.xiaomanjun.sleepdownschedule.core.ui.scaffold.DetailTopBar
import com.xiaomanjun.sleepdownschedule.core.ui.scaffold.settingsPageBackground
import com.xiaomanjun.sleepdownschedule.core.ui.settings.SettingsDatePickerContent
import com.xiaomanjun.sleepdownschedule.core.ui.settings.SettingsGroup
import com.xiaomanjun.sleepdownschedule.core.ui.settings.SettingsNavigationRow
import com.xiaomanjun.sleepdownschedule.core.ui.settings.SettingsSwipeDeleteRow
import com.xiaomanjun.sleepdownschedule.model.TodoItemEntity
import com.xiaomanjun.sleepdownschedule.model.TodoQuadrant
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
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
 * 待办页：沿用二级页的顶栏、分组卡与居中弹层，2×2 象限均分可用高度、格内独立滚动。
 * 完成后原地灰字划线，本次刚勾选的项不会立刻沉到格尾；加号使用 Dock 上的共用控件。
 */
@Composable
internal fun TodoPage(
    items: List<TodoItemEntity>,
    config: ScheduleConfigEntity,
    bottomInset: Dp,
    composerRequest: Int,
    onSave: (TodoItemEntity) -> Unit,
    onToggleDone: (TodoItemEntity) -> Unit,
    onDelete: (TodoItemEntity) -> Unit,
    onClearCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val zone = remember { ZoneId.systemDefault() }
    var today by remember { mutableStateOf(LocalDate.now(zone)) }
    LaunchedEffect(Unit) {
        while (true) {
            today = LocalDate.now(zone)
            delay(60_000)
        }
    }
    // 进入页面时的完成快照：本次新勾选的项仍按未完成的位置排序，行不会自己滑走。
    val settledDoneIds = remember { items.filter { it.isDone }.mapTo(HashSet()) { it.id } }

    var editorTarget by remember { mutableStateOf<TodoItemEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<TodoItemEntity?>(null) }
    var filterDate by remember { mutableStateOf<LocalDate?>(null) }
    var showCompleted by remember { mutableStateOf(false) }

    // Dock 上的共用加号通过菜单请求新建；菜单完全收起后才挂载编辑器。
    LaunchedEffect(composerRequest) {
        if (composerRequest > 0) creating = true
    }

    val visible = remember(items, filterDate) {
        if (filterDate == null) items else items.filter { parseIsoDate(it.dueDate) == filterDate }
    }
    val grouped = remember(visible, settledDoneIds) { groupTodoItems(visible, settledDoneIds) }
    val unfinished = unfinishedCount(items)
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val dialogBackdrop = LocalCenteredDialogSceneBackdrop.current

    Box(modifier.fillMaxSize().background(settingsPageBackground(config))) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            DetailTopBar(
                title = "待办",
                config = config,
                backdrop = null,
                onBack = {},
                showBackButton = false,
                statusTopOverride = 0.dp,
                backButtonStartPadding = 16.dp,
                actions = {
                    Box(Modifier.padding(end = 10.dp)) {
                        DialogLiquidButton(
                            backdrop = null,
                            label = "已完成",
                            onClick = { showCompleted = true },
                            role = DialogButtonRole.Neutral,
                            height = 34.dp,
                            horizontalPadding = 14.dp
                        )
                    }
                }
            )
            Text(
                text = "${today.monthValue}月${today.dayOfMonth}日 周${weekdayLabel(today.dayOfWeek)} · 共 ${unfinished} 项待办",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (offset in 0..6) {
                    val date = weekStart.plusDays(offset.toLong())
                    val selected = date == filterDate
                    val count = items.count { !it.isDone && parseIsoDate(it.dueDate) == date }
                    val accent = MaterialTheme.colorScheme.primary
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) accent.copy(alpha = 0.14f) else Color.Transparent)
                            .clickable { filterDate = if (selected) null else date }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            weekdayLabel(date.dayOfWeek), fontSize = 10.5.sp,
                            color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${date.dayOfMonth}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                            color = if (selected) accent else MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            Modifier.padding(top = 2.dp).size(4.dp).clip(CircleShape).background(
                                if (count == 0) Color.Transparent else accent
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
                        today, zone, config, Modifier.weight(1f).fillMaxHeight(),
                        onToggleDone, { editorTarget = it }, { deleteTarget = it }
                    )
                    QuadrantCard(
                        TodoQuadrant.IMPORTANT_NOT_URGENT, grouped.getValue(TodoQuadrant.IMPORTANT_NOT_URGENT),
                        today, zone, config, Modifier.weight(1f).fillMaxHeight(),
                        onToggleDone, { editorTarget = it }, { deleteTarget = it }
                    )
                }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuadrantCard(
                        TodoQuadrant.URGENT_NOT_IMPORTANT, grouped.getValue(TodoQuadrant.URGENT_NOT_IMPORTANT),
                        today, zone, config, Modifier.weight(1f).fillMaxHeight(),
                        onToggleDone, { editorTarget = it }, { deleteTarget = it }
                    )
                    QuadrantCard(
                        TodoQuadrant.NEITHER, grouped.getValue(TodoQuadrant.NEITHER),
                        today, zone, config, Modifier.weight(1f).fillMaxHeight(),
                        onToggleDone, { editorTarget = it }, { deleteTarget = it }
                    )
                }
            }
        }
    }

    val editing = editorTarget
    if (creating || editing != null) {
        TodoEditorDialog(
            initial = editing,
            today = today,
            backdrop = dialogBackdrop,
            config = config,
            onDismiss = { creating = false; editorTarget = null },
            onSave = { item ->
                onSave(item)
                creating = false
                editorTarget = null
            }
        )
    }

    deleteTarget?.let { target ->
        LiquidAlertDialog(
            title = "删除待办",
            message = "确定要删除「${target.title}」吗？",
            actions = listOf(
                LiquidAlertAction("取消", LiquidAlertActionStyle.Cancel) { deleteTarget = null },
                LiquidAlertAction("删除", LiquidAlertActionStyle.Destructive) {
                    deleteTarget = null
                    onDelete(target)
                }
            ),
            backdrop = dialogBackdrop,
            config = config,
            onDismissRequest = { deleteTarget = null }
        )
    }

    if (showCompleted) {
        val completed = items.filter { it.isDone }.sortedByDescending { it.doneAt }
        LiquidAlertDialog(
            title = "已完成",
            message = "还没有完成的待办",
            actions = listOf(
                LiquidAlertAction("清空已完成", LiquidAlertActionStyle.Destructive) {
                    showCompleted = false
                    onClearCompleted()
                },
                LiquidAlertAction("关闭", LiquidAlertActionStyle.Cancel) { showCompleted = false }
            ),
            backdrop = dialogBackdrop,
            config = config,
            onDismissRequest = { showCompleted = false },
            scrollableMessageContent = true,
            messageContent = if (completed.isEmpty()) null else {
                {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (item in completed) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        item.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textDecoration = TextDecoration.LineThrough,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    item.doneAt?.let { doneAt ->
                                        Text(
                                            todoDoneLabel(doneAt, today, zone),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                DialogLiquidButton(
                                    backdrop = null,
                                    label = "恢复",
                                    onClick = { onToggleDone(item) },
                                    role = DialogButtonRole.Neutral,
                                    height = 32.dp,
                                    horizontalPadding = 12.dp
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun QuadrantCard(
    quadrant: TodoQuadrant,
    items: List<TodoItemEntity>,
    today: LocalDate,
    zone: ZoneId,
    config: ScheduleConfigEntity,
    modifier: Modifier,
    onToggleDone: (TodoItemEntity) -> Unit,
    onEdit: (TodoItemEntity) -> Unit,
    onDelete: (TodoItemEntity) -> Unit
) {
    val accent = QuadrantAccents.getValue(quadrant)
    SettingsGroup(backdrop = null, config = config, modifier = modifier) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    quadrant.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${unfinishedCount(items)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                Modifier.padding(start = 14.dp, top = 6.dp)
                    .width(22.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent)
            )
            Spacer(Modifier.height(4.dp))
            if (items.isEmpty()) {
                Text(
                    "暂无待办",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(bottom = 6.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        TodoRow(item, today, zone, onToggleDone, onEdit, onDelete)
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoRow(
    item: TodoItemEntity,
    today: LocalDate,
    zone: ZoneId,
    onToggleDone: (TodoItemEntity) -> Unit,
    onEdit: (TodoItemEntity) -> Unit,
    onDelete: (TodoItemEntity) -> Unit
) {
    val done = item.isDone
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    SettingsSwipeDeleteRow(
        rowKey = item.id,
        onRequestDelete = { onDelete(item) },
        softAppearance = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEdit(item) }
                .padding(start = 14.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (done) muted else Color.Transparent)
                    .border(
                        1.5.dp,
                        if (done) Color.Transparent else muted.copy(alpha = 0.55f),
                        CircleShape
                    )
                    .clickable { onToggleDone(item) },
                contentAlignment = Alignment.Center
            ) {
                if (done) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "取消完成",
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.padding(3.dp)
                    )
                }
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                    color = if (done) muted.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurface
                )
                val meta = if (done) item.doneAt?.let { todoDoneLabel(it, today, zone) }
                else todoDueMeta(item, today)?.text
                if (meta != null) {
                    val level = if (done) null else todoDueMeta(item, today)?.level
                    Text(
                        meta,
                        fontSize = 11.sp,
                        color = when (level) {
                            TodoDueLevel.OVERDUE -> Color(0xFFFF453A)
                            TodoDueLevel.TODAY -> MaterialTheme.colorScheme.primary
                            else -> muted
                        }
                    )
                }
            }
        }
    }
}

private enum class TodoEditorPage { DETAILS, DATE }

@Composable
private fun TodoEditorDialog(
    initial: TodoItemEntity?,
    today: LocalDate,
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    onDismiss: () -> Unit,
    onSave: (TodoItemEntity) -> Unit
) {
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var note by remember(initial) { mutableStateOf(initial?.note.orEmpty()) }
    var quadrant by remember(initial) { mutableStateOf(initial?.quadrantValue ?: TodoQuadrant.IMPORTANT_URGENT) }
    var dueDate by remember(initial) { mutableStateOf(parseIsoDate(initial?.dueDate)) }
    var page by remember { mutableStateOf(TodoEditorPage.DETAILS) }
    var pickerDate by remember { mutableStateOf(dueDate ?: today) }
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant

    SleepDownPickerDialog(
        show = true,
        title = when (page) {
            TodoEditorPage.DETAILS -> if (initial == null) "新建待办" else "编辑待办"
            TodoEditorPage.DATE -> "选择截止日期"
        },
        onDismissRequest = {
            if (page == TodoEditorPage.DETAILS) onDismiss() else page = TodoEditorPage.DETAILS
        },
        backdrop = backdrop,
        config = config,
        contentPadding = PaddingValues(SleepDownDesignTokens.QuickSheet.PickerContentPadding),
        contentTransitionKey = page,
        scrollableContent = true,
        smoothContentResize = true,
        bottomActions = {
            if (page == TodoEditorPage.DETAILS) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SleepDownDesignTokens.Dialog.ActionSpacing)
                ) {
                    QuickSheetLiquidAction(
                        "取消", true, backdrop, config,
                        modifier = Modifier.weight(1f),
                        height = SleepDownDesignTokens.CenteredDialog.ActionHeight
                    ) { onDismiss() }
                    QuickSheetLiquidAction(
                        "保存", title.isNotBlank(), backdrop, config,
                        modifier = Modifier.weight(1f),
                        primary = true,
                        height = SleepDownDesignTokens.CenteredDialog.ActionHeight
                    ) {
                        onSave(
                            (initial ?: TodoItemEntity(title = title)).copy(
                                title = title.trim(),
                                note = note.trim(),
                                quadrant = quadrant.name,
                                dueDate = dueDate?.toString()
                            )
                        )
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SleepDownDesignTokens.Dialog.ActionSpacing)
                ) {
                    QuickSheetLiquidAction(
                        "清除", dueDate != null, backdrop, config,
                        modifier = Modifier.weight(1f),
                        height = SleepDownDesignTokens.CenteredDialog.ActionHeight
                    ) {
                        dueDate = null
                        page = TodoEditorPage.DETAILS
                    }
                    QuickSheetLiquidAction(
                        "返回", true, backdrop, config,
                        modifier = Modifier.weight(1f),
                        height = SleepDownDesignTokens.CenteredDialog.ActionHeight
                    ) { page = TodoEditorPage.DETAILS }
                    QuickSheetLiquidAction(
                        "确定", true, backdrop, config,
                        modifier = Modifier.weight(1f),
                        primary = true,
                        height = SleepDownDesignTokens.CenteredDialog.ActionHeight
                    ) {
                        dueDate = pickerDate
                        page = TodoEditorPage.DETAILS
                    }
                }
            }
        },
        contentForState = { state ->
            when (state as TodoEditorPage) {
                TodoEditorPage.DETAILS -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DialogCapsuleField(
                        value = title,
                        onValueChange = { if (it.length <= 80) title = it },
                        placeholder = "要做什么",
                        config = config,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("象限", style = MaterialTheme.typography.labelMedium, color = contentColor)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (row in TodoQuadrant.entries.chunked(2)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (value in row) {
                                    QuickSheetLiquidAction(
                                        label = value.label,
                                        enabled = true,
                                        backdrop = backdrop,
                                        config = config,
                                        modifier = Modifier.weight(1f),
                                        primary = quadrant == value,
                                        height = 38.dp
                                    ) { quadrant = value }
                                }
                            }
                        }
                    }
                    SettingsNavigationRow(
                        title = "截止日期",
                        subtitle = dueDate?.let { date ->
                            "${date.monthValue}月${date.dayOfMonth}日 · " +
                                (todoDueMeta(
                                    (initial ?: TodoItemEntity(title = "", dueDate = date.toString()))
                                        .copy(dueDate = date.toString()),
                                    today
                                )?.text.orEmpty())
                        } ?: "无截止日期",
                        onClick = {
                            pickerDate = dueDate ?: today
                            page = TodoEditorPage.DATE
                        }
                    )
                    DialogCapsuleField(
                        value = note,
                        onValueChange = { if (it.length <= 500) note = it },
                        placeholder = "备注（可选）",
                        config = config,
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                TodoEditorPage.DATE -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "${pickerDate.monthValue}月${pickerDate.dayOfMonth}日",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    SettingsDatePickerContent(
                        year = pickerDate.year,
                        month = pickerDate.monthValue,
                        day = pickerDate.dayOfMonth,
                        onYearChange = { pickerDate = pickerDate.withYear(it) },
                        onMonthChange = { pickerDate = pickerDate.withMonth(it) },
                        onDayChange = { day ->
                            pickerDate = pickerDate.withDayOfMonth(
                                day.coerceIn(1, pickerDate.lengthOfMonth())
                            )
                        }
                    )
                }
            }
        },
        content = {}
    )
}
