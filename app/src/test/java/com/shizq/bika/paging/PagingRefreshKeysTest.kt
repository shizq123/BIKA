package com.shizq.bika.paging

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 单向分页的刷新锚点。
 *
 * 这个 helper 是从三个 PagingSource 里抽出来的：它们的 prevKey 都硬编码 null，
 * 所以原表达式 `prevKey?.plus(1) ?: nextKey?.minus(1)` 的第一个分支恒不成立。
 * 这些用例锁的就是"只走 nextKey - 1 这一条路"的行为。
 */
class PagingRefreshKeysTest {

    private fun state(
        anchorPosition: Int?,
        pages: List<PagingSource.LoadResult.Page<Int, String>>,
    ) = PagingState(
        pages = pages,
        anchorPosition = anchorPosition,
        config = PagingConfig(pageSize = 2),
        leadingPlaceholderCount = 0,
    )

    private fun page(
        data: List<String>,
        prevKey: Int? = null,
        nextKey: Int?,
    ) = PagingSource.LoadResult.Page(
        data = data,
        prevKey = prevKey,
        nextKey = nextKey,
    )

    @Test
    fun `没有锚点时返回 null`() {
        val result = state(
            anchorPosition = null,
            pages = listOf(page(listOf("a", "b"), nextKey = 2)),
        ).forwardOnlyRefreshKey()

        // 用户还没滚动过，没有可锚定的位置
        assertNull(result)
    }

    @Test
    fun `页列表为空时返回 null`() {
        val result = state(anchorPosition = 0, pages = emptyList())
            .forwardOnlyRefreshKey()

        assertNull(result)
    }

    @Test
    fun `锚点落在第一页时返回该页页码`() {
        // 第一页的 nextKey 是 2，减一得到 1，即刷新后重新加载第一页
        val result = state(
            anchorPosition = 0,
            pages = listOf(page(listOf("a", "b"), nextKey = 2)),
        ).forwardOnlyRefreshKey()

        assertEquals(1, result)
    }

    @Test
    fun `锚点落在中间页时返回该页页码`() {
        val result = state(
            anchorPosition = 2,
            pages = listOf(
                page(listOf("a", "b"), nextKey = 2),
                page(listOf("c", "d"), nextKey = 3),
            ),
        ).forwardOnlyRefreshKey()

        // 锚点在第二页，其 nextKey 为 3，减一得 2
        assertEquals(2, result)
    }

    @Test
    fun `末页的 nextKey 为 null 时返回 null`() {
        val result = state(
            anchorPosition = 1,
            pages = listOf(page(listOf("a", "b"), nextKey = null)),
        ).forwardOnlyRefreshKey()

        // 已知的固有限制：滚到末页后刷新会回到第一页。
        // prevKey 恒为 null 导致无从推算当前页码，不是本次重构引入的。
        assertNull(result)
    }

    @Test
    fun `prevKey 有值也不改变结果`() {
        // 锁死"第一个分支是死代码"这个前提：即便某天有人填了 prevKey，
        // 这个 helper 的语义也只由 nextKey 决定。若要支持双向分页，
        // 这条用例会失败，提示需要改 helper 而不是悄悄改变行为。
        val result = state(
            anchorPosition = 2,
            pages = listOf(
                page(listOf("c", "d"), prevKey = 1, nextKey = 3),
            ),
        ).forwardOnlyRefreshKey()

        assertEquals(2, result)
    }
}
