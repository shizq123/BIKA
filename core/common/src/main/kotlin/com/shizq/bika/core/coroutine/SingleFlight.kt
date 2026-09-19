package com.shizq.bika.core.coroutine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import java.util.concurrent.ConcurrentHashMap

/**
 * 按 key 合流并发请求（single-flight）。
 *
 * 同一个 key 上的多个并发调用只触发一次传入的 block，所有调用者共享同一个结果。
 * 请求结束即摘除登记，**不缓存结果**——下一次调用是一次全新的请求。
 * 需要"一段时间内复用结果"的场景应该用带 TTL 的缓存，而不是这个类。
 *
 * 典型用途：同一份接口响应被两个互不相关的消费者各取一半（例如评论接口同时
 * 返回列表首页与置顶评论），两边各自发请求会让同一个 GET 打两遍。
 *
 * @param scope 承载共享请求的 scope，必须比任何单个调用者都活得久。
 *   不要传调用方自己的 scope：那一方被取消会把其他调用者还在等的请求一起取消。
 */
class SingleFlight<K : Any, V>(scope: CoroutineScope) {

    /**
     * 实际承载请求的 scope，是构造参数 scope 的 SupervisorJob 子作用域。
     *
     * 不直接用传入的 scope：`async` 失败虽然把异常存进 Deferred 等人来 await，
     * 但它**同时会取消自己的父 Job**（这一点与 `launch` 没有区别，区别只在于
     * 异常是否额外报给 CoroutineExceptionHandler）。于是只要调用方传进来的
     * scope 不是 SupervisorJob，一次请求失败就会连带打掉整个 scope——
     * 对 viewModelScope 意味着这个 ViewModel 后续所有协程都不再工作。
     *
     * 在这里插一层 SupervisorJob 把失败隔断，正确性就不再依赖调用方传什么 scope。
     * 仍以传入 scope 的 Job 为父，所以它被取消时这里也会一并取消。
     */
    private val flightScope =
        CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job]))

    /**
     * ConcurrentHashMap 而非 Mutex：[ConcurrentHashMap.computeIfAbsent] 的
     * "查不到就创建"本身是原子的，正好是合流需要的语义。Mutex 方案还得处理
     * "持锁查表、锁外 await"带来的登记与清理交错，复杂度不值得。
     */
    private val inFlight = ConcurrentHashMap<K, Deferred<V>>()

    /** 当前在途请求数，仅用于测试与诊断 */
    internal val inFlightCount: Int get() = inFlight.size

    suspend fun run(key: K, block: suspend () -> V): V {
        var created: Deferred<V>? = null

        val deferred = inFlight.computeIfAbsent(key) {
            flightScope.async { block() }.also { created = it }
        }

        // 只有创建者注册清理，且用条件 remove(key, value)：请求完成与下一次登记
        // 可能交错，无条件 remove 会把后来者的在途请求从表里抹掉。
        // 取消路径同样要摘除，否则这条已死的 Deferred 会被后来者 await 到、
        // 立刻抛 CancellationException。
        //
        // 注册必须在 computeIfAbsent 返回之后：block 可能瞬间完成，而
        // mappingFunction 执行期间该桶是锁住的，在里面回头改这张表会死锁。
        created?.let { self ->
            self.invokeOnCompletion { inFlight.remove(key, self) }
        }

        return deferred.await()
    }
}
