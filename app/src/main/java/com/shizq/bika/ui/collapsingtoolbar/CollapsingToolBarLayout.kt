package com.shizq.bika.ui.collapsingtoolbar

import android.os.Build
import androidx.compose.animation.SplineBasedFloatDecayAnimationSpec
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastSumBy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class CollapsingOption(
    val collapsingWhenTop: Boolean,
    val isAutoSnap: Boolean
) {
    data object EnterAlways : CollapsingOption(collapsingWhenTop = false, isAutoSnap = false)
    data object EnterAlwaysCollapsed :
        CollapsingOption(collapsingWhenTop = true, isAutoSnap = false)

    data object EnterAlwaysAutoSnap : CollapsingOption(collapsingWhenTop = false, isAutoSnap = true)
    data object EnterAlwaysCollapsedAutoSnap :
        CollapsingOption(collapsingWhenTop = true, isAutoSnap = true)

    companion object {
        private val optionList by lazy {
            listOf(
                EnterAlways,
                EnterAlwaysCollapsed,
                EnterAlwaysAutoSnap,
                EnterAlwaysCollapsedAutoSnap
            )
        }

        fun toIndex(option: CollapsingOption): Int = optionList.indexOf(option)
        fun toOption(id: Int): CollapsingOption = optionList[id]
    }
}

@Stable
data class ToolBarCollapsedInfo(
    val progress: Float,
    val toolBarHeight: Dp
)

class CollapsingToolBarLayoutToolBarScope(
    private val toolBarState: CollapsingToolBarState,
    val collapsedInfo: ToolBarCollapsedInfo
) {
    fun Modifier.toolBarScrollable(): Modifier =
        this.composed {
            scrollable(
                orientation = Orientation.Vertical,
                state = rememberScrollableState { it },
                flingBehavior = object : FlingBehavior {
                    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                        return if (toolBarState.collapsingOption.isAutoSnap) {
                            val centerPx =
                                (toolBarState.toolBarMaxHeightPx + toolBarState.toolBarMinHeightPx) / 2
                            val toolBarHeightPx = toolBarState.toolBarHeightPx
                            toolBarState.snapToolBar(toolBarHeightPx >= centerPx)
                            0f
                        } else {
                            if (initialVelocity < 0) {
                                toolBarState.flingY(initialVelocity)
                                0f
                            } else {
                                initialVelocity
                            }
                        }
                    }
                },
                enabled = (!toolBarState.collapsingOption.collapsingWhenTop || collapsedInfo.progress < 1f)
            )
        }

    fun Modifier.requiredToolBarMaxHeight(maxHeight: Dp = toolBarState.toolBarMaxHeight): Modifier =
        this
            .fillMaxHeight()
            .requiredHeight(maxHeight)
            .offset(y = -(toolBarState.toolBarMaxHeight - collapsedInfo.toolBarHeight) / 2)
}

