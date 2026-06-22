package com.shizq.bika.ui.reader.layout

internal fun hasPages(totalPages: Int): Boolean = totalPages > 0

internal fun lastPageIndex(totalPages: Int): Int = (totalPages - 1).coerceAtLeast(0)

internal fun Int.coerceToPageIndex(totalPages: Int): Int = coerceIn(0, lastPageIndex(totalPages))
