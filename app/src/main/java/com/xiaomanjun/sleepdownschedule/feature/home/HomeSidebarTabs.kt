package com.xiaomanjun.sleepdownschedule.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.catalog.components.LiquidBottomTab
import com.kyant.backdrop.catalog.components.LiquidBottomTabs
import com.kyant.shapes.RoundedRectangle
import com.xiaomanjun.sleepdownschedule.ScheduleConfigEntity
import com.xiaomanjun.sleepdownschedule.core.ui.chrome.HomeLightGlassSelectedAccentColor
import com.xiaomanjun.sleepdownschedule.core.ui.chrome.HomeLightGlassSurfaceColor
import com.xiaomanjun.sleepdownschedule.core.ui.chrome.homeChromeBlur
import com.xiaomanjun.sleepdownschedule.core.ui.chrome.homeChromeGlassSurfaceAlpha
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalNativeCapsuleButtonRenderer
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalNativeMaterialSurfaceRenderer
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalNativeSegmentRenderer
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.NativeCapsuleButtonRequest
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.NativeMaterialSurfaceRequest
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.NativeSegmentRequest
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Sidebar

/** Compact HD navigation shares the sidebar's destinations and keeps its 48 dp frame. */
@Composable
internal fun HomeSidebarTabBar(
    tabs: List<Pair<HomeSidebarDestination, String>>,
    selected: HomeSidebarDestination,
    enabled: Boolean,
    moving: Boolean,
    lightGlass: Boolean,
    ink: Color,
    config: ScheduleConfigEntity,
    backdrop: Backdrop,
    modifier: Modifier,
    onNavigate: (HomeSidebarDestination) -> Unit,
    onExpand: () -> Unit
) {
    Box(modifier.height(48.dp)) {
        val nativeTabs = LocalNativeSegmentRenderer.current?.invoke(
            NativeSegmentRequest(
                labels = tabs.map { it.second },
                selected = tabs.indexOfFirst { it.first == selected }.coerceAtLeast(0),
                onSelected = { onNavigate(tabs[it].first) },
                modifier = Modifier.fillMaxWidth().padding(start = 36.dp, top = 4.dp, bottom = 4.dp),
                dark = !lightGlass,
                height = 40.dp,
                enabled = enabled
            )
        ) == true
        if (nativeTabs) {
            // The expanding sidebar already supplies the material while both controls crossfade.
            if (!moving) LocalNativeMaterialSurfaceRenderer.current?.invoke(
                NativeMaterialSurfaceRequest(
                    modifier = Modifier.matchParentSize().zIndex(-1f),
                    shape = RoundedRectangle(24.dp),
                    dark = !lightGlass
                )
            ) {}
            HomeSidebarIconButton(
                description = "展开侧栏", dark = !lightGlass, enabled = enabled,
                modifier = Modifier.width(36.dp).align(Alignment.CenterStart),
                height = 48.dp, onClick = onExpand
            ) {
                Icon(MiuixIcons.Sidebar, null, tint = ink, modifier = Modifier.size(20.dp))
            }
        } else {
            CompositionLocalProvider(LocalContentColor provides ink) {
                LiquidBottomTabs(
                    selectedTabIndex = { tabs.indexOfFirst { it.first == selected }.coerceAtLeast(0) },
                    onTabSelected = { index -> onNavigate(tabs[index].first) },
                    backdrop = backdrop, tabsCount = tabs.size,
                    modifier = Modifier.fillMaxWidth(),
                    containerHeight = 48.dp, indicatorHeight = 40.dp,
                    blurRadius = homeChromeBlur(1.3.dp, config),
                    containerAlpha = homeChromeGlassSurfaceAlpha(lightGlass),
                    lensHeight = 10.dp, lensAmount = 40.dp,
                    indicatorWidthOverflow = 8.dp, indicatorHeightOverflow = 4.dp,
                    indicatorLensHeight = 12.dp, indicatorLensAmount = 17.dp,
                    officialHighlightAlpha = 0.07f, officialShadowAlpha = 0.05f,
                    officialInnerShadowAlpha = 0.08f, chromaticAberrationEnabled = true,
                    isLightThemeOverride = lightGlass,
                    lightContainerColor = HomeLightGlassSurfaceColor,
                    lightAccentColor = HomeLightGlassSelectedAccentColor,
                    useOfficialGlassParameters = true,
                    containerSurfaceEnabled = !moving,
                    leadingWidth = 36.dp,
                    leadingContent = {
                        Box(Modifier.width(36.dp).fillMaxHeight()
                            .clickable(enabled = enabled, role = Role.Button,
                                interactionSource = remember { MutableInteractionSource() }, indication = null
                            ) { onExpand() }
                            .semantics { contentDescription = "展开侧栏" },
                            contentAlignment = Alignment.Center) {
                            Icon(MiuixIcons.Sidebar, null, tint = ink, modifier = Modifier.size(20.dp))
                        }
                    }
                ) {
                    tabs.forEach { (destination, label) ->
                        LiquidBottomTab(onClick = { onNavigate(destination) }) {
                            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun HomeSidebarIconButton(
    description: String,
    dark: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    height: Dp,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val described = modifier.semantics { contentDescription = description }
    if (LocalNativeCapsuleButtonRenderer.current?.invoke(
        NativeCapsuleButtonRequest(
            onClick = onClick, modifier = described, height = height,
            contentPadding = PaddingValues(0.dp), dark = dark, enabled = enabled
        )
    ) { content() } == true) return
    Box(
        described.height(height).clip(RoundedRectangle(12.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}
