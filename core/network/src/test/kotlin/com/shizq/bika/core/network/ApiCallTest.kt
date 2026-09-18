package com.shizq.bika.core.network

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * [runCatchingApi] 的契约：业务异常收成 Result，取消异常必须继续向上抛。
 *
 * 为什么这条值得单测：CancellationException 是 Exception 的子类，
 * 裸写 catch (e: Exception) 会把"协程被正常取消"当成业务失败。
 * 状态机里的后果是退出页面时把 UI 覆写成 Error——用户看到"加载失败"。
 * 这个区别靠读代码很容易漏掉，而且回归后症状隐蔽。
 */
class ApiCallTest {

    @Test
    fun `成功时包装返回值`() = runTest {
        val result = runCatchingApi { 42 }

        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `可以承载可空返回值`() = runTest {
        val result = runCatchingApi<String?> { null }

        assertTrue(result.isSuccess)
        assertEquals(null, result.getOrNull())
    }

    @Test
    fun `业务异常收成失败而不抛出`() = runTest {
        val boom = IOException("网络不可达")

        val result = runCatchingApi { throw boom }

        assertTrue(result.isFailure)
        assertEquals(boom, result.exceptionOrNull())
    }

    @Test
    fun `IllegalStateException 等运行时异常同样被收敛`() = runTest {
        val result = runCatchingApi { error("解析失败") }

        assertTrue(result.isFailure)
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `CancellationException 原样抛出而不是收成失败`() = runTest {
        var caught: CancellationException? = null

        try {
            runCatchingApi { throw CancellationException("被取消") }
        } catch (e: CancellationException) {
            caught = e
        }

        // 若这里为 null，说明取消被当成业务失败吞掉了
        assertEquals("被取消", caught?.message)
    }

    @Test
    fun `协程被取消时不会走失败分支`() = runTest {
        val started = CompletableDeferred<Unit>()
        var reachedFailureBranch = false

        val job = launch {
            runCatchingApi {
                started.complete(Unit)
                delay(10_000)
                "不该到这里"
            }.onFailure {
                // 结构化并发下取消必须能穿透，这里被执行就是回归
                reachedFailureBranch = true
            }
        }

        started.await()
        job.cancel()
        job.join()

        assertFalse(reachedFailureBranch)
        assertTrue(job.isCancelled)
    }

    @Test
    fun `取消后不会继续执行 runCatchingApi 之后的代码`() = runTest {
        val started = CompletableDeferred<Unit>()
        var ranAfterCall = false

        val job = launch {
            runCatchingApi {
                started.complete(Unit)
                delay(10_000)
            }
            // 取消被吞成 Result.failure 时，协程会正常往下走到这里。
            // 正确行为是异常穿透、协程就地结束。
            ranAfterCall = true
        }

        started.await()
        job.cancel()
        job.join()

        assertFalse(ranAfterCall)
    }

    @Test
    fun `失败后不影响后续调用`() = runTest {
        runCatchingApi { throw IOException("第一次失败") }

        val second = runCatchingApi { "第二次成功" }

        assertEquals("第二次成功", second.getOrNull())
    }
}
