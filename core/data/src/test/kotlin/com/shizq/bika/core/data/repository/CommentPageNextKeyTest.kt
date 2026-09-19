package com.shizq.bika.core.data.repository

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 评论分页的下一页 key。
 *
 * 与 PageData.nextPageKey 同一套规则，重点同样是"空页立即停"：
 * 只看 totalPages 的写法在该字段虚高时会让 Paging 对着空页一路 append。
 */
class CommentPageNextKeyTest {

    private fun page(
        totalPages: Int,
        isEmpty: Boolean = false,
    ) = CommentPage(
        comments = emptyList(),
        topComments = emptyList(),
        totalPages = totalPages,
        isEmpty = isEmpty,
    )

    @Test
    fun `中间页返回下一页`() {
        assertEquals(2, page(totalPages = 5).nextPageKey(requestedPage = 1))
    }

    @Test
    fun `末页返回 null`() {
        assertNull(page(totalPages = 5).nextPageKey(requestedPage = 5))
    }

    @Test
    fun `空页即使未到末页也停止`() {
        assertNull(page(totalPages = 5, isEmpty = true).nextPageKey(requestedPage = 2))
    }

    @Test
    fun `整页都被去重时仍继续翻页`() {
        // isEmpty 取的是接口原始响应，不是去重后的 comments。
        // 这里 comments 为空但 isEmpty = false，代表"这页有数据、只是全被去重了"，
        // 若按去重结果判空会提前停止、丢掉后面的评论
        val allDeduplicated = page(totalPages = 5, isEmpty = false)

        assertEquals(3, allDeduplicated.nextPageKey(requestedPage = 2))
    }

    @Test
    fun `总页数为零时返回 null`() {
        assertNull(page(totalPages = 0, isEmpty = true).nextPageKey(requestedPage = 1))
    }
}
