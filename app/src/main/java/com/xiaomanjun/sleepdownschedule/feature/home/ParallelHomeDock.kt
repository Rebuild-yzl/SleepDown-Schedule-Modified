package com.xiaomanjun.sleepdownschedule.feature.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.catalog.utils.InteractiveHighlight
import com.xiaomanjun.sleepdownschedule.ScheduleConfigEntity
import com.xiaomanjun.sleepdownschedule.app.ui.*
import com.xiaomanjun.sleepdownschedule.core.ui.chrome.homeChromeBlur
import com.xiaomanjun.sleepdownschedule.core.ui.chrome.homeChromeGlassSurfaceAlpha
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.SleepDownFloatingAddButton
import com.xiaomanjun.sleepdownschedule.core.ui.interaction.dockImeCompensationPx
import com.xiaomanjun.sleepdownschedule.core.ui.interaction.excludeParentPullGesture
import com.xiaomanjun.sleepdownschedule.core.ui.material.LocalHomeButtonAccent
import com.xiaomanjun.sleepdownschedule.core.ui.material.LocalHomeMaterialControls
import com.xiaomanjun.sleepdownschedule.core.ui.material.LocalHomeMaterialStyle
import com.xiaomanjun.sleepdownschedule.glass.ui.LocalAdaptiveGlass
import com.xiaomanjun.sleepdownschedule.glass.ui.appUsesDarkTheme
import com.xiaomanjun.sleepdownschedule.glass.withRecordingDensity
import kotlin.math.roundToInt

internal val HomeImportButtonColor = Color(0xFF007AFF)
internal fun homeImportButtonGlassColor(light: Boolean): Color =
    HomeImportButtonColor.copy(alpha = homeChromeGlassSurfaceAlpha(light))

@Composable
internal fun homeDockImportButtonGlassColor(light: Boolean): Color {
    val nativeStyle = com.xiaomanjun.sleepdownschedule.core.ui.material.LocalHomeMaterialStyle.current != null
    val accent = com.xiaomanjun.sleepdownschedule.core.ui.material.LocalHomeButtonAccent.current
        ?.let { Color(it) }
        ?: HomeImportButtonColor
    // Native FAB's custom theme layer is opaque. Match it before blending toward the menu tint.
    return accent.copy(alpha = if (nativeStyle)
        1f else homeChromeGlassSurfaceAlpha(light))
}

/** The return endpoint is the resting button, without the captured press deformation. */
@Composable
internal fun HomeImportButtonReturnVisual(
    backdrop: Backdrop?, config: ScheduleConfigEntity, modifier: Modifier = Modifier
) {
    if (LocalHomeMaterialControls.current.FloatingButtonVisual(dark = !LocalAdaptiveGlass.current.lightGlass, modifier = modifier)) return
    SleepDownFloatingAddButton(
        backdrop = backdrop,
        contentDescription = "添加与导入",
        onClick = {},
        surfaceColor = homeDockImportButtonGlassColor(LocalAdaptiveGlass.current.lightGlass),
        blurRadius = homeChromeBlur(1.3.dp, config),
        enabled = false,
        modifier = modifier
    )
}

