package com.shizq.bika.core.data.repository

import com.shizq.bika.core.data.model.Comment

/**
 * 漫画评论的数据入口。
 *
 * 存在的理由只有一个：评论列表的第一页和置顶评论来自同一个接口响应
 * （`comments/` 同时返回 `comments` 与 `topComments`），但它们在 UI 上是
 * 两个独立的消费者——分页列表由 Paging 驱动，置顶区由 ViewModel 的 StateFlow
 * 驱动。两边各自发一次请求，进入评论页时同一个 GET 会打两遍。
 *
 * 这里对"同一 comicId + 同一 page 的并发请求"做合流（single-flight），
 * 让两个消费者共享一次网络往返，同时各自保留独立的错误处理。
 */
interface CommentsRepository {

    /**
     * 取某一页评论，并发调用同一页时合流为一次请求。
     *
     * 刻意不缓存结果：评论要求下拉即新，缓存会让刷新拿到旧数据。
     * 合流窗口仅覆盖"请求在途"这段时间，请求一旦结束，下次调用就是新的网络请求。
     *
     * 失败时抛出原始异常，由各调用方自行处理（分页源转 LoadResult.Error，
     * 置顶区保留上一次的值）。
     */
    suspend fun getCommentPage(comicId: String, page: Int): CommentPage
}

/**
 * 评论接口单页响应的领域模型。
 *
 * @param comments 本页的常规评论
 * @param topComments 置顶评论。只随第一页返回，其余页恒为空列表——
 *   调用方不应依赖非首页的这个字段
 * @param totalPages 服务端声明的总页数
 */
data class CommentPage(
    val comments: List<Comment>,
    val topComments: List<Comment>,
    val totalPages: Int,
    val isEmpty: Boolean,
)

/**
 * 单向分页（`prevKey` 恒为 null）的下一页 key，规则与
 * [com.shizq.bika.core.network.model.nextPageKey] 一致：
 * 除了 `page >= totalPages`，空页也必须终止——`totalPages` 来自服务端且不保证
 * 与实际数据自洽，只看它的话一旦虚高，Paging 会对着空页一路 append。
 *
 * [isEmpty][CommentPage.isEmpty] 取的是接口原始响应是否为空，而非去重后的
 * [comments][CommentPage.comments]：整页都是重复项时页本身是有数据的，
 * 用去重结果判空会提前停止、丢掉后面的评论。
 */
fun CommentPage.nextPageKey(requestedPage: Int): Int? = when {
    isEmpty -> null
    requestedPage >= totalPages -> null
    else -> requestedPage + 1
}
