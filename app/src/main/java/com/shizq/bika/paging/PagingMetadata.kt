package com.shizq.bika.paging

import kotlinx.coroutines.flow.MutableStateFlow

object PagingMetadata {
    val totalElements = MutableStateFlow(0)
    val currentPage = MutableStateFlow(0)
    val totalPages = MutableStateFlow(0)
    val title = MutableStateFlow("")
}