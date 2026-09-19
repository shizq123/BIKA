package com.shizq.bika.paging

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

    @Test
    fun `中间页返回下一页`() {
        assertEquals(2, nextPageKey(page = 1, pages = 5, isEmptyPage = false))
    }

    @Test
    fun `末页返回 null`() {
        assertNull(nextPageKey(page = 5, pages = 5, isEmptyPage = false))
    }

    @Test
    fun `页码超出总页数时返回 null`() {
        // 服务端对超范围请求做 clamp 或章节缩水时会走到这里
        assertNull(nextPageKey(page = 7, pages = 5, isEmptyPage = false))
    }

    @Test
    fun `空页即使未到末页也停止`() {
        // pages 虚高：声明还有 5 页，实际第 2 页就没数据了
        assertNull(nextPageKey(page = 2, pages = 5, isEmptyPage = true))
    }

    @Test
    fun `首页为空时不再请求第二页`() {
        assertNull(nextPageKey(page = 1, pages = 3, isEmptyPage = true))
    }

    @Test
    fun `总页数为零时返回 null`() {
        // 空列表的正常响应：pages=0、docs 为空
        assertNull(nextPageKey(page = 1, pages = 0, isEmptyPage = true))
    }

    @Test
    fun `总页数为零但有数据时仍然停止`() {
        // 字段自相矛盾，按保守处理：不追加请求，已拿到的数据照常展示
        assertNull(nextPageKey(page = 1, pages = 0, isEmptyPage = false))
    }
}
