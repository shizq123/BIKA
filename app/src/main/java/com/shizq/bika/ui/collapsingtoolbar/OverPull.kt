package com.shizq.bika.ui.collapsingtoolbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity

internal fun Modifier.topOverPull(
    state: TopOverPullState,
    enabled: Boolean = true,
    consume: Boolean = false
) = then(Modifier.topOverPull(state::onPull, state::onRelease, enabled, consume))

internal fun Modifier.bottomOverPull(
    state: BottomOverPullState,
    enabled: Boolean = true,
    consume: Boolean = false
) = then(Modifier.bottomOverPull(state::onPull, state::onRelease, enabled, consume))

internal fun Modifier.topOverPull(
    onPull: (pullDelta: Float) -> Float,
    onRelease: suspend (flingVelocity: Float) -> Float,
    enabled: Boolean = true,
    consume: Boolean = false
) = then(
    Modifier.nestedScroll(
        TopOverPullNestedScrollConnection(
            onPull,
            onRelease,
            enabled,
            consume
        )
    )
)

internal fun Modifier.bottomOverPull(
    onPull: (pullDelta: Float) -> Float,
    onRelease: suspend (flingVelocity: Float) -> Float,
    enabled: Boolean = true,
    consume: Boolean = false
) = then(
    Modifier.nestedScroll(
        BottomOverPullNestedScrollConnection(
            onPull,
            onRelease,
            enabled,
            consume
        )
    )
)

private class TopOverPullNestedScrollConnection(
    private val onPull: (pullDelta: Float) -> Float,
    private val onRelease: suspend (flingVelocity: Float) -> Float,
    private val enabled: Boolean,
    private val consume: Boolean
) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = when {
        !enabled -> Offset.Zero
        source == NestedScrollSource.UserInput && available.y < 0 -> {
            val pullConsumed = onPull(available.y)
            if (consume) Offset(0f, pullConsumed) else Offset.Zero
        } // Swiping up
        else -> Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset = when {
        !enabled -> Offset.Zero
        source == NestedScrollSource.UserInput && available.y > 0 -> {
            val pullConsumed = onPull(available.y)
            if (consume) Offset(0f, pullConsumed) else Offset.Zero
        } // Pulling down
        else -> Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val releaseConsumed = onRelease(available.y)
        return if (consume) Velocity(0f, releaseConsumed) else Velocity.Zero
    }
}

private class BottomOverPullNestedScrollConnection(
    private val onPull: (pullDelta: Float) -> Float,
    private val onRelease: suspend (flingVelocity: Float) -> Float,
    private val enabled: Boolean,
    private val consume: Boolean
) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = when {
        !enabled -> Offset.Zero
        source == NestedScrollSource.UserInput && available.y > 0 -> {
            val pullConsumed = onPull(available.y)
            if (consume) Offset(0f, pullConsumed) else Offset.Zero
        } // Swiping down
        else -> Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset = when {
        !enabled -> Offset.Zero
        source == NestedScrollSource.UserInput && available.y < 0 -> {
            val pullConsumed = onPull(available.y)
            if (consume) Offset(0f, pullConsumed) else Offset.Zero
        } // Pulling up
        else -> Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val releaseConsumed = onRelease(available.y)
        return if (consume) Velocity(0f, releaseConsumed) else Velocity.Zero
    }
}

@Composable
internal fun rememberTopOverPullState(
    distanceMultiplier: Float = 1f
): TopOverPullState {
    require(distanceMultiplier > 0f) { "The distanceMultiplier must be greater than zero!" }
    val state = remember(Unit) {
        TopOverPullState(distanceMultiplier)
    }
    return state
}

@Composable
internal fun rememberBottomOverPullState(
    distanceMultiplier: Float = 1f
): BottomOverPullState {
    require(distanceMultiplier > 0f) { "The distanceMultiplier must be greater than zero!" }
    val state = remember(Unit) {
        BottomOverPullState(distanceMultiplier)
    }
    return state
}

internal class BottomOverPullState internal constructor(private val distanceMultiplier: Float = 1f) {
    private var distancePulled by mutableFloatStateOf(0f)
    val overPullDistance by derivedStateOf { distancePulled * distanceMultiplier }

    internal fun onPull(originPullDelta: Float): Float {
        val pullDelta = -originPullDelta
        val newOffset = (distancePulled + pullDelta).coerceAtLeast(0f)
        val dragConsumed = newOffset - distancePulled
        distancePulled = newOffset
        return -dragConsumed
    }

    internal fun onRelease(velocity: Float): Float {
        val consumed = when {
            distancePulled == 0f -> 0f
            velocity > 0f -> 0f
            else -> velocity
        }
        distancePulled = 0f
        return consumed
    }
}

internal class TopOverPullState internal constructor(private val distanceMultiplier: Float = 1f) {
    private var distancePulled by mutableFloatStateOf(0f)
    val overPullDistance by derivedStateOf { distancePulled * distanceMultiplier }

    internal fun onPull(originPullDelta: Float): Float {
        val newOffset = (distancePulled + originPullDelta).coerceAtLeast(0f)
        val dragConsumed = newOffset - distancePulled
        distancePulled = newOffset
        return dragConsumed
    }

    internal fun onRelease(velocity: Float): Float {
        val consumed = when {
            distancePulled == 0f -> 0f
            velocity < 0f -> 0f
            else -> velocity
        }
        distancePulled = 0f
        return consumed
    }
}