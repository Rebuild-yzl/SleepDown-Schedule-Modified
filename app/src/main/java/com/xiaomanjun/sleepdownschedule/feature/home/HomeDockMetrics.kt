package com.xiaomanjun.sleepdownschedule.feature.home

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * One source of truth for the Dock's resting geometry.
 *
 * The bar height, the shared capsule radius and the floating add button all derive from the same
 * cell ratio, so a tab count change can never leave the button taller than the bar it parks
 * behind. Five tabs is the designed compact bar; fewer tabs grow slightly instead of keeping a
 * fixed size that no longer matches the cells.
 */
@Immutable
internal data class HomeDockMetrics(
    val itemCount: Int,
    val height: Dp,
    val itemWidth: Dp,
    val edgePadding: Dp,
    val iconSize: Dp,
    val labelFontSize: TextUnit
) {
    val width: Dp get() = itemWidth * itemCount + edgePadding * 2
    val cornerRadius: Dp get() = height / 2
    /** The add button shares the bar's height, diameter and capsule radius. */
    val addButtonSize: Dp get() = height
    val itemHeight: Dp get() = height - edgePadding * 2

    companion object {
        private const val ReferenceCount = 5
        private const val ReferenceHeight = 46f
        private const val GrowthPerMissingTab = 4f
        private const val ItemAspect = 50f / 46f

        fun forCount(count: Int): HomeDockMetrics {
            val items = count.coerceAtLeast(1)
            val height = (
                ReferenceHeight + GrowthPerMissingTab * (ReferenceCount - items).coerceAtLeast(0)
                ).dp
            return HomeDockMetrics(
                itemCount = items,
                height = height,
                itemWidth = height * ItemAspect,
                edgePadding = 4.dp,
                iconSize = height * (18f / 46f),
                labelFontSize = (9.5f * height.value / 46f).sp
            )
        }
    }
}
