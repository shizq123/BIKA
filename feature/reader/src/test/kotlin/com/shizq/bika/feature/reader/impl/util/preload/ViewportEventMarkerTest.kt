package com.shizq.bika.feature.reader.impl.util.preload

import org.junit.Test
import kotlin.test.assertEquals

class ViewportEventMarkerTest {
    @Test
    fun `explicit generation is shared by the next viewport snapshot`() {
        val marker = ViewportEventMarker()
        marker.mark(
            cause = ViewportChangeCause.LayoutReflow,
            generation = 7L,
        )

        val event = marker.snapshot(
            visibleRange = 10..11,
            direction = ScrollDirection.Forward,
            defaultCause = ViewportChangeCause.UserScroll,
        )

        assertEquals(ViewportChangeCause.LayoutReflow, event.cause)
        assertEquals(7L, event.generation)
        assertEquals(10..11, event.visibleRange)
    }

    @Test
    fun `generation never moves backward when an older layout reports late`() {
        val marker = ViewportEventMarker()
        marker.mark(ViewportChangeCause.LayoutReflow, generation = 8L)
        assertEquals(8L, marker.snapshot(null, null, ViewportChangeCause.Unknown).generation)

        marker.mark(ViewportChangeCause.LayoutReflow, generation = 3L)
        assertEquals(8L, marker.snapshot(null, null, ViewportChangeCause.Unknown).generation)
    }

    @Test
    fun `marker is consumed after one snapshot`() {
        val marker = ViewportEventMarker()
        marker.mark(ViewportChangeCause.ProgrammaticJump)

        val first = marker.snapshot(0..1, null, ViewportChangeCause.UserScroll)
        val second = marker.snapshot(1..2, ScrollDirection.Forward, ViewportChangeCause.UserScroll)

        assertEquals(ViewportChangeCause.ProgrammaticJump, first.cause)
        assertEquals(ViewportChangeCause.UserScroll, second.cause)
    }
}