class CollapsingToolBarLayoutContentScope(
    private val state: CollapsingToolBarState
) {
    suspend fun ScrollableState.scrollWithToolBarBy(value: Float) {
        val consumedOffset = state.onPreScroll(Offset(0f, -value))
        val consumed = scrollBy(value + consumedOffset.y)
        state.onPostScroll(Offset(0f, -consumed), Offset(0f, -(value - consumed)))
    }

    suspend fun ScrollableState.animateScrollWithToolBarBy(
        value: Float,
        animationSpec: AnimationSpec<Float> = spring()
    ) {
        val millisToNanos = 1_000_000L
        val duration = 8L //8ms
        val vectorConverter = Float.VectorConverter
        val vectorAnimationSpec = animationSpec.vectorize(vectorConverter)
        vectorConverter.convertToVector(0f)
        val durationNanos = vectorAnimationSpec.getDurationNanos(
            vectorConverter.convertToVector(0f),
            vectorConverter.convertToVector(value),
            vectorConverter.convertToVector(0f)
        )
        var playTimeNanos = 0L
        var prevValue = 0f
        while (playTimeNanos < durationNanos) {
            val valueVector = vectorAnimationSpec.getValueFromNanos(
                playTimeNanos,
                vectorConverter.convertToVector(0f),
                vectorConverter.convertToVector(value),
                vectorConverter.convertToVector(0f)
            )
            val newValue = vectorConverter.convertFromVector(valueVector)
            val currentOffset = newValue - prevValue
            scrollWithToolBarBy(currentOffset)
            delay(duration)
            playTimeNanos += duration * millisToNanos
            prevValue = newValue
        }
    }

    suspend fun LazyListState.animateScrollWithToolBarToItem(index: Int, scrollOffset: Int = 0) {
        var expectedCount = ANIMATE_SCROLL_TIME / ANIMATE_SCROLL_DURATION
        while (expectedCount > 0) {
            if (firstVisibleItemIndex == index && firstVisibleItemScrollOffset == scrollOffset) break
            val expectedDistance = expectedDistanceTo(index, scrollOffset)
            if (expectedDistance == 0) break
            val expectedFrameDistance = expectedDistance.toFloat() / expectedCount
            expectedCount -= 1
            scrollWithToolBarBy(expectedFrameDistance)
            delay(ANIMATE_SCROLL_DURATION)
        }
    }

    private fun LazyListState.expectedDistanceTo(index: Int, targetScrollOffset: Int): Int {
        val visibleItems = layoutInfo.visibleItemsInfo
        val averageSize = visibleItems.fastSumBy { it.size } / visibleItems.size
        val indexesDiff = index - firstVisibleItemIndex
        return (averageSize * indexesDiff) + targetScrollOffset - firstVisibleItemScrollOffset
    }

    companion object {
        private const val ANIMATE_SCROLL_DURATION = 4L
        private const val ANIMATE_SCROLL_TIME = 100L
    }
}


