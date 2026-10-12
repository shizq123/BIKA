package com.shizq.bika.domain.filter

import com.shizq.bika.core.domain.filter.ComicStatus
import com.shizq.bika.core.domain.filter.FeedCriteria
import com.shizq.bika.core.domain.filter.GlobalContentPolicy
import com.shizq.bika.core.domain.filter.IntRangeSpec
import com.shizq.bika.core.domain.filter.compileFilterPolicy
import com.shizq.bika.core.model.ComicSummary
import com.shizq.bika.core.model.RemoteImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeedFilterMatchingTest {

    @Test
    fun `空策略放行所有漫画`() {
        val policy = compileFilterPolicy(FeedCriteria(), GlobalContentPolicy())

        assertTrue(policy.isEmpty)
        assertTrue(policy.matches(comic()))
    }

    @Test
    fun `包含主题同组内取 OR`() {
        val policy = policy(
            local = FeedCriteria(includedTopics = setOf("全彩", "同人")),
        )

        assertTrue(policy.matches(comic(categories = listOf("同人"))))
        assertFalse(policy.matches(comic(categories = listOf("短篇"))))
    }

    @Test
    fun `本地与全局排除主题合并`() {
        val policy = policy(
            local = FeedCriteria(excludedTopics = setOf("NTR")),
            global = GlobalContentPolicy(
                blockedTopicsEnabled = true,
                blockedTopics = setOf("重口地帶"),
            ),
        )

        assertTrue(policy.matches(comic(categories = listOf("純愛"))))
        assertFalse(policy.matches(comic(categories = listOf("NTR"))))
        assertFalse(policy.matches(comic(categories = listOf("重口地帶"))))
    }

    @Test
    fun `关闭全局主题规则后不应用已保存主题`() {
        val policy = policy(
            global = GlobalContentPolicy(
                blockedTopicsEnabled = false,
                blockedTopics = setOf("NTR"),
            ),
        )

        assertTrue(policy.matches(comic(categories = listOf("NTR"))))
    }

    @Test
    fun `状态条件支持完结与连载`() {
        val finished = policy(
            local = FeedCriteria(statuses = setOf(ComicStatus.Finished)),
        )

        assertTrue(finished.matches(comic(finished = true)))
        assertFalse(finished.matches(comic(finished = false)))

        val all = policy(
            local = FeedCriteria(statuses = ComicStatus.entries.toSet()),
        )
        assertTrue(all.matches(comic(finished = true)))
        assertTrue(all.matches(comic(finished = false)))
    }

    @Test
    fun `章节区间是闭区间且同组内取 OR`() {
        val policy = policy(
            local = FeedCriteria(
                episodeRanges = setOf(IntRangeSpec(2, 5), IntRangeSpec(10, 20)),
            ),
        )

        assertTrue(policy.matches(comic(epsCount = 2)))
        assertTrue(policy.matches(comic(epsCount = 5)))
        assertTrue(policy.matches(comic(epsCount = 15)))
        assertFalse(policy.matches(comic(epsCount = 6)))
    }

    @Test
    fun `无上界区间正常匹配`() {
        val policy = policy(
            local = FeedCriteria(episodeRanges = setOf(IntRangeSpec(min = 101))),
        )

        assertFalse(policy.matches(comic(epsCount = 100)))
        assertTrue(policy.matches(comic(epsCount = 101)))
        assertTrue(policy.matches(comic(epsCount = 9999)))
    }

    @Test
    fun `页数未知时放行`() {
        val policy = policy(
            local = FeedCriteria(pageRanges = setOf(IntRangeSpec(min = 500))),
        )

        assertTrue(policy.matches(comic(pagesCount = 0)))
    }

    @Test
    fun `页数有值时参与筛选`() {
        val policy = policy(
            local = FeedCriteria(pageRanges = setOf(IntRangeSpec(50, 200))),
        )

        assertTrue(policy.matches(comic(pagesCount = 50)))
        assertFalse(policy.matches(comic(pagesCount = 49)))
        assertFalse(policy.matches(comic(pagesCount = 201)))
    }

    @Test
    fun `屏蔽标签由统一策略处理`() {
        val policy = policy(
            global = GlobalContentPolicy(blockedTags = setOf("屏蔽标签")),
        )

        assertFalse(policy.matches(comic(tags = listOf("屏蔽标签"))))
        assertTrue(policy.matches(comic(tags = listOf("其他标签"))))
    }

    @Test
    fun `不同条件分组之间取 AND`() {
        val policy = policy(
            local = FeedCriteria(
                includedTopics = setOf("同人"),
                statuses = setOf(ComicStatus.Finished),
                episodeRanges = setOf(IntRangeSpec(2, 5)),
            ),
        )

        assertTrue(
            policy.matches(
                comic(
                    categories = listOf("同人"),
                    finished = true,
                    epsCount = 3
                )
            )
        )
        assertFalse(
            policy.matches(
                comic(
                    categories = listOf("同人"),
                    finished = false,
                    epsCount = 3
                )
            )
        )
        assertFalse(
            policy.matches(
                comic(
                    categories = listOf("同人"),
                    finished = true,
                    epsCount = 8
                )
            )
        )
    }

    @Test
    fun `条件切换不可变且再次切换会取消`() {
        val empty = FeedCriteria()
        val selected = empty
            .toggleIncludedTopic("全彩")
            .toggleStatus(ComicStatus.Finished)
            .toggleEpisodeRange(IntRangeSpec(2, 5))

        assertTrue(empty.includedTopics.isEmpty())
        assertEquals(setOf("全彩"), selected.includedTopics)
        assertEquals(setOf(ComicStatus.Finished), selected.statuses)

        val cleared = selected
            .toggleIncludedTopic("全彩")
            .toggleStatus(ComicStatus.Finished)
            .toggleEpisodeRange(IntRangeSpec(2, 5))
        assertFalse(cleared.hasAnySelection)
    }

    @Test
    fun `clear 只清空页面条件`() {
        val criteria = FeedCriteria(
            includedTopics = setOf("全彩"),
            excludedTopics = setOf("NTR"),
            statuses = setOf(ComicStatus.Ongoing),
        )

        assertEquals(FeedCriteria(), criteria.clear())
    }

    @Test
    fun `区间两侧都为空时构造失败`() {
        assertFailsWith<IllegalArgumentException> {
            IntRangeSpec()
        }
    }

    private fun policy(
        local: FeedCriteria = FeedCriteria(),
        global: GlobalContentPolicy = GlobalContentPolicy(),
    ) = compileFilterPolicy(local, global)

    private fun comic(
        categories: List<String> = emptyList(),
        finished: Boolean = false,
        epsCount: Int = 1,
        pagesCount: Int = 0,
        tags: List<String> = emptyList(),
    ) = ComicSummary(
        id = "test-id",
        title = "测试漫画",
        author = "作者",
        image = RemoteImage(),
        categories = categories,
        finished = finished,
        epsCount = epsCount,
        pagesCount = pagesCount,
        tags = tags,
    )
}
