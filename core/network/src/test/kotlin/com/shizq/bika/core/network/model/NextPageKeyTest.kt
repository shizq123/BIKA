package com.shizq.bika.core.network.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 单向分页的下一页 key。
 *
 * 重点是"空页立即停"：只看 pages 的旧写法在 pages 虚高时会让 Paging
 * 对着空页一路 append，用户侧是滚到底就一直转圈。
 */
class NextPageKeyTest {

    private fun page(
        pages: Int,
        docCount: Int,
        page: Int = 1,
    ) = PageData(
        total = docCount,
        limit = 20,
        page = page,
        pages = pages,
        docs = List(docCount) { "doc$it" },
    )

    @Test
    fun `中间页返回下一页`() {
        assertEquals(2, page(pages = 5, docCount = 20).nextPageKey(requestedPage = 1))
    }

    @Test
    fun `末页返回 null`() {
        assertNull(page(pages = 5, docCount = 20).nextPageKey(requestedPage = 5))
    }

    @Test
    fun `页码超出总页数时返回 null`() {
        // 服务端对超范围请求做 clamp 时会走到这里
        assertNull(page(pages = 5, docCount = 20).nextPageKey(requestedPage = 7))
    }

    @Test
    fun `空页即使未到末页也停止`() {
        // pages 虚高：声明还有 5 页，实际第 2 页就没数据了
        assertNull(page(pages = 5, docCount = 0).nextPageKey(requestedPage = 2))
    }

    @Test
    fun `首页为空时不再请求第二页`() {
        assertNull(page(pages = 3, docCount = 0).nextPageKey(requestedPage = 1))
    }

    @Test
    fun `总页数为零时返回 null`() {
        // 空列表的正常响应：pages=0、docs 为空
        assertNull(page(pages = 0, docCount = 0).nextPageKey(requestedPage = 1))
    }

    @Test
    fun `总页数为零但有数据时仍然停止`() {
        // 字段自相矛盾，按保守处理：不追加请求，已拿到的数据照常展示
        assertNull(page(pages = 0, docCount = 20).nextPageKey(requestedPage = 1))
    }

    @Test
    fun `用请求页而非响应页推算下一页`() {
        // 响应里的 page 被服务端 clamp 回 5，若用它算下一页会永远停在 6、原地打转
        val clamped = page(pages = 5, docCount = 20, page = 5)

        assertNull(clamped.nextPageKey(requestedPage = 9))
    }

    @Test
    fun `未满一页但仍有后续页时继续`() {
        // 去重或服务端删条目会让单页短于 limit，这不等于到了末页
        assertEquals(3, page(pages = 5, docCount = 3).nextPageKey(requestedPage = 2))
    }
}
