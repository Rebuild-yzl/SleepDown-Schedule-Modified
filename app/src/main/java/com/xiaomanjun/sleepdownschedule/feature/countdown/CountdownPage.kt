package com.xiaomanjun.sleepdownschedule.feature.countdown

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.xiaomanjun.sleepdownschedule.core.ui.settings.SettingsDatePickerContent
import com.xiaomanjun.sleepdownschedule.core.ui.settings.SettingsDivider
import com.xiaomanjun.sleepdownschedule.core.ui.settings.SettingsNavigationRow
import com.xiaomanjun.sleepdownschedule.core.ui.settings.SettingsSwipeDeleteRow
import com.xiaomanjun.sleepdownschedule.core.ui.settings.SettingsTimePickerContent
import com.xiaomanjun.sleepdownschedule.core.ui.settings.SettingsToggleRow
import com.xiaomanjun.sleepdownschedule.core.ui.text.CourseCardText
import com.xiaomanjun.sleepdownschedule.feature.home.LocalHomePaneVisible
import com.xiaomanjun.sleepdownschedule.feature.home.day.HomeReadableText
import com.xiaomanjun.sleepdownschedule.feature.home.day.homeForegroundColor
import com.xiaomanjun.sleepdownschedule.glass.ui.CourseGlassCard
import com.kyant.shapes.RoundedRectangle
import com.xiaomanjun.sleepdownschedule.model.CountdownEventEntity
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.delay

/**
 * 倒数日页：沿用二级页顶栏与分组卡。Hero 显示最近一个未过期事件的剩余时间（有时刻按秒走），
 * 下面是卡片列表；卡片天数按日历日差，过期项显示「已过 N 天」并沉到列表末尾。
 */
@Composable
internal fun CountdownPage(
    events: List<CountdownEventEntity>,
    config: ScheduleConfigEntity,
    backdrop: Backdrop?,
    bottomInset: Dp,
    composerPending: Boolean,
    onComposerShown: () -> Unit,
    onSave: (CountdownEventEntity) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val zone = remember { ZoneId.systemDefault() }
    var today by remember { mutableStateOf(LocalDate.now(zone)) }
    var editorTarget by remember { mutableStateOf<CountdownEventEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<CountdownEventEntity?>(null) }
    // 预热中的隐藏页不走秒表；真正可见时才挂载，避免无人查看时每秒重组整页。
    val paneVisible = LocalHomePaneVisible.current
    val now by produceState(initialValue = LocalDateTime.now(zone), paneVisible.value) {
        if (!paneVisible.value) return@produceState
        while (true) {
            value = LocalDateTime.now(zone)
            delay(1_000)
        }
    }
    LaunchedEffect(paneVisible.value) {
        if (!paneVisible.value) return@LaunchedEffect
        today = LocalDate.now(zone)
        while (true) {
            delay(60_000)
            today = LocalDate.now(zone)
        }
    }
    // Dock 上的共用加号通过菜单请求新建；菜单完全收起后才挂载编辑器。
    LaunchedEffect(composerPending) {
        if (!composerPending) return@LaunchedEffect
        creating = true
        onComposerShown()
    }

    val sorted = remember(events, today, now.hour, now.minute) {
        sortCountdownEvents(events, today, now)
    }
    val hero = remember(events, today, now.hour, now.minute) {
        heroCountdownEvent(events, today, now)
    }
    val heroDate = hero?.let { countdownDate(it, today, now) }
    val dialogBackdrop = LocalCenteredDialogSceneBackdrop.current
    val textColor = homeForegroundColor(config)

    // 页内页面：不铺自己的底色，壁纸与切换动效都由首页宿主提供。
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HomeReadableText(
                    text = "倒数日",
                    color = textColor,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 6.dp,
                    bottom = bottomInset + 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "countdown-hero") {
                    HeroPanel(hero, heroDate, today, now, config, backdrop, textColor)
                }
                if (sorted.isEmpty()) {
                    item(key = "countdown-empty") {
                        HomeReadableText(
                            text = "还没有倒数日，点 Dock 上的加号添加",
                            color = textColor.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                        )
                    }
                } else {
                    items(sorted, key = { it.id }) { event ->
                        CountdownCard(
                            event = event,
                            today = today,
                            now = now,
                            zone = zone,
                            config = config,
                            backdrop = backdrop,
                            ink = textColor,
                            onEdit = { editorTarget = event },
                            onDelete = { deleteTarget = event }
                        )
                    }
                }
            }
        }
    }

    val editing = editorTarget
    if (creating || editing != null) {
        CountdownEditorDialog(
            initial = editing,
            today = today,
            backdrop = dialogBackdrop,
            config = config,
            onDismiss = { creating = false; editorTarget = null },
            onSave = { event ->
                onSave(event)
                creating = false
                editorTarget = null
            }
        )
    }

    deleteTarget?.let { target ->
        LiquidAlertDialog(
            title = "删除倒数日",
            message = "确定要删除「${target.title}」吗？",
            actions = listOf(
                LiquidAlertAction("取消", LiquidAlertActionStyle.Cancel) { deleteTarget = null },
                LiquidAlertAction("删除", LiquidAlertActionStyle.Destructive) {
                    deleteTarget = null
                    onDelete(target.id)
                }
            ),
            backdrop = dialogBackdrop,
            config = config,
            onDismissRequest = { deleteTarget = null }
        )
    }
}

