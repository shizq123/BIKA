package com.shizq.bika.core.ui.wizard

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 向导颜色配置
 */
@Immutable
data class WizardColors(
    val progressColor: Color,
    val progressTrackColor: Color,
    val titleColor: Color,
    val subtitleColor: Color,
    val primaryButtonColor: Color,
    val completedStepColor: Color,
    val currentStepColor: Color,
    val upcomingStepColor: Color,
    val errorColor: Color
)

/**
 * 内容切换动画配置
 */
@Immutable
data class WizardContentTransition(
    val forwardEnter: EnterTransition,
    val forwardExit: ExitTransition,
    val backwardEnter: EnterTransition,
    val backwardExit: ExitTransition
) {
    fun createTransitionSpec(
        direction: NavigationDirection
    ): AnimatedContentTransitionScope<Int>.() -> ContentTransform = {
        when (direction) {
            NavigationDirection.FORWARD -> forwardEnter togetherWith forwardExit
            NavigationDirection.BACKWARD -> backwardEnter togetherWith backwardExit
            NavigationDirection.NONE -> fadeIn() togetherWith fadeOut()
        }
    }
}

/**
 * 默认配置
 */
object WizardDefaults {

    @Composable
    fun colors(
        progressColor: Color = MaterialTheme.colorScheme.primary,
        progressTrackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        titleColor: Color = MaterialTheme.colorScheme.onSurface,
        subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        primaryButtonColor: Color = MaterialTheme.colorScheme.primary,
        completedStepColor: Color = MaterialTheme.colorScheme.primary,
        currentStepColor: Color = MaterialTheme.colorScheme.primary,
        upcomingStepColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        errorColor: Color = MaterialTheme.colorScheme.error
    ): WizardColors = WizardColors(
        progressColor = progressColor,
        progressTrackColor = progressTrackColor,
        titleColor = titleColor,
        subtitleColor = subtitleColor,
        primaryButtonColor = primaryButtonColor,
        completedStepColor = completedStepColor,
        currentStepColor = currentStepColor,
        upcomingStepColor = upcomingStepColor,
        errorColor = errorColor
    )

    fun contentTransition(
        forwardEnter: EnterTransition = slideInHorizontally { it } + fadeIn(),
        forwardExit: ExitTransition = slideOutHorizontally { -it } + fadeOut(),
        backwardEnter: EnterTransition = slideInHorizontally { -it } + fadeIn(),
        backwardExit: ExitTransition = slideOutHorizontally { it } + fadeOut()
    ): WizardContentTransition = WizardContentTransition(
        forwardEnter = forwardEnter,
        forwardExit = forwardExit,
        backwardEnter = backwardEnter,
        backwardExit = backwardExit
    )
}
