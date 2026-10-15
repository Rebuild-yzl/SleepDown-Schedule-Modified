package com.xiaomanjun.sleepdownschedule.feature.course.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.ScheduleConfigEntity
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.QuickSheetLiquidAction
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.SleepDownDesignTokens
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.SleepDownPickerDialog
import com.xiaomanjun.sleepdownschedule.core.ui.settings.SettingsPickerValueRow
import com.xiaomanjun.sleepdownschedule.core.ui.settings.SettingsTextFieldRow
import com.xiaomanjun.sleepdownschedule.core.ui.settings.SettingsToggleRow
import top.yukonga.miuix.kmp.basic.NumberPicker
import top.yukonga.miuix.kmp.basic.NumberPickerDefaults

@Composable
internal fun CourseLessonClockPicker(
    draft: CourseLessonClockDraft,
    preferredIndices: List<Int>,
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    renderInRootScaffold: Boolean,
    onChange: (CourseLessonClockDraft) -> Unit
) {
    val preview = runCatching { draft.times(preferredIndices) }.getOrNull()
    Column(
        modifier = Modifier.fillMaxWidth()
            .heightIn(max = SleepDownDesignTokens.CenteredDialog.SelectionContentMaxHeight)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        SettingsTextFieldRow("开始上课时间", draft.start, { onChange(draft.copy(start = it)) }, compact = true)
        CourseLessonCountPickerRow(
            count = draft.count.toIntOrNull()?.coerceIn(1, 40) ?: 1,
            backdrop = backdrop,
            config = config,
            renderInRootScaffold = renderInRootScaffold,
            onChange = { onChange(draft.copy(count = it.toString())) }
        )
        SettingsToggleRow("每节课分别设置", "", draft.individually, backdrop, compact = true) {
            onChange(draft.copy(individually = it))
        }
        if (!draft.individually) {
            SettingsTextFieldRow(
                "课程时长（分钟）", draft.classMinutes, { onChange(draft.copy(classMinutes = it)) },
                keyboardType = KeyboardType.Number, compact = true
            )
            SettingsTextFieldRow(
                "课间时长（分钟）", draft.breakMinutes, { onChange(draft.copy(breakMinutes = it)) },
                keyboardType = KeyboardType.Number, compact = true
            )
        } else {
            Text(
                "开始时间留空时，按上一节的课间时长自动衔接。",
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall
            )
            repeat(draft.count.toIntOrNull()?.takeIf { it in 1..40 } ?: 0) { index ->
                val row = draft.lesson(index)
                Text(
                    "第 ${index + 1} 节",
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.titleSmall
                )
                SettingsTextFieldRow(
                    "开始时间", row.startOverride,
                    { onChange(draft.withLesson(index, row.copy(startOverride = it))) },
                    keyboardType = KeyboardType.Ascii,
                    placeholder = preview?.getOrNull(index)?.start?.toString() ?: "自动", compact = true
                )
                SettingsTextFieldRow(
                    "课程时长（分钟）", row.classMinutes,
                    { onChange(draft.withLesson(index, row.copy(classMinutes = it))) },
                    keyboardType = KeyboardType.Number, compact = true
                )
                if (index < (draft.count.toIntOrNull() ?: 0) - 1) {
                    SettingsTextFieldRow(
                        "课间时长（分钟）", row.breakMinutes,
                        { onChange(draft.withLesson(index, row.copy(breakMinutes = it))) },
                        keyboardType = KeyboardType.Number, compact = true
                    )
                }
            }
        }
        preview?.let {
            Text(
                "${it.first().start} – ${it.last().end}",
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun CourseLessonCountPickerRow(
    count: Int,
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    renderInRootScaffold: Boolean,
    onChange: (Int) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    var selectedCount by remember(count, showPicker) { mutableIntStateOf(count) }
    SettingsPickerValueRow("节数", "$count 节", { showPicker = true }, compact = true)
    SleepDownPickerDialog(
        show = showPicker,
        title = "选择节数",
        onDismissRequest = { showPicker = false },
        backdrop = backdrop,
        config = config,
        renderInRootScaffold = renderInRootScaffold
    ) {
        val foreground = LocalContentColor.current
        NumberPicker(
            value = selectedCount,
            onValueChange = { selectedCount = it },
            range = 1..40,
            visibleItemCount = 3,
            label = { "$it 节" },
            colors = NumberPickerDefaults.colors(
                selectedTextColor = foreground,
                unselectedTextColor = foreground.copy(alpha = 0.34f)
            ),
            textStyle = MaterialTheme.typography.titleLarge.copy(color = foreground),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SleepDownDesignTokens.Dialog.ActionSpacing)
        ) {
            QuickSheetLiquidAction(
                "取消", true, backdrop, config, modifier = Modifier.weight(1f),
                height = SleepDownDesignTokens.CenteredDialog.ActionHeight
            ) { showPicker = false }
            QuickSheetLiquidAction(
                "确定", true, backdrop, config, primary = true, modifier = Modifier.weight(1f),
                height = SleepDownDesignTokens.CenteredDialog.ActionHeight
            ) {
                onChange(selectedCount)
                showPicker = false
            }
        }
    }
}
