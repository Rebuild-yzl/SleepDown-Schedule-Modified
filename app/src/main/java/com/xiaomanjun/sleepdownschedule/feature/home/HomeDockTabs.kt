package com.xiaomanjun.sleepdownschedule.feature.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.catalog.components.LiquidBottomTab
import com.kyant.backdrop.catalog.components.LiquidBottomTabs
import com.xiaomanjun.sleepdownschedule.R
import com.xiaomanjun.sleepdownschedule.ScheduleConfigEntity
import com.xiaomanjun.sleepdownschedule.core.ui.chrome.HomeLightGlassSelectedAccentColor
import com.xiaomanjun.sleepdownschedule.core.ui.chrome.HomeLightGlassSurfaceColor
import com.xiaomanjun.sleepdownschedule.core.ui.chrome.homeChromeBlur
import com.xiaomanjun.sleepdownschedule.core.ui.chrome.homeChromeGlassSurfaceAlpha
import com.xiaomanjun.sleepdownschedule.core.ui.material.LocalHomeMaterialControls
import com.xiaomanjun.sleepdownschedule.glass.ui.GlassPill

// 五项固定顺序：视图 / 课程表 / 待办 / 倒数日 / 设置。「今日」只是改名与隐藏，实现仍在 HomeMode.Day。
private val HomeDockEntries = listOf(
    R.drawable.ic_day_view to "视图",
    R.drawable.ic_week_view to "课程表",
    R.drawable.ic_todo_quadrant to "待办",
    R.drawable.ic_countdown_event to "倒数日",
    R.drawable.ic_settings to "设置"
)

/** Both phone modes use the same Dock cells and material; parallel mode adds the shared add button. */
@Composable
internal fun HomeDockTabs(
    selectedIndex: Int,
    config: ScheduleConfigEntity,
    backdrop: Backdrop?,
    lightGlass: Boolean,
    modifier: Modifier = Modifier,
    dockMetrics: HomeDockMetrics? = null,
    containerSurfaceEnabled: Boolean = true,
    onSelect: (Int) -> Unit
) {
    val tabs = HomeDockEntries
    // Height, cell ratio, icon and label scale follow the tab count (and the caller's width budget);
    // the add button reuses the same height.
    val metrics = dockMetrics ?: remember(tabs.size) { HomeDockMetrics.forCount(tabs.size) }
    if (LocalHomeMaterialControls.current.Dock(tabs, selectedIndex, dark = !lightGlass,
            modifier = modifier, onSelect = onSelect)) return
    if (backdrop == null) {
        GlassPill(backdrop = null, config = config, modifier = modifier) {
            Row(
                Modifier.height(metrics.height).padding(metrics.edgePadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, (icon, label) ->
                    DockItem(selectedIndex == index, null, config, icon, label, metrics) { onSelect(index) }
                }
            }
        }
        return
    }
    LiquidBottomTabs(
        selectedTabIndex = { selectedIndex }, onTabSelected = onSelect,
        backdrop = backdrop, tabsCount = tabs.size, modifier = modifier,
        containerHeight = metrics.height, indicatorHeight = metrics.itemHeight,
        blurRadius = homeChromeBlur(1.3.dp, config),
        containerAlpha = homeChromeGlassSurfaceAlpha(lightGlass), lensHeight = 10.dp, lensAmount = 40.dp,
        indicatorWidthOverflow = metrics.edgePadding * 2, indicatorHeightOverflow = metrics.edgePadding,
        indicatorLensHeight = 12.dp, indicatorLensAmount = 17.dp,
        officialHighlightAlpha = 0.07f, officialShadowAlpha = 0.05f, officialInnerShadowAlpha = 0.08f,
        chromaticAberrationEnabled = true, isLightThemeOverride = lightGlass,
        lightContainerColor = HomeLightGlassSurfaceColor, lightAccentColor = HomeLightGlassSelectedAccentColor,
        useOfficialGlassParameters = true, containerSurfaceEnabled = containerSurfaceEnabled
    ) {
        tabs.forEachIndexed { index, (icon, label) ->
            LiquidBottomTab(onClick = { onSelect(index) }) {
                DockTabContent(icon, label, metrics.iconSize, metrics.labelFontSize)
            }
        }
    }
}
