package com.shizq.bika.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shizq.bika.core.database.BikaDatabase
import com.shizq.bika.core.database.model.DownloadErrorCode
import com.shizq.bika.core.database.model.DownloadStatus
import com.shizq.bika.core.database.model.DownloadTaskEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * [DownloadTaskDao] 的租约（`worker_token`）与调度语义回归测试。
 *
 * 这些 SQL 是下载状态机的唯一权威：认领在一条 UPDATE 里完成状态检查 + 时间检查 +
 * 并发槽位检查，所有写入靠 `worker_token` 判断所有权。一旦某条 WHERE 写漏，
 * 表现出来的是「任务卡住不动」或「已取消的任务被复活成 COMPLETED」，
 * 都是很难从日志反推的线上问题，因此在 DAO 层直接钉死。
 *
 * 测试放在 core:database 而不是 core:download，因为 [BikaDatabase] 是 internal，
 * 只有本模块的测试编译单元能看见它。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DownloadTaskDaoTest {

    private lateinit var db: BikaDatabase
    private lateinit var dao: DownloadTaskDao

    private val now = 1_000_000L

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            BikaDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.downloadTaskDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── 认领：四种结果 ──────────────────────────────────────────────────

    @Test
    fun `认领不存在的任务返回 NotFound`() = runTest {
        val outcome = dao.claimPendingTaskTransactionally("ghost", "w1", 3, now)
        assertEquals(ClaimTaskOutcome.NotFound, outcome)
    }

    @Test
    fun `认领 PENDING 任务成功并写入 token`() = runTest {
        dao.upsert(task("t1", status = DownloadStatus.PENDING, nextScheduleAt = now - 1))

        val outcome = dao.claimPendingTaskTransactionally("t1", "w1", 3, now)

        assertTrue(outcome is ClaimTaskOutcome.Claimed, "实际结果: $outcome")
        val stored = dao.getById("t1")!!
        assertEquals(DownloadStatus.DOWNLOADING, stored.status)
        assertEquals("w1", stored.workerToken)
        // 认领应清掉上一轮的错误信息，否则 UI 会一直显示旧的失败原因
        assertEquals(DownloadErrorCode.NONE, stored.errorCode)
        assertEquals("", stored.errorMessage)
    }

    @Test
    fun `非 PENDING 状态无法被认领`() = runTest {
        val notRunnable = listOf(
            DownloadStatus.PAUSED,
            DownloadStatus.COMPLETED,
            DownloadStatus.CANCELED,
            DownloadStatus.FAILED,
            DownloadStatus.WAITING_FOR_NETWORK,
            DownloadStatus.DOWNLOADING,
        )

        for ((index, status) in notRunnable.withIndex()) {
            val id = "t-$index"
            dao.upsert(task(id, status = status, nextScheduleAt = now - 1))

            assertEquals(
                ClaimTaskOutcome.NotRunnable,
                dao.claimPendingTaskTransactionally(id, "w1", 10, now),
                "status=$status 不应被认领",
            )
        }
    }

    @Test
    fun `未到调度时间的任务不可认领`() = runTest {
        dao.upsert(task("t1", status = DownloadStatus.PENDING, nextScheduleAt = now + 60_000))

        assertEquals(
            ClaimTaskOutcome.NotRunnable,
            dao.claimPendingTaskTransactionally("t1", "w1", 3, now),
        )
        assertNull(dao.getById("t1")!!.workerToken, "失败的认领不应留下 token")
    }

    @Test
    fun `并发槽位满时返回 NoSlot`() = runTest {
        // 已有 2 个在跑，上限 2
        dao.upsert(task("r1", status = DownloadStatus.DOWNLOADING, workerToken = "a"))
        dao.upsert(task("r2", status = DownloadStatus.DOWNLOADING, workerToken = "b"))
        dao.upsert(task("t1", status = DownloadStatus.PENDING, nextScheduleAt = now - 1))

        assertEquals(
            ClaimTaskOutcome.NoSlot,
            dao.claimPendingTaskTransactionally("t1", "w1", 2, now),
        )
        assertEquals(DownloadStatus.PENDING, dao.getById("t1")!!.status)
    }

    @Test
    fun `槽位刚好够时可以认领`() = runTest {
        dao.upsert(task("r1", status = DownloadStatus.DOWNLOADING, workerToken = "a"))
        dao.upsert(task("t1", status = DownloadStatus.PENDING, nextScheduleAt = now - 1))

        assertTrue(
            dao.claimPendingTaskTransactionally(
                "t1",
                "w1",
                2,
                now
            ) is ClaimTaskOutcome.Claimed
        )
    }

    @Test
    fun `并发认领同一任务只有一个成功`() = runTest {
        dao.upsert(task("t1", status = DownloadStatus.PENDING, nextScheduleAt = now - 1))

        val outcomes = coroutineScope {
            (1..8).map { i ->
                async { dao.claimPendingTaskTransactionally("t1", "w$i", 5, now) }
            }.awaitAll()
        }

        assertEquals(
            1,
            outcomes.count { it is ClaimTaskOutcome.Claimed },
            "同一任务被重复认领会导致两个 worker 同时写同一目录，实际: $outcomes",
        )
    }

    @Test
    fun `并发认领多个任务不超过并发上限`() = runTest {
        repeat(10) { i ->
            dao.upsert(task("t$i", status = DownloadStatus.PENDING, nextScheduleAt = now - 1))
        }

        val outcomes = coroutineScope {
            (0 until 10).map { i ->
                async { dao.claimPendingTaskTransactionally("t$i", "w$i", 3, now) }
            }.awaitAll()
        }

        val claimed = outcomes.count { it is ClaimTaskOutcome.Claimed }
        assertTrue(claimed <= 3, "认领了 $claimed 个，超过上限 3")
        assertEquals(claimed, dao.countTasksByStatus(DownloadStatus.DOWNLOADING))
    }

    // ── 所有权写入：token 匹配才生效 ────────────────────────────────────

    @Test
    fun `持有 token 的进度更新生效`() = runTest {
        dao.upsert(task("t1", status = DownloadStatus.DOWNLOADING, workerToken = "w1"))

        assertEquals(1, dao.updateProgressOwned("t1", "w1", 5, 10, now))

        val stored = dao.getById("t1")!!
        assertEquals(5, stored.downloadedPages)
        assertEquals(10, stored.totalPages)
        assertEquals(50, stored.progress)
    }

    @Test
    fun `token 不匹配的写入全部失败`() = runTest {
        dao.upsert(task("t1", status = DownloadStatus.DOWNLOADING, workerToken = "w1"))

        assertEquals(0, dao.updateProgressOwned("t1", "stale", 5, 10, now))
        assertEquals(0, dao.markCompletedOwned("t1", "stale", "/p", 10, now))
        assertEquals(0, dao.markFailedOwned("t1", "stale", "IO_ERROR", "x", true, now))
        assertEquals(
            0,
            dao.markWaitingForNetworkOwned("t1", "stale", "NETWORK_UNAVAILABLE", "x", now)
        )
        assertEquals(0, dao.requeueRecoverableOwned("t1", "stale", now, "IO_ERROR", "x", now))

        val stored = dao.getById("t1")!!
        assertEquals(DownloadStatus.DOWNLOADING, stored.status, "过期 worker 不应改动任务状态")
        assertEquals("w1", stored.workerToken)
    }

    @Test
    fun `token 为 null 时不能被任何 owned 写入命中`() = runTest {
        dao.upsert(task("t1", status = DownloadStatus.DOWNLOADING, workerToken = null))

        assertEquals(0, dao.updateProgressOwned("t1", "w1", 1, 10, now))
        assertEquals(0, dao.markCompletedOwned("t1", "w1", "/p", 10, now))
    }

    @Test
    fun `完成后清空 token 并写入完成时间`() = runTest {
        dao.upsert(task("t1", status = DownloadStatus.DOWNLOADING, workerToken = "w1"))

        assertEquals(1, dao.markCompletedOwned("t1", "w1", "/data/comic/1", 12, now))

        val stored = dao.getById("t1")!!
        assertEquals(DownloadStatus.COMPLETED, stored.status)
        assertNull(stored.workerToken, "完成后必须释放租约，否则该行会一直占用并发槽")
        assertEquals("/data/comic/1", stored.localPath)
        assertEquals(12, stored.totalPages)
        assertEquals(12, stored.downloadedPages)
        assertEquals(100, stored.progress)
        assertNotNull(stored.completedAt)
    }

    @Test
    fun `重新排队时清空 token 并递增重试计数`() = runTest {
        dao.upsert(
            task(
                "t1",
                status = DownloadStatus.DOWNLOADING,
                workerToken = "w1",
                retryCount = 2
            )
        )

        assertEquals(
            1,
            dao.requeueRecoverableOwned("t1", "w1", now + 30_000, "IO_ERROR", "磁盘忙", now),
        )

        val stored = dao.getById("t1")!!
        assertEquals(DownloadStatus.PENDING, stored.status)
        assertNull(stored.workerToken)
        assertEquals(3, stored.retryCount)
        assertEquals(now + 30_000, stored.nextScheduleAt)
    }

    @Test
    fun `标记失败可选择是否递增重试计数`() = runTest {
        dao.upsert(
            task(
                "a",
                status = DownloadStatus.DOWNLOADING,
                workerToken = "w1",
                retryCount = 1
            )
        )
        dao.upsert(
            task(
                "b",
                status = DownloadStatus.DOWNLOADING,
                workerToken = "w2",
                retryCount = 1
            )
        )

        dao.markFailedOwned("a", "w1", "HTTP_ERROR", "404", incrementRetryCount = false, now = now)
        dao.markFailedOwned("b", "w2", "HTTP_ERROR", "500", incrementRetryCount = true, now = now)

        assertEquals(1, dao.getById("a")!!.retryCount)
        assertEquals(2, dao.getById("b")!!.retryCount)
        assertNull(dao.getById("a")!!.workerToken)
        assertNull(dao.getById("b")!!.workerToken)
    }

    @Test
    fun `等待网络时清空 token`() = runTest {
        dao.upsert(task("t1", status = DownloadStatus.DOWNLOADING, workerToken = "w1"))

        assertEquals(
            1,
            dao.markWaitingForNetworkOwned("t1", "w1", "WIFI_REQUIRED", "仅 Wi-Fi", now),
        )

        val stored = dao.getById("t1")!!
        assertEquals(DownloadStatus.WAITING_FOR_NETWORK, stored.status)
        assertNull(stored.workerToken, "让位后必须释放槽位")
        assertEquals(DownloadErrorCode.WIFI_REQUIRED, stored.errorCode)
    }

    // ── 进度计算 ────────────────────────────────────────────────────────

    @Test
    fun `totalPages 为 0 时进度为 0 而不是除零崩溃`() = runTest {
        dao.upsert(task("t1", status = DownloadStatus.DOWNLOADING, workerToken = "w1"))

        dao.updateProgressOwned("t1", "w1", 0, 0, now)

        assertEquals(0, dao.getById("t1")!!.progress)
    }

    @Test
    fun `进度用整数除法向下取整`() = runTest {
        dao.upsert(task("t1", status = DownloadStatus.DOWNLOADING, workerToken = "w1"))

        // 1/3 = 33.3%，SQL 里是整数除法，结果应为 33
        dao.updateProgressOwned("t1", "w1", 1, 3, now)
        assertEquals(33, dao.getById("t1")!!.progress)

        dao.updateProgressOwned("t1", "w1", 3, 3, now)
        assertEquals(100, dao.getById("t1")!!.progress)
    }

    // ── 调度查询 ────────────────────────────────────────────────────────

    @Test
    fun `可调度查询只返回到期的 PENDING`() = runTest {
        dao.upsert(task("due", status = DownloadStatus.PENDING, nextScheduleAt = now - 1))
        dao.upsert(task("future", status = DownloadStatus.PENDING, nextScheduleAt = now + 1))
        dao.upsert(task("paused", status = DownloadStatus.PAUSED, nextScheduleAt = now - 1))
        dao.upsert(
            task(
                "waiting",
                status = DownloadStatus.WAITING_FOR_NETWORK,
                nextScheduleAt = now - 1
            )
        )
        dao.upsert(task("running", status = DownloadStatus.DOWNLOADING, nextScheduleAt = now - 1))

        val ids = dao.getDispatchableTasks(now = now, limit = 10).map { it.id }

        assertEquals(listOf("due"), ids)
    }

    @Test
    fun `WAITING_FOR_NETWORK 不会被自然调度`() = runTest {
        dao.upsert(task("w", status = DownloadStatus.WAITING_FOR_NETWORK, nextScheduleAt = now - 1))

        // 这条固化当前行为：等待网络的任务只能靠 requeueWaitingForNetworkTasks 拉回，
        // 没有这个调用它就永远不会被执行。
        assertTrue(dao.getDispatchableTasks(now = now, limit = 10).isEmpty())

        assertEquals(1, dao.requeueWaitingForNetworkTasks(now = now))
        assertEquals(listOf("w"), dao.getDispatchableTasks(now = now, limit = 10).map { it.id })
    }

    @Test
    fun `可调度查询按优先级降序再按创建时间升序`() = runTest {
        dao.upsert(task("low-old", priority = 0, createdAt = 100, nextScheduleAt = now - 1))
        dao.upsert(task("high", priority = 10, createdAt = 200, nextScheduleAt = now - 1))
        dao.upsert(task("low-new", priority = 0, createdAt = 300, nextScheduleAt = now - 1))

        assertEquals(
            listOf("high", "low-old", "low-new"),
            dao.getDispatchableTasks(now = now, limit = 10).map { it.id },
        )
    }

    @Test
    fun `可调度查询遵守 limit`() = runTest {
        repeat(5) { i ->
            dao.upsert(task("t$i", status = DownloadStatus.PENDING, nextScheduleAt = now - 1))
        }

        assertEquals(2, dao.getDispatchableTasks(now = now, limit = 2).size)
    }

    @Test
    fun `下次调度时间取未来最小值`() = runTest {
        dao.upsert(task("a", status = DownloadStatus.PENDING, nextScheduleAt = now + 5_000))
        dao.upsert(task("b", status = DownloadStatus.PENDING, nextScheduleAt = now + 1_000))
        dao.upsert(task("past", status = DownloadStatus.PENDING, nextScheduleAt = now - 1))

        assertEquals(now + 1_000, dao.getNextPendingScheduleAt(now = now))
    }

    @Test
    fun `没有未来任务时下次调度时间为 null`() = runTest {
        dao.upsert(task("past", status = DownloadStatus.PENDING, nextScheduleAt = now - 1))

        assertNull(dao.getNextPendingScheduleAt(now = now))
    }

    // ── 状态计数与优先级 ────────────────────────────────────────────────

    @Test
    fun `按状态计数`() = runTest {
        dao.upsert(task("a", status = DownloadStatus.DOWNLOADING))
        dao.upsert(task("b", status = DownloadStatus.DOWNLOADING))
        dao.upsert(task("c", status = DownloadStatus.PENDING))

        assertEquals(2, dao.countTasksByStatus(DownloadStatus.DOWNLOADING))
        assertEquals(1, dao.countTasksByStatus(DownloadStatus.PENDING))
        assertEquals(0, dao.countTasksByStatus(DownloadStatus.FAILED))
    }

    @Test
    fun `bringToTop 使任务优先级最高`() = runTest {
        dao.upsert(task("a", priority = 5))
        dao.upsert(task("b", priority = 9))

        dao.bringToTop("a", Instant.fromEpochMilliseconds(now))

        assertTrue(dao.getById("a")!!.priority > dao.getById("b")!!.priority)
    }

    @Test
    fun `空表时取最大优先级返回 0 而不是 null`() = runTest {
        assertEquals(0, dao.getMaxPriority())
    }

    // ── markPending 与中断恢复 ──────────────────────────────────────────

    @Test
    fun `markPending 清空 token 使任务可重新认领`() = runTest {
        dao.upsert(task("t1", status = DownloadStatus.DOWNLOADING, workerToken = "w1"))

        dao.markPending("t1", nextScheduleAt = now - 1, now = now)

        val stored = dao.getById("t1")!!
        assertEquals(DownloadStatus.PENDING, stored.status)
        assertNull(stored.workerToken, "不清 token 的话旧 worker 仍能写这一行")
        assertTrue(
            dao.claimPendingTaskTransactionally(
                "t1",
                "w2",
                3,
                now
            ) is ClaimTaskOutcome.Claimed
        )
    }

    @Test
    fun `replaceStatus 批量把中断的下载改回 PENDING`() = runTest {
        dao.upsert(task("a", status = DownloadStatus.DOWNLOADING, workerToken = "w1"))
        dao.upsert(task("b", status = DownloadStatus.DOWNLOADING, workerToken = "w2"))
        dao.upsert(task("c", status = DownloadStatus.COMPLETED))

        val changed = dao.replaceStatus(
            sourceStatus = DownloadStatus.DOWNLOADING,
            targetStatus = DownloadStatus.PENDING,
            errorCode = DownloadErrorCode.NONE,
            errorMessage = "",
            updatedAt = Instant.fromEpochMilliseconds(now),
        )

        assertEquals(2, changed)
        assertEquals(DownloadStatus.COMPLETED, dao.getById("c")!!.status)
    }

    /**
     * 记录当前实现的一个缺口：`replaceStatus` 不清 `worker_token`。
     *
     * 中断恢复后任务回到 PENDING 但仍带着上一次的 token，被重新认领时 token 会被
     * 覆盖，所以不会立刻出错；但 P0 报告里 worker 被系统停止的场景依赖这条 SQL
     * 归还槽位，届时残留 token 会让旧 worker 仍能写入这一行。
     * 修复时应把本测试改为断言 token 为 null。
     */
    @Test
    fun `已知缺口 - replaceStatus 保留了残留 token`() = runTest {
        dao.upsert(task("a", status = DownloadStatus.DOWNLOADING, workerToken = "w1"))

        dao.replaceStatus(
            sourceStatus = DownloadStatus.DOWNLOADING,
            targetStatus = DownloadStatus.PENDING,
            errorCode = DownloadErrorCode.NONE,
            errorMessage = "",
            updatedAt = Instant.fromEpochMilliseconds(now),
        )

        assertEquals("w1", dao.getById("a")!!.workerToken)
    }

    /**
     * 记录当前实现的另一个缺口：`updateStatus`（取消/暂停走这条）不清 token，
     * 且 `markCompletedOwned` 不校验状态，导致已取消的任务能被慢一步完成的
     * worker 复活成 COMPLETED。
     *
     * 修复（updateStatus 里 `worker_token = NULL`，或给 owned 写入加
     * `AND status = 'DOWNLOADING'`）之后，应把断言反转为「仍是 CANCELED」。
     */
    @Test
    fun `已知缺口 - 已取消任务会被慢一步的 worker 复活`() = runTest {
        dao.upsert(task("t1", status = DownloadStatus.DOWNLOADING, workerToken = "w1"))

        dao.updateStatus(
            taskId = "t1",
            status = DownloadStatus.CANCELED,
            errorCode = DownloadErrorCode.CANCELED,
            errorMessage = "用户取消",
            retryDelta = 0,
            completedAt = null,
            updatedAt = Instant.fromEpochMilliseconds(now),
        )
        assertEquals("w1", dao.getById("t1")!!.workerToken)

        val affected = dao.markCompletedOwned("t1", "w1", "/p", 10, now)

        assertEquals(1, affected)
        assertEquals(DownloadStatus.COMPLETED, dao.getById("t1")!!.status)
    }

    // ── 唯一约束 ────────────────────────────────────────────────────────

    @Test
    fun `同一漫画同一章节的 upsert 覆盖而非重复插入`() = runTest {
        dao.upsert(task("t1", comicId = "c1", episodeOrder = 1, priority = 1))
        dao.upsert(task("t1", comicId = "c1", episodeOrder = 1, priority = 2))

        assertEquals(2, dao.getById("t1")!!.priority)
    }

    /**
     * 记录 P0-2 的根因：`DownloadTask` 领域模型不含 `worker_token` /
     * `next_schedule_at`，`asEntity()` 只能填默认值，整行 upsert 会把运行中
     * 任务的租约与调度时间静默抹掉。
     *
     * 这里直接用 entity 复现同一后果。修复（改用只插入不覆盖的写入，或让
     * upsert 不触碰这两列）之后，应把断言改为「token 仍为 w1」。
     */
    @Test
    fun `已知缺口 - upsert 会抹掉运行中任务的租约`() = runTest {
        dao.upsert(
            task(
                "t1",
                status = DownloadStatus.DOWNLOADING,
                workerToken = "w1",
                nextScheduleAt = 999
            ),
        )

        // 模拟 saveTask(DownloadTask)：领域模型没有这两个字段，落回默认值
        dao.upsert(task("t1", status = DownloadStatus.PENDING))

        val stored = dao.getById("t1")!!
        assertNull(stored.workerToken)
        assertEquals(0L, stored.nextScheduleAt)
        // 租约被抹掉后，原 worker 的所有写入都会静默失败
        assertEquals(0, dao.updateProgressOwned("t1", "w1", 1, 10, now))
    }

    // ── 测试辅助 ────────────────────────────────────────────────────────

    private fun task(
        id: String,
        comicId: String = "comic-$id",
        episodeOrder: Int = 1,
        status: DownloadStatus = DownloadStatus.PENDING,
        workerToken: String? = null,
        nextScheduleAt: Long = 0L,
        priority: Int = 0,
        retryCount: Int = 0,
        createdAt: Long = 0L,
    ) = DownloadTaskEntity(
        id = id,
        comicId = comicId,
        comicTitle = "标题",
        coverUrl = "",
        episodeId = "ep-$id",
        episodeTitle = "章节",
        episodeOrder = episodeOrder,
        status = status,
        localPath = "",
        priority = priority,
        workerToken = workerToken,
        nextScheduleAt = nextScheduleAt,
        retryCount = retryCount,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(createdAt),
    )
}
