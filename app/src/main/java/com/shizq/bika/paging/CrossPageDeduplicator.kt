package com.shizq.bika.paging

/**
 * 跨页去重：同一 id 只保留首次出现的那一条。
 *
 * 为什么需要：服务端分页会抖动（新数据插入导致同一条同时落在两页），
 * 直接交给 LazyColumn 就是 "Key was already used" 崩溃。在 PagingSource
 * 里拦掉之后，UI 侧的 key 才能回归纯 id——掺入 index 的写法会让新数据
 * 插入后所有后续 item 的身份发生变化，item 内的 remember 状态随之错位。
 *
 * 生命周期与 PagingSource 实例一致：invalidate 后 Paging 会新建 PagingSource，
 * 已见集合随之重置，不会跨刷新累积。
 *
 * 非线程安全，依赖 Paging 串行调用 load 的约定。
 */
internal class CrossPageDeduplicator<T>(private val idOf: (T) -> String) {

    private val seen = mutableSetOf<String>()

    /** 返回 [items] 中此前未出现过的条目，保持原有顺序。 */
    fun retainUnseen(items: List<T>): List<T> = items.filter { seen.add(idOf(it)) }
}
