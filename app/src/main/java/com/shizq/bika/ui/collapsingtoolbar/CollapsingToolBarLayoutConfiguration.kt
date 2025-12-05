package com.shizq.bika.ui.collapsingtoolbar

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf

/**
 * @param collapsingWhenTopConfiguration
 * internal configuration for CollapsingOption.collapsingWhenTop is true.
 *
 */
@Stable
class CollapsingToolBarLayoutConfiguration(
    val collapsingWhenTopConfiguration: CollapsingWhenTopConfiguration = CollapsingWhenTopConfiguration()
) {
    /**
     * @param useInternalStretchEffectOnTop
     * in collapsingWhenTop case, LocalOverscrollConfiguration is disabled in Android 12 or higher due to consuming scroll event issue.
     * Therefore, This parameter should be set to true for using Stretch Effect when pull down on the top of list.
     * only available in Android 12 or higher.
     *
     * @param useInternalStretchEffectOnBottom
     * in collapsingWhenTop case, LocalOverscrollConfiguration is disabled in Android 12 or higher due to consuming scroll event issue.
     * Therefore, This parameter should be set to true for using Stretch Effect when pull up on the bottom of list.
     * only available in Android 12 or higher.
     *
     */
    @Stable
    class CollapsingWhenTopConfiguration(
        val useInternalStretchEffectOnTop: Boolean = true,
        val useInternalStretchEffectOnBottom: Boolean = true
    )
}


val LocalCollapsingToolBarLayoutConfiguration = compositionLocalOf {
    CollapsingToolBarLayoutConfiguration()
}