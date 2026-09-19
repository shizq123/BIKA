package com.shizq.bika.paging

/**
 * 单向分页的下一页 key。
 *
 * 两个终止条件都必需：
 * - `page >= pages`：正常的末页判断。
 * - `docs` 为空：`pages` 来自服务端且套了 LenientIntSerializer，不保证与 `docs`
 *   自洽。只看 `pages` 时，一旦它虚高，Paging 会对着空页一路 append 到 `pages`，
 *   每次滚到底都重新发一轮请求，用户侧表现为"一直在转圈但没有新内容"。
 *
 * 这条规则原先在 10 个 PagingSource 里各写一遍，抽出来是为了让它只有一处定义、
 * 也才有可测性——分散的版本里已经出现过 `page < pages` 与 `page >= pages` 两种写法。
 *
 * @param page 本次请求的页码，从 1 开始
 * @param pages 服务端声明的总页数
 * @param isEmptyPage 本页 `docs` 是否为空
 */
internal fun nextPageKey(page: Int, pages: Int, isEmptyPage: Boolean): Int? = when {
    isEmptyPage -> null
    page >= pages -> null
    else -> page + 1
}
