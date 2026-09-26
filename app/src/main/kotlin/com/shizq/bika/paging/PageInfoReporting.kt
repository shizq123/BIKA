package com.shizq.bika.paging

/**
 * 能够上报分页总量的 [androidx.paging.PagingSource]。
 *
 * Paging 的 LoadResult 只携带 prevKey/nextKey，不携带「总页数」，
 * 而「跳转到指定页」这类 UI 需要总页数。此接口把这个旁路上报统一成一处契约，
 * 避免每个数据源各自声明一个同名字段、由调用方用 `when` 逐个匹配。
 *
 * 实现约定：
 * - 仅在成功取得数据后调用（即将返回 [androidx.paging.PagingSource.LoadResult.Page] 时），
 *   失败与取消不上报，以免用旧值或 0 覆盖 UI 上已有的总页数。
 * - 回调在 Paging 的加载线程上执行，实现方不得假设它在主线程。
 * - 单页数据源也应实现本接口并上报 `1`，这样调用方无需为「不上报」的情况写特例分支。
 */
interface PageInfoReporting {
    /**
     * 由持有方（通常是 ViewModel）赋值。
     *
     * 注意这是可变字段，赋值必须发生在 Pager 开始加载之前，
     * 即在 `Pager { }` 的工厂 lambda 内完成，不能在 Pager 构造之后再补。
     */
    var onPageInfoLoaded: ((totalPages: Int, totalCount: Int) -> Unit)?
}
