package com.shizq.bika.core.network

import kotlin.coroutines.cancellation.CancellationException

/**
 * 执行一次网络调用，把业务异常收成 [Result]，但让 [CancellationException] 继续向上抛。
 *
 * 为什么需要它：`CancellationException` 是 `Exception` 的子类，裸写
 * `catch (e: Exception)` 会把"协程被正常取消"（页面退出、flatMapLatest 切换）
 * 当成业务失败处理——状态机因此会在退出页面时把 UI 覆写成 Error，
 * 用户看到的是"加载失败"而不是正常返回。
 *
 * 结构化并发要求取消信号必须能传播到上层，所以这里必须 rethrow。
 */
suspend inline fun <T> runCatchingApi(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
