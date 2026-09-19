package com.shizq.bika.paging

/**
 * 跨页去重：同一 id 只保留首次出现的那一条。
 *
 * 为什么需要：服务端分页会抖动（新数据插入导致同一条同时落在两页），
 * 直接交给 LazyColumn 就是 "Key was already used" 崩溃。在 PagingSource
 * 里拦掉之后，UI 侧的 key 才能回归纯 id——掺入 index 的写法会让新数据
 * 插入后所有后续 item 的身份发生变化，item 内的 remember 状态随之错位。
 *
 * 记账按页进行且可覆盖（幂等）：同一 page 重新加载时先撤销它上一次的登记，
 * 再重新登记。单向累加的写法下，一次被丢弃的 load（协程在网络返回后、
 * 结果投递前被取消）已经把 id 写进集合，重试同一 page 会整页被过滤成空，
 * 直到下次 invalidate 才恢复。
 *
 * 生命周期与 PagingSource 实例一致：invalidate 后 Paging 会新建 PagingSource，
 * 已见集合随之重置，不会跨刷新累积。
 *
 * 非线程安全，依赖 Paging 串行调用 load 的约定。
 */
internal class CrossPageDeduplicator<T>(private val idOf: (T) -> String) {

    /** page -> 该页最终保留下来的 id。按页存放才能在重载时精确撤销。 */
    private val seenByPage = mutableMapOf<Int, Set<String>>()

    /**
     * 返回 [items] 中未被 [page] 以外的页占用过的条目，保持原有顺序。
     *
     * 同一 [page] 重复调用是幂等的：本页的旧登记会被本次结果覆盖。
     */
    fun retainUnseen(page: Int, items: List<T>): List<T> {
        val claimedByOthers = HashSet<String>()
        seenByPage.forEach { (p, ids) -> if (p != page) claimedByOthers.addAll(ids) }

        val kept = ArrayList<T>(items.size)
        val keptIds = LinkedHashSet<String>(items.size)
        for (item in items) {
            val id = idOf(item)
            // id !in keptIds 顺带去掉页内重复
            if (id !in claimedByOthers && keptIds.add(id)) kept.add(item)
        }

        seenByPage[page] = keptIds
        return kept
    }
}