@Stable
class CollapsingToolBarState(
    private val density: Density,
    val toolBarMaxHeight: Dp,
    val toolBarMinHeight: Dp,
    val collapsingOption: CollapsingOption
) {
    var progress: Float by mutableFloatStateOf(0f)
        internal set
    var contentOffset: Float by mutableFloatStateOf(0f)
        internal set
    internal var toolbarOffsetHeightPx: Float by mutableFloatStateOf(0f)

    internal val toolBarMaxHeightPx: Int = with(density) { toolBarMaxHeight.roundToPx() }
    internal val toolBarMinHeightPx: Int = with(density) { toolBarMinHeight.roundToPx() }
    val toolBarHeight by derivedStateOf {
        toolBarMaxHeight - ((toolBarMaxHeight - toolBarMinHeight) * progress)
    }
    internal val toolBarHeightPx by derivedStateOf {
        with(density) { toolBarHeight.roundToPx() }
    }
    private val toolbarHeightRangePx by derivedStateOf { toolBarMaxHeightPx - toolBarMinHeightPx }

    suspend fun snapToolBar(isExpand: Boolean) {
        val beginValue = toolBarHeightPx.toFloat()
        val finishValue =
            if (isExpand) toolBarMaxHeightPx.toFloat() else toolBarMinHeightPx.toFloat()
        var prevValue = beginValue
        animate(beginValue, finishValue, animationSpec = spring()) { currentValue, _ ->
            val diff = -(prevValue - currentValue)
            if (collapsingOption.collapsingWhenTop) {
                contentOffset -= diff
                if (contentOffset < 0f) {
                    contentOffset = 0f
                }
            }
            toolbarOffsetHeightPx =
                (toolbarOffsetHeightPx - diff).coerceIn(0f, toolbarHeightRangePx.toFloat())
            progress =
                if (toolbarHeightRangePx > 0) 1f - ((toolbarHeightRangePx - toolbarOffsetHeightPx) / toolbarHeightRangePx) else 0f
            prevValue = currentValue
        }
    }

    private fun consumeScrollHeight(availableY: Float): Float {
        val nextToolbarHeightPx =
            (toolbarOffsetHeightPx - availableY).coerceIn(0f, toolbarHeightRangePx.toFloat())
        val consumedY = toolbarOffsetHeightPx - nextToolbarHeightPx
        toolbarOffsetHeightPx = nextToolbarHeightPx
        progress =
            if (toolbarHeightRangePx > 0) 1f - ((toolbarHeightRangePx - toolbarOffsetHeightPx) / toolbarHeightRangePx) else 0f
        return consumedY
    }

    internal fun onPreScroll(available: Offset): Offset {
        val directionDown = available.y < 0
        return if (directionDown) {
            Offset(
                0f,
                if (toolbarOffsetHeightPx < toolbarHeightRangePx) consumeScrollHeight(available.y) else 0f
            )
        } else {
            if (collapsingOption.collapsingWhenTop) {
                Offset(0f, if (contentOffset <= 0f) consumeScrollHeight(available.y) else 0f)
            } else {
                Offset(0f, if (toolbarOffsetHeightPx > 0) consumeScrollHeight(available.y) else 0f)
            }
        }
    }

    internal fun onPostScroll(consumed: Offset, available: Offset) {
        if (collapsingOption.collapsingWhenTop) {
            contentOffset -= consumed.y
            // Correction Logic. same as Google code.
            // Reset the total content offset to zero when scrolling all the way down. This
            // will eliminate some float precision inaccuracies.
            if (consumed.y == 0f && available.y > 0f || contentOffset < 0f) {
                contentOffset = 0f
            }
        }
    }

    internal suspend fun flingY(velocityY: Float) {
        var isDone = false
        contentOffset = 0f
        var prevValue = 0f
        animateDecay(0f, velocityY, SplineBasedFloatDecayAnimationSpec(density)) { value, _ ->
            if (!isDone) {
                val diff = value - prevValue
                prevValue = value
                val consumedOffset = onPreScroll(Offset(0f, diff))
                onPostScroll(consumedOffset, Offset.Zero)
                if (consumedOffset.y == 0f) isDone = true
            }
        }
    }

    companion object {
        val Saver: Saver<CollapsingToolBarState, *> = listSaver(
            save = {
                listOf(
                    it.density.density,
                    it.density.fontScale,
                    it.toolBarMaxHeight.value,
                    it.toolBarMinHeight.value,
                    CollapsingOption.toIndex(it.collapsingOption),
                    it.progress,
                    it.contentOffset,
                    it.toolbarOffsetHeightPx
                )
            },
            restore = {
                CollapsingToolBarState(
                    Density(it[0] as Float, it[1] as Float),
                    Dp(it[2] as Float),
                    Dp(it[3] as Float),
                    CollapsingOption.toOption(it[4] as Int)
                ).apply {
                    progress = it[5] as Float
                    contentOffset = it[6] as Float
                    toolbarOffsetHeightPx = it[7] as Float
                }
            }
        )
    }
}


