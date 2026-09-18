package com.shizq.bika.paging

import androidx.paging.PagingState

/**
 * 单向（只向后）分页的刷新锚点。
 *
 * 本项目的 PagingSource 都把 `prevKey` 固定为 null，因此原先那段
 * `prevKey?.plus(1) ?: nextKey?.minus(1)` 里的第一个分支恒不成立，
 * 实际生效的永远是 `nextKey - 1`。这里只保留真正会走到的路径。
 *
 * 注意末页（nextKey == null）时返回 null，Paging 会从第一页重新加载，
 * 这是 prevKey 缺失情况下的固有限制。
 */
fun <T : Any> PagingState<Int, T>.forwardOnlyRefreshKey(): Int? {
    val anchor = anchorPosition ?: return null
    return closestPageToPosition(anchor)?.nextKey?.minus(1)
}
