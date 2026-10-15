package com.xiaomanjun.sleepdownschedule.feature.agent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.R
import com.xiaomanjun.sleepdownschedule.ScheduleConfigEntity
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.AddMenuAction
import com.xiaomanjun.sleepdownschedule.core.ui.material.LocalHomeMaterialControls
import com.xiaomanjun.sleepdownschedule.core.ui.settings.SleepDownLiquidCascadingPopup
import com.xiaomanjun.sleepdownschedule.core.ui.settings.SleepDownLiquidMenuItem

/** Composed inside the plus button so both popup implementations use its real anchor. */
@Composable
internal fun AgentAttachmentPopup(
    show: Boolean, imagesEnabled: Boolean, filesEnabled: Boolean,
    config: ScheduleConfigEntity, backdrop: Backdrop?, foreground: Color,
    onDismiss: () -> Unit, onImage: () -> Unit, onFile: () -> Unit
) {
    val actions = buildList {
        if (imagesEnabled) add(AddMenuAction(iconRes = R.drawable.ic_image_attachment, label = "选择图片",
            onClick = { onDismiss(); onImage() }))
        if (filesEnabled) add(AddMenuAction(iconRes = R.drawable.ic_ai_import, label = "导入课表文件",
            onClick = { onDismiss(); onFile() }))
    }
    val anchorSize = with(LocalDensity.current) { 40.dp.toPx() }
    val anchor = Rect(0f, 0f, anchorSize, anchorSize)
    val session = remember(show) { Any() }
    if (show && actions.isNotEmpty() && LocalHomeMaterialControls.current.ShortcutMenu(
            session, anchor, foreground.luminance() > 0.5f, actions, closing = false,
            onDismiss = onDismiss, bindDismiss = {}
        )) return
    SleepDownLiquidCascadingPopup(
        show = show && actions.isNotEmpty(), anchorBounds = anchor,
        items = actions.mapIndexed { index, action -> SleepDownLiquidMenuItem(
            key = "attachment_$index", text = action.label, iconRes = action.iconRes, onClick = action.onClick) },
        onDismissRequest = onDismiss, backdrop = backdrop, config = config,
        contentColor = foreground, panelMinWidth = 184.dp
    )
}