@Composable
fun rememberCollapsingToolBarState(
    toolBarMaxHeight: Dp = 56.dp,
    toolBarMinHeight: Dp = 0.dp,
    collapsingOption: CollapsingOption = CollapsingOption.EnterAlwaysCollapsed
): CollapsingToolBarState {
    val density = LocalDensity.current
    return rememberSaveable(saver = CollapsingToolBarState.Saver) {
        CollapsingToolBarState(density, toolBarMaxHeight, toolBarMinHeight, collapsingOption)
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollapsingToolBarLayout(
    modifier: Modifier = Modifier,
    state: CollapsingToolBarState,
    updateToolBarHeightManually: Boolean = false,
    toolbar: @Composable CollapsingToolBarLayoutToolBarScope.() -> Unit,
    content: @Composable CollapsingToolBarLayoutContentScope.() -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val collapsingToolBarConfiguration = LocalCollapsingToolBarLayoutConfiguration.current
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            private var snapAnimationJob: Job? = null
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                snapAnimationJob?.cancel()
                return state.onPreScroll(available)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                state.onPostScroll(consumed, available)
                return super.onPostScroll(consumed, available, source)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                snapAnimationJob = coroutineScope.launch {
                    if (available.y > 0f) {
                        state.flingY(available.y)
                    } else if (available.y < 0f && state.progress < 1f) {
                        state.flingY(available.y)
                    }
                    if (state.collapsingOption.isAutoSnap) {
                        val centerPx = (state.toolBarMaxHeightPx + state.toolBarMinHeightPx) / 2
                        val toolBarHeightPx = state.toolBarHeightPx
                        state.snapToolBar(toolBarHeightPx >= centerPx)
                    }
                    snapAnimationJob = null
                }
                return if (available.y > 0f) Velocity(0f, available.y) else Velocity.Zero
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .then(modifier)
    ) {
        //ToolBar
        val toolBarCollapsedInfo = ToolBarCollapsedInfo(state.progress, state.toolBarHeight)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                .then(if (!updateToolBarHeightManually) Modifier.height(state.toolBarHeight) else Modifier)
        ) {
            CollapsingToolBarLayoutToolBarScope(state, toolBarCollapsedInfo).toolbar()
        }
        //Content
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            state.collapsingOption.collapsingWhenTop &&
            LocalOverscrollFactory.current != null
        ) {
            // BugFix
            // Since Android 12(S), Overscroll effect consumes scroll event first.
            // So, Disable Overscroll effect in >= Android 12 version case and
            // Use the Internal Stretch Effect with according to configurations.
            CompositionLocalProvider(LocalOverscrollFactory provides null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .conditional(collapsingToolBarConfiguration.collapsingWhenTopConfiguration.useInternalStretchEffectOnTop) {
                            topStretchEffect()
                        }
                        .conditional(collapsingToolBarConfiguration.collapsingWhenTopConfiguration.useInternalStretchEffectOnBottom) {
                            bottomStretchEffect()
                        }
                ) {
                    CollapsingToolBarLayoutContentScope(state).content()
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                CollapsingToolBarLayoutContentScope(state).content()
            }
        }
    }
}

private inline fun Modifier.conditional(
    condition: Boolean,
    modifier: Modifier.() -> Modifier
): Modifier {
    return if (condition) {
        then(modifier(Modifier))
    } else {
        this
    }
}

private fun Modifier.topStretchEffect(
    stretchMultiplier: Float = STRETCH_MULTIPLIER
) = composed {
    val topOverPullState = rememberTopOverPullState()
    val overStretchScale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        snapshotFlow { topOverPullState.overPullDistance.coerceAtLeast(0f) }.collect { overPullDistance ->
            val nextScale = overPullDistance * stretchMultiplier
            if (nextScale == 0f && overStretchScale.value != 0f) {
                overStretchScale.animateTo(targetValue = 0f, animationSpec = tween(250))
            } else {
                overStretchScale.snapTo(nextScale)
            }
        }
    }
    return@composed this
        .topOverPull(topOverPullState)
        .clipToBounds()
        .graphicsLayer(
            scaleY = (overStretchScale.value + 1f),
            transformOrigin = TransformOrigin(0f, 0f)
        )
}

private fun Modifier.bottomStretchEffect(
    stretchMultiplier: Float = STRETCH_MULTIPLIER
) = composed {
    val bottomOverPullState = rememberBottomOverPullState()
    val overStretchScale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        snapshotFlow { bottomOverPullState.overPullDistance.coerceAtLeast(0f) }.collect { overPullDistance ->
            val nextScale = overPullDistance * stretchMultiplier
            if (nextScale == 0f && overStretchScale.value != 0f) {
                overStretchScale.animateTo(targetValue = 0f, animationSpec = tween(250))
            } else {
                overStretchScale.snapTo(nextScale)
            }
        }
    }
    return@composed this
        .bottomOverPull(bottomOverPullState)
        .clipToBounds()
        .graphicsLayer(
            scaleY = (overStretchScale.value + 1f),
            transformOrigin = TransformOrigin(0f, 1f)
        )
}

internal const val STRETCH_MULTIPLIER = 0.000015f