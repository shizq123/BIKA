package com.shizq.bika.feature.reader.impl.layout

import com.shizq.bika.core.model.BookSpreadsMode
import com.shizq.bika.core.model.reader.ViewerType
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 跨页判定（[isWideViewport] + [resolveDoublePage]）。
 *
 * 这两个函数原先内联在 rememberReaderContext 的 remember 块里，无法单测。
 * 它们的输出决定 [PageSpreadState] 的分组方式，判定错了整章页码都会错位，
 * 属于需要钉住的业务规则。
 */
class DoublePageResolutionTest {

    // ── isWideViewport ──────────────────────────────────────────────

    @Test
    fun `横屏一律视为宽视口`() {
        assertTrue(isWideViewport(widthPx = 1080, heightPx = 1920, isLandscape = true))
    }

    @Test
    fun `尺寸未知时不算宽视口`() {
        // 首帧 containerSize 为 0×0：0f/0f = NaN，而 NaN 的任何比较都是 false。
        // 必须显式短路，否则「宽高比」这条判定路径静默失效。
        assertFalse(isWideViewport(widthPx = 0, heightPx = 0, isLandscape = false))
        assertFalse(isWideViewport(widthPx = 1920, heightPx = 0, isLandscape = false))
        assertFalse(isWideViewport(widthPx = 0, heightPx = 1080, isLandscape = false))
    }

    @Test
    fun `负数尺寸不算宽视口`() {
        assertFalse(isWideViewport(widthPx = -1, heightPx = -1, isLandscape = false))
    }

    @Test
    fun `竖屏但宽高比达标算宽视口`() {
        // 竖屏平板/折叠屏展开态：orientation 是竖屏，但足够宽放下两页。
        // 这正是旧实现（结果被 remember(configuration) 永久缓存）会漏掉的场景。
        assertTrue(isWideViewport(widthPx = 1600, heightPx = 1200, isLandscape = false))
    }

    @Test
    fun `宽高比恰好等于阈值算宽视口`() {
        // 阈值取 >=，边界值属于宽视口。
        assertTrue(isWideViewport(widthPx = 1250, heightPx = 1000, isLandscape = false))
    }

    @Test
    fun `宽高比略低于阈值不算宽视口`() {
        assertFalse(isWideViewport(widthPx = 1249, heightPx = 1000, isLandscape = false))
    }

    @Test
    fun `普通手机竖屏不算宽视口`() {
        assertFalse(isWideViewport(widthPx = 1080, heightPx = 2400, isLandscape = false))
    }

    // ── resolveDoublePage ───────────────────────────────────────────

    @Test
    fun `条漫模式永不跨页`() {
        // 连续滚动没有「一屏」的概念，即使用户显式选了 DOUBLE 也不适用。
        for (mode in BookSpreadsMode.entries) {
            assertFalse(
                resolveDoublePage(ViewerType.Scrolling, mode, isWideViewport = true),
                "Scrolling + $mode 不应跨页",
            )
        }
    }

    @Test
    fun `SINGLE 强制单页`() {
        assertFalse(
            resolveDoublePage(
                ViewerType.Pager,
                BookSpreadsMode.SINGLE,
                isWideViewport = true
            )
        )
    }

    @Test
    fun `DOUBLE 强制跨页且忽略视口`() {
        // 用户显式选择优先于视口探测，窄屏也照办。
        assertTrue(
            resolveDoublePage(
                ViewerType.Pager,
                BookSpreadsMode.DOUBLE,
                isWideViewport = false
            )
        )
    }

    @Test
    fun `AUTO 跟随视口`() {
        assertTrue(resolveDoublePage(ViewerType.Pager, BookSpreadsMode.AUTO, isWideViewport = true))
        assertFalse(
            resolveDoublePage(
                ViewerType.Pager,
                BookSpreadsMode.AUTO,
                isWideViewport = false
            )
        )
    }

    @Test
    fun `AUTO 在首帧尺寸未知时退化为单页`() {
        // 端到端串一遍：尺寸未知 → 非宽视口 → 单页。
        // 真实尺寸到达后 remember key 变化会重算，不会停在这个结果上。
        val wide = isWideViewport(widthPx = 0, heightPx = 0, isLandscape = false)
        assertFalse(resolveDoublePage(ViewerType.Pager, BookSpreadsMode.AUTO, wide))
    }
}
