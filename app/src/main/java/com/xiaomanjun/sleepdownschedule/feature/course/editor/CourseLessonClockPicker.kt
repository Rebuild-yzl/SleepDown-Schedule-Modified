package com.xiaomanjun.sleepdownschedule.feature.course.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.SleepDownDesignTokens
import com.xiaomanjun.sleepdownschedule.core.ui.settings.SettingsTextFieldRow
import com.xiaomanjun.sleepdownschedule.core.ui.settings.SettingsToggleRow

@Composable
internal fun CourseLessonClockPicker(
    draft: CourseLessonClockDraft,
    preferredIndices: List<Int>,
    backdrop: Backdrop?,
    onChange: (CourseLessonClockDraft) -> Unit
) {
    val preview = runCatching { draft.times(preferredIndices) }.getOrNull()
    Column(Modifier.fillMaxWidth().heightIn(max = SleepDownDesignTokens.CenteredDialog.SelectionContentMaxHeight)
        .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        SettingsTextFieldRow("开始上课时间", draft.start, { onChange(draft.copy(start = it)) }, compact = true)
        SettingsTextFieldRow("节数", draft.count, { onChange(draft.copy(count = it)) },
            keyboardType = KeyboardType.Number, compact = true)
        SettingsToggleRow("每节课分别设置", "", draft.individually, backdrop, compact = true) {
            onChange(draft.copy(individually = it))
        }
        if (!draft.individually) {
            SettingsTextFieldRow("课程时长（分钟）", draft.classMinutes, { onChange(draft.copy(classMinutes = it)) },
                keyboardType = KeyboardType.Number, compact = true)
            SettingsTextFieldRow("课间时长（分钟）", draft.breakMinutes, { onChange(draft.copy(breakMinutes = it)) },
                keyboardType = KeyboardType.Number, compact = true)
        } else {
            Text("开始时间留空时，按上一节的课间时长自动衔接。", style = MaterialTheme.typography.bodySmall)
            repeat(draft.count.toIntOrNull()?.takeIf { it in 1..40 } ?: 0) { index ->
                val row = draft.lesson(index)
                Text("第 ${index + 1} 节", style = MaterialTheme.typography.titleSmall)
                SettingsTextFieldRow("开始时间", row.startOverride,
                    { onChange(draft.withLesson(index, row.copy(startOverride = it))) },
                    keyboardType = KeyboardType.Ascii,
                    placeholder = preview?.getOrNull(index)?.start?.toString() ?: "自动", compact = true)
                SettingsTextFieldRow("课程时长（分钟）", row.classMinutes,
                    { onChange(draft.withLesson(index, row.copy(classMinutes = it))) },
                    keyboardType = KeyboardType.Number, compact = true)
                if (index < (draft.count.toIntOrNull() ?: 0) - 1) {
                    SettingsTextFieldRow("课间时长（分钟）", row.breakMinutes,
                        { onChange(draft.withLesson(index, row.copy(breakMinutes = it))) },
                        keyboardType = KeyboardType.Number, compact = true)
                }
            }
        }
        preview?.let { Text("${it.first().start} – ${it.last().end}", style = MaterialTheme.typography.bodySmall) }
    }
}
