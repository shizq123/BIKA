package com.shizq.bika.paging

/**
 * 跨页去重实现已移至 `core:data`，供该模块内的分页源（如 ChapterListPagingSource）复用。
 *
 * 这里保留别名而非再写一份：app 模块有六七个分页源引用旧路径，
 * 同一套去重逻辑出现两份实现迟早会改漏一边。
 */
internal typealias CrossPageDeduplicator<T> = com.shizq.bika.core.data.paging.CrossPageDeduplicator<T>