/** Owns only the Dock split. Menus and their destinations use the shared anchored overlay host. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ParallelHomeDock(
    selectedIndex: Int, config: ScheduleConfigEntity, backdrop: Backdrop,
    onSelect: (Int) -> Unit, onOpenMenu: (Rect, GraphicsLayer) -> Unit,
    buttonHidden: Boolean = false,
    modifier: Modifier = Modifier
) {
    // 五项里前四项（视图 / 课程表 / 待办 / 倒数日）共用同一个加号与同一套展开动效，
    // 只有设置页收起加号。homeSelected 仍表示「首页玻璃」这一层材质语义。
    val addCapable = selectedIndex < 4
    val homeSelected = selectedIndex < 2
    val adaptiveGlass = LocalAdaptiveGlass.current
    val light = if (homeSelected) adaptiveGlass.lightGlass else !appUsesDarkTheme(config)
    if (LocalHomeMaterialControls.current.ParallelDock(
            selectedIndex = selectedIndex, dark = !light, buttonHidden = buttonHidden,
            modifier = modifier, onSelect = onSelect, onOpenMenu = onOpenMenu
        )) return
    val ink by animateColorAsState(
        if (homeSelected) adaptiveGlass.contentColor else if (light) Color.Black else Color.White,
        tween(220), label = "ParallelDockInk")
    val plusSurface by animateColorAsState(
        homeDockImportButtonGlassColor(light), tween(220), label = "ParallelImportSurface")
    val split = remember { Animatable(if (addCapable) 1f else 0f) }
    val splitReady by remember { derivedStateOf { !split.isRunning && split.value >= 0.999f } }
    LaunchedEffect(addCapable) {
        val target = if (addCapable) 1f else 0f
        if (split.value == target) return@LaunchedEffect
        split.animateTo(target, tween(460, easing = HomeDropletEasing))
    }
    var buttonBounds by remember { mutableStateOf(Rect.Zero) }
    val returnButtonLayer = rememberGraphicsLayer()
    val interactionScope = rememberCoroutineScope()
    val plusInteraction = remember(interactionScope) { InteractiveHighlight(interactionScope) }
    val density = LocalDensity.current
    val recordingDensity = density
    val systemBottom = WindowInsets.navigationBarsIgnoringVisibility.getBottom(density)
    val bottomInset = with(density) { systemBottom.toDp() }
    val imeCompensation = dockImeCompensationPx(WindowInsets.ime.getBottom(density), systemBottom)
    BoxWithConstraints(modifier.fillMaxSize()) {
        // COUI's expansion view uses a 12dp edge-to-edge gap. Keep the same resting
        // spacing in liquid glass, including when a narrow window reduces the dock.
        // 条 + 间距 + 加号的总宽与页内容对齐（页内容左右各 16dp），条高与加号随之变大。
        val metrics = remember(maxWidth) {
            HomeDockMetrics.forCount(5, availableWidth = (maxWidth - 32.dp).coerceAtLeast(240.dp))
        }
        val plusSize = metrics.addButtonSize
        val plusGap = HomeDockMetrics.AddButtonGapValue.dp
        val groupWidth = metrics.width + plusGap + plusSize
        val dockWidth = metrics.width
        val dockTop = 24.dp
        val dockBounds = remember(density, groupWidth, dockWidth, metrics) {
            derivedStateOf { with(density) {
                val s = split.value.coerceIn(0f, 1f)
                val dockX = ((groupWidth - dockWidth) / 2).toPx() * (1f - s)
                Rect(dockX, dockTop.toPx(), dockX + dockWidth.toPx(), dockTop.toPx() + metrics.height.toPx())
            } }
        }
        val buttonInteractive = addCapable && splitReady && !buttonHidden
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = bottomInset + 8.dp)
            .width(groupWidth).height(dockTop + metrics.height).graphicsLayer { translationY = imeCompensation.toFloat() }) {
            // Keep the original LiquidButton mounted throughout the split. Its glass, shadow,
            // refraction and press response use the same renderer at rest and while travelling.
            // Clip its returning part behind the Dock even though the Dock itself is translucent.
            Box(Modifier.matchParentSize().drawWithContent {
                val body = dockBounds.value
                val mask = Path().apply { addRoundRect(RoundRect(body, CornerRadius(body.height / 2f))) }
                clipPath(mask, ClipOp.Difference) { this@drawWithContent.drawContent() }
            }) {
                SleepDownFloatingAddButton(onClick = {
                    if (buttonBounds.width > 1f && buttonBounds.height > 1f) {
                        onOpenMenu(buttonBounds, returnButtonLayer)
                    }
                }, backdrop = backdrop, contentDescription = "添加与导入", enabled = buttonInteractive,
                    modifier = Modifier.offset {
                        val s = split.value.coerceIn(0f, 1f)
                        val body = dockBounds.value
                        IntOffset(
                            lerp(body.right - plusSize.toPx(), (groupWidth - plusSize).toPx(), s).roundToInt(),
                            body.top.roundToInt()
                        )
                    }.graphicsLayer {
                        // 设置页没有加号：让它一边缩进条内、一边淡出，而不是整块贴在玻璃后面漏边。
                        val progress = split.value.coerceIn(0f, 1f)
                        val keep = 0.62f + 0.38f * progress
                        scaleX = keep
                        scaleY = keep
                        alpha = progress
                    }.size(plusSize)
                        .onGloballyPositioned { buttonBounds = it.boundsInRoot() }
                        .drawWithContent {
                            // Hidden disables LiquidButton's press transform. Record that resting
                            // endpoint for an eventual Activity handoff, without reading a bitmap
                            // (or recording the cropped pressed button) on the opening tap.
                            returnButtonLayer.record {
                                // Pin LocalDensity: record's delegated density otherwise recurses.
                                withRecordingDensity(recordingDensity) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                            if (!buttonHidden) drawLayer(returnButtonLayer)
                        }
                        .then(if (buttonInteractive) plusInteraction.gestureModifier else Modifier)
                        .excludeParentPullGesture()
                        .then(if (buttonInteractive) Modifier.semantics { testTag = "parallel_import_button" }
                            else Modifier.clearAndSetSemantics {}),
                    surfaceColor = plusSurface, sharedInteractiveHighlight = plusInteraction,
                    blurRadius = homeChromeBlur(1.3.dp, config), size = plusSize)
            }
            CompositionLocalProvider(LocalContentColor provides ink) {
                HomeDockTabs(selectedIndex, config, backdrop, light, dockMetrics = metrics,
                    modifier = Modifier.offset {
                        IntOffset((((groupWidth - dockWidth) / 2).toPx() * (1f - split.value)).roundToInt(), dockTop.roundToPx())
                    }.width(dockWidth).excludeParentPullGesture().semantics { testTag = "parallel_home_dock" },
                    onSelect = onSelect)
            }
        }
    }
}
