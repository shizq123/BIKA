package com.shizq.bika.feature.reader.impl.util.preload

import org.junit.Test
import kotlin.test.assertEquals

class PreloadPlannerTest {
    @Test
    fun `user direction controls directional window`() {
        val planner = PreloadPlanner()

        val forward = planner.plan(
            viewport = ViewportSnapshot(
                visibleRange = 10..12,
                direction = ScrollDirection.Forward,
                cause = ViewportChangeCause.UserScroll,
            ),
            preloadCount = 3,
            itemCount = 100,
        )
        assertEquals(listOf(13, 14, 15), forward.indices)

        val backward = planner.plan(
            viewport = ViewportSnapshot(
                visibleRange = 8..10,
                direction = ScrollDirection.Backward,
                cause = ViewportChangeCause.UserScroll,
            ),
            preloadCount = 2,
            itemCount = 100,
        )
        assertEquals(listOf(7, 6), backward.indices)
    }

    @Test
    fun `programmatic jump plans both sides instead of changing direction`() {
        val planner = PreloadPlanner()
        planner.plan(
            viewport = ViewportSnapshot(
                visibleRange = 30..32,
                direction = ScrollDirection.Forward,
                cause = ViewportChangeCause.UserScroll,
            ),
            preloadCount = 2,
            itemCount = 100,
        )

        val jump = planner.plan(
            viewport = ViewportSnapshot(
                visibleRange = 70..72,
                cause = ViewportChangeCause.ProgrammaticJump,
            ),
            preloadCount = 4,
            itemCount = 100,
        )
        assertEquals(listOf(73, 69, 74, 68), jump.indices)

        val afterJump = planner.plan(
            viewport = ViewportSnapshot(
                visibleRange = 70..72,
                direction = ScrollDirection.Forward,
                cause = ViewportChangeCause.UserScroll,
            ),
            preloadCount = 2,
            itemCount = 100,
        )
        assertEquals(listOf(73, 74), afterJump.indices)
    }

    @Test
    fun `reflow does not overwrite the last trusted direction`() {
        val planner = PreloadPlanner()
        planner.plan(
            viewport = ViewportSnapshot(
                visibleRange = 20..22,
                direction = ScrollDirection.Backward,
                cause = ViewportChangeCause.UserScroll,
            ),
            preloadCount = 2,
            itemCount = 100,
        )

        val reflow = planner.plan(
            viewport = ViewportSnapshot(
                visibleRange = 18..20,
                cause = ViewportChangeCause.LayoutReflow,
            ),
            preloadCount = 2,
            itemCount = 100,
        )
        assertEquals(listOf(17, 16), reflow.indices)
    }
}
