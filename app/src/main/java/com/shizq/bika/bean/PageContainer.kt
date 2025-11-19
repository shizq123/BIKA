package com.shizq.bika.bean

data class PageContainer<T>(
    val docs: List<T> = emptyList(),
    val total: Int = 0,
    val limit: Int = 0,
    val page: Int = 0,             // 当前页码 (API分页)
    val pages: Int = 0          // 总页数 (API分页)
)