@Composable
private fun HeroPanel(
    event: CountdownEventEntity?,
    date: LocalDate?,
    today: LocalDate,
    now: LocalDateTime,
    config: ScheduleConfigEntity,
    backdrop: Backdrop?,
    ink: Color
) {
    CourseGlassCard(
        backdrop = backdrop,
        config = config,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedRectangle(26.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (event == null || date == null) {
                CourseCardText(
                    "近期没有倒计时",
                    color = ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                CourseCardText(
                    "过去的事件在下面的列表里",
                    color = ink.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
                return@Column
            }
            CourseCardText(
                event.title,
                color = ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            val moment = countdownMoment(event, date)
            if (event.targetTime == null) {
                val days = countdownDays(today, date)
                Row(verticalAlignment = Alignment.Bottom) {
                    CourseCardText(
                        "${days}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                    CourseCardText(
                        " 天",
                        color = ink.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            } else {
                val remaining = Duration.between(now, moment).seconds.coerceAtLeast(0L)
                val d = remaining / 86_400
                val h = (remaining % 86_400) / 3_600
                val m = (remaining % 3_600) / 60
                val s = remaining % 60
                CourseCardText(
                    text = buildString {
                        if (d > 0) append("${d} 天 ")
                        append(String.format("%02d:%02d:%02d", h, m, s))
                    },
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(6.dp))
            CourseCardText(
                text = buildString {
                    append("${date.monthValue}月${date.dayOfMonth}日")
                    event.targetTime?.let { append(" · ${it}") }
                    if (event.repeatYearly) append(" · 每年")
                },
                color = ink.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun CountdownCard(
    event: CountdownEventEntity,
    today: LocalDate,
    now: LocalDateTime,
    zone: ZoneId,
    config: ScheduleConfigEntity,
    backdrop: Backdrop?,
    ink: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val date = countdownDate(event, today, now)
    val days = date?.let { countdownDays(today, it) }
    val past = days != null && days < 0
    val accent = if (past) ink.copy(alpha = 0.62f) else MaterialTheme.colorScheme.primary
    SettingsSwipeDeleteRow(
        rowKey = event.id,
        onRequestDelete = onDelete,
        softAppearance = true
    ) {
        // 与课程卡同一张玻璃，材质设置（颜色 / 透明度 / 模糊 / 折射 / 字体）随之生效。
        CourseGlassCard(
            backdrop = backdrop,
            config = config,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedRectangle(22.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEdit)
                    .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (event.pinned) {
                            CourseCardText(
                                "置顶",
                                color = accent,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }
                        CourseCardText(
                            event.title,
                            color = ink,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    CourseCardText(
                        text = buildString {
                            date?.let { append("${it.monthValue}月${it.dayOfMonth}日") }
                                ?: append("日期无效")
                            event.targetTime?.let { append(" · ${it}") }
                            if (event.repeatYearly) append(" · 每年重复")
                        },
                        color = ink.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    CourseCardText(
                        text = when {
                            days == null -> "—"
                            past -> "${-days}"
                            else -> "${days}"
                        },
                        color = accent,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    CourseCardText(
                        text = if (past) "已过 · 天" else "还有 · 天",
                        color = ink.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

private enum class CountdownEditorPage { DETAILS, DATE, TIME }

@Composable
private fun CountdownEditorDialog(
    initial: CountdownEventEntity?,
    today: LocalDate,
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    onDismiss: () -> Unit,
    onSave: (CountdownEventEntity) -> Unit
) {
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var targetDate by remember(initial) {
        mutableStateOf(parseTargetDate(initial ?: CountdownEventEntity(title = "", targetDate = today.toString())) ?: today)
    }
    var allDay by remember(initial) { mutableStateOf(initial?.targetTime == null) }
    var timeMinute by remember(initial) {
        mutableIntStateOf(
            parseIsoTime(initial?.targetTime)?.let { it.hour * 60 + it.minute } ?: (9 * 60)
        )
    }
    var repeatYearly by remember(initial) { mutableStateOf(initial?.repeatYearly ?: false) }
    var pinned by remember(initial) { mutableStateOf(initial?.pinned ?: false) }
    var page by remember { mutableStateOf(CountdownEditorPage.DETAILS) }
    var pickerDate by remember { mutableStateOf(targetDate) }
    val timeText = "%02d:%02d".format(timeMinute / 60, timeMinute % 60)
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant

    SleepDownPickerDialog(
        show = true,
        title = when (page) {
            CountdownEditorPage.DETAILS ->
                if (initial == null) "新建倒数日" else "编辑倒数日"
            CountdownEditorPage.DATE -> "选择目标日期"
            CountdownEditorPage.TIME -> "选择目标时间"
        },
        onDismissRequest = {
            if (page == CountdownEditorPage.DETAILS) onDismiss() else page = CountdownEditorPage.DETAILS
        },
        backdrop = backdrop,
        config = config,
        contentPadding = PaddingValues(SleepDownDesignTokens.QuickSheet.PickerContentPadding),
        contentTransitionKey = page,
        scrollableContent = true,
        smoothContentResize = true,
        bottomActions = {
            if (page == CountdownEditorPage.DETAILS) {
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
                            (initial ?: CountdownEventEntity(title = "", targetDate = targetDate.toString()))
                                .copy(
                                    title = title.trim(),
                                    targetDate = targetDate.toString(),
                                    targetTime = if (allDay) null else timeText,
                                    repeatYearly = repeatYearly,
                                    pinned = pinned
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
                        "返回", true, backdrop, config,
                        modifier = Modifier.weight(1f),
                        height = SleepDownDesignTokens.CenteredDialog.ActionHeight
                    ) { page = CountdownEditorPage.DETAILS }
                    QuickSheetLiquidAction(
                        "确定", true, backdrop, config,
                        modifier = Modifier.weight(1f),
                        primary = true,
                        height = SleepDownDesignTokens.CenteredDialog.ActionHeight
                    ) {
                        when (page) {
                            CountdownEditorPage.DATE -> targetDate = pickerDate
                            CountdownEditorPage.TIME -> Unit
                            else -> Unit
                        }
                        page = CountdownEditorPage.DETAILS
                    }
                }
            }
        },
        contentForState = { state ->
            when (state as CountdownEditorPage) {
                CountdownEditorPage.DETAILS -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DialogCapsuleField(
                        value = title,
                        onValueChange = { if (it.length <= 60) title = it },
                        placeholder = "纪念什么日子",
                        config = config,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    )
                    SettingsNavigationRow(
                        title = "目标日期",
                        subtitle = "${targetDate.year}年${targetDate.monthValue}月${targetDate.dayOfMonth}日",
                        onClick = {
                            pickerDate = targetDate
                            page = CountdownEditorPage.DATE
                        }
                    )
                    SettingsDivider()
                    SettingsToggleRow(
                        title = "全天",
                        subtitle = "只按日期倒计时",
                        checked = allDay,
                        backdrop = backdrop,
                        onCheckedChange = { allDay = it }
                    )
                    if (!allDay) {
                        SettingsDivider()
                        SettingsNavigationRow(
                            title = "目标时间",
                            subtitle = timeText,
                            onClick = { page = CountdownEditorPage.TIME }
                        )
                    }
                    SettingsDivider()
                    SettingsToggleRow(
                        title = "每年重复",
                        subtitle = "按今年的这一天重新计算",
                        checked = repeatYearly,
                        backdrop = backdrop,
                        onCheckedChange = { repeatYearly = it }
                    )
                    SettingsDivider()
                    SettingsToggleRow(
                        title = "置顶",
                        subtitle = "固定在列表最前面",
                        checked = pinned,
                        backdrop = backdrop,
                        onCheckedChange = { pinned = it }
                    )
                }
                CountdownEditorPage.DATE -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                CountdownEditorPage.TIME -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        timeText,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    SettingsTimePickerContent(
                        value = timeMinute,
                        range = 0..1439
                    ) { timeMinute = it }
                }
            }
        },
        content = {}
    )
}
