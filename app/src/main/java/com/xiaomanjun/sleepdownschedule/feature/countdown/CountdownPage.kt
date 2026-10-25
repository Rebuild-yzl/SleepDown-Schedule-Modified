package com.xiaomanjun.sleepdownschedule.feature.countdown

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaomanjun.sleepdownschedule.model.CountdownEventEntity
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.delay

/**
 * 倒数日页：Hero 显示最近一个未过期事件的剩余时间（有时刻就按秒走），下面是卡片列表。
 * 卡片天数按日历日差，过期项显示「已过 N 天」并沉到列表末尾。
 */
@Composable
internal fun CountdownPage(
    events: List<CountdownEventEntity>,
    bottomInset: Dp,
    modifier: Modifier = Modifier,
    onSave: (CountdownEventEntity) -> Unit,
    onDelete: (Long) -> Unit
) {
    val zone = remember { ZoneId.systemDefault() }
    var today by remember { mutableStateOf(LocalDate.now(zone)) }
    var editorTarget by remember { mutableStateOf<CountdownEventEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    val now by produceState(initialValue = LocalDateTime.now(zone)) {
        while (true) {
            value = LocalDateTime.now(zone)
            delay(1_000)
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            today = LocalDate.now(zone)
            delay(60_000)
        }
    }

    val sorted = remember(events, today) { sortCountdownEvents(events, today) }
    val hero = remember(events, today) { heroCountdownEvent(events, today) }
    val heroDate = hero?.let { countdownDate(it, today) }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("倒数日", fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
            }
            HeroPanel(hero, heroDate, today, now)
            if (sorted.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "还没有倒数日，点右下角的加号添加",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = 4.dp, bottom = bottomInset + 88.dp
                    )
                ) {
                    items(sorted, key = { it.id }) { event ->
                        CountdownCard(event, today, zone, onEdit = { editorTarget = event }, onDelete = { onDelete(event.id) })
                    }
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
            Icon(Icons.Rounded.Add, contentDescription = "新建倒数日", tint = MaterialTheme.colorScheme.onPrimary)
        }
    }

    val editing = editorTarget
    if (creating || editing != null) {
        CountdownEditorDialog(
            initial = editing,
            today = today,
            onDismiss = { creating = false; editorTarget = null },
            onSave = { event ->
                onSave(event)
                creating = false
                editorTarget = null
            }
        )
    }
}

@Composable
private fun HeroPanel(
    event: CountdownEventEntity?,
    date: LocalDate?,
    today: LocalDate,
    now: LocalDateTime
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(vertical = 18.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (event == null || date == null) {
            Text("近期没有倒计时", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "过去的事件在下面的列表里",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }
        Text(
            event.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        val moment = countdownMoment(event, date)
        if (event.targetTime == null) {
            val days = countdownDays(today, date)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "$days", fontSize = 34.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    " 天", fontSize = 17.sp, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        } else {
            val remaining = Duration.between(now, moment).seconds.coerceAtLeast(0L)
            val d = remaining / 86_400
            val h = (remaining % 86_400) / 3_600
            val m = (remaining % 3_600) / 60
            val s = remaining % 60
            Text(
                text = buildString {
                    if (d > 0) append("$d 天 ")
                    append(String.format("%02d:%02d:%02d", h, m, s))
                },
                fontSize = 30.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = buildString {
                append("${date.monthValue}月${date.dayOfMonth}日")
                event.targetTime?.let { append(" · $it") }
                if (event.repeatYearly) append(" · 每年")
            },
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CountdownCard(
    event: CountdownEventEntity,
    today: LocalDate,
    zone: ZoneId,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    val date = countdownDate(event, today)
    val days = date?.let { countdownDays(today, it) }
    val past = days != null && days < 0
    val accent = if (past) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .combinedClickable(onClick = onEdit, onLongClick = { menu = true })
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (event.pinned) {
                        Text("置顶", fontSize = 9.5.sp, color = accent, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        event.title, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        date?.let { append("${it.monthValue}月${it.dayOfMonth}日") } ?: append("日期无效")
                        event.targetTime?.let { append(" · $it") }
                        if (event.repeatYearly) append(" · 每年重复")
                    },
                    fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = when {
                        days == null -> "—"
                        past -> "${-days}"
                        else -> "$days"
                    },
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = accent
                )
                Text(
                    text = if (past) "已过 · 天" else "还有 · 天",
                    fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { menu = true }, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Rounded.MoreVert, contentDescription = "更多",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp)
                )
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("编辑") }, onClick = { menu = false; onEdit() })
            DropdownMenuItem(text = { Text("删除") }, onClick = { menu = false; onDelete() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountdownEditorDialog(
    initial: CountdownEventEntity?,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (CountdownEventEntity) -> Unit
) {
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var date by remember(initial) { mutableStateOf(parseTargetDate(initial ?: CountdownEventEntity(title = "", targetDate = today.toString())) ?: today) }
    var allDay by remember(initial) { mutableStateOf(initial?.targetTime == null) }
    var timeText by remember(initial) { mutableStateOf(initial?.targetTime ?: "09:00") }
    var repeatYearly by remember(initial) { mutableStateOf(initial?.repeatYearly ?: false) }
    var pinned by remember(initial) { mutableStateOf(initial?.pinned ?: false) }
    var pickingDate by remember { mutableStateOf(false) }
    val parsedTime = parseIsoTime(timeText)
    val valid = title.isNotBlank() && (allDay || parsedTime != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新建倒数日" else "编辑倒数日") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { if (it.length <= 60) title = it },
                    label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${date.monthValue}月${date.dayOfMonth}日", fontSize = 13.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { date = today }) { Text("今天") }
                    TextButton(onClick = { date = today.plusDays(1) }) { Text("明天") }
                }
                TextButton(onClick = { pickingDate = true }) { Text("选择日期") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("全天", fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Switch(checked = allDay, onCheckedChange = { allDay = it })
                }
                if (!allDay) {
                    OutlinedTextField(
                        value = timeText, onValueChange = { timeText = it },
                        label = { Text("时间 HH:mm") }, singleLine = true,
                        isError = parsedTime == null, modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("每年重复", fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Switch(checked = repeatYearly, onCheckedChange = { repeatYearly = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("置顶", fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Switch(checked = pinned, onCheckedChange = { pinned = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(
                        (initial ?: CountdownEventEntity(title = "", targetDate = date.toString())).copy(
                            title = title.trim(),
                            targetDate = date.toString(),
                            targetTime = if (allDay) null else timeText,
                            repeatYearly = repeatYearly,
                            pinned = pinned
                        )
                    )
                }
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )

    if (pickingDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.toEpochDay() * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    pickingDate = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("取消") } }
        ) { DatePicker(state = state) }
    }
}
