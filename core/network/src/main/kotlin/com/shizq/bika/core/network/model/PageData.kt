package com.shizq.bika.core.network.model

import com.shizq.bika.core.network.utils.LenientIntSerializer
import kotlinx.serialization.Serializable

@Serializable
data class PageData<T>(
    @Serializable(with = LenientIntSerializer::class) val total: Int,
    @Serializable(with = LenientIntSerializer::class) val limit: Int,
    @Serializable(with = LenientIntSerializer::class) val page: Int,
    @Serializable(with = LenientIntSerializer::class) val pages: Int,
    val docs: List<T>,
)

/**
 * 单向分页（`prevKey` 恒为 null）的下一页 key。
 *
 * 两个终止条件都必需：
 * - `requestedPage >= pages`：正常的末页判断。
 * - [docs] 为空：本类所有 Int 字段都套了 [LenientIntSerializer]（服务端偶发
 *   返回非整数），不保证 `pages` 与 `docs` 自洽。只看 `pages` 时，一旦它虚高，
 *   Paging 会对着空页一路 append 到 `pages`，用户侧表现为"滚到底一直转圈
 *   但没有新内容"。
 *
 * 取 [requestedPage] 而不是响应里的 [page]：服务端对超范围请求会做 clamp，
 * 用被 clamp 回来的值算下一页会原地打转。
 *
 * 这条规则原先在 10 个 PagingSource 里各写一遍，且已经出现 `page < pages` 与
 * `page >= pages` 两种写法；收到这里是为了只有一处定义、也才有可测性。
 */
fun PageData<*>.nextPageKey(requestedPage: Int): Int? = when {
    docs.isEmpty() -> null
    requestedPage >= pages -> null
    else -> requestedPage + 1
}
