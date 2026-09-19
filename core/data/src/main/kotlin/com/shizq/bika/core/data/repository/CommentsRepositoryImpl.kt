package com.shizq.bika.core.data.repository

import com.shizq.bika.core.coroutine.ApplicationScope
import com.shizq.bika.core.data.model.asExternalModel
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.Type
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import java.util.concurrent.ConcurrentHashMap

class CommentsRepositoryImpl @Inject constructor(
    private val network: BikaDataSource,
    @ApplicationScope private val scope: CoroutineScope,
) : CommentsRepository {

    private data class PageKey(val comicId: String, val page: Int)

    /**
     * 在途请求表。
     *
     * 用 ConcurrentHashMap 而非 Mutex：[ConcurrentHashMap.computeIfAbsent] 的
     * "查不到就创建"本身是原子的，正好是合流需要的语义；而 Mutex 方案里
     * "持锁查表、锁外 await"还要额外处理登记与清理的交错，复杂度不值得。
     */
    private val inFlight = ConcurrentHashMap<PageKey, Deferred<CommentPage>>()

    override suspend fun getCommentPage(comicId: String, page: Int): CommentPage {
        val key = PageKey(comicId, page)
        var created: Deferred<CommentPage>? = null

        val deferred = inFlight.computeIfAbsent(key) {
            // 挂在 ApplicationScope 而不是调用方的 scope：两个消费者共享同一个
            // Deferred，若挂在其中一方身上，那一方被取消（例如 Paging 重建分页源）
            // 会把另一方还在等的请求一起取消。
            // 该 scope 是 SupervisorJob，这里的失败不会波及同级任务。
            scope.async { fetchPage(comicId, page) }.also { created = it }
        }

        // 只有创建者负责摘除登记，且用条件 remove：请求完成与下一次登记可能交错，
        // 无条件 remove 会把后来者的在途请求从表里抹掉。
        // 取消路径也走这里——已取消的 Deferred 留在表里会被后来者 await 到、
        // 立刻抛 CancellationException。
        //
        // 注册放在 computeIfAbsent 之后：请求可能瞬间完成，而 mappingFunction
        // 执行期间 ConcurrentHashMap 的桶是锁住的，在里面回头改这张表会死锁。
        val self = created
        self?.invokeOnCompletion { inFlight.remove(key, self) }

        return deferred.await()
    }

    private suspend fun fetchPage(comicId: String, page: Int): CommentPage {
        val response = network.getComments(Type.COMIC, comicId, page)
        val commentsPage = response.comments

        return CommentPage(
            comments = commentsPage.docs.map { it.asExternalModel() },
            // topComments 只随第一页返回；其余页给空列表，避免调用方误用
            topComments = if (page == 1) {
                response.topComments.map { it.asExternalModel() }
            } else {
                emptyList()
            },
            totalPages = commentsPage.pages,
            isEmpty = commentsPage.docs.isEmpty(),
        )
    }
}
