package com.shizq.bika.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.shizq.bika.paging.ChapterListPagingSource
import com.shizq.bika.paging.ComicPagingSource
import com.shizq.bika.ui.reader.ReaderActivity.Companion.EXTRA_ID
import com.shizq.bika.ui.reader.ReaderActivity.Companion.EXTRA_ORDER
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

class ReaderViewModel(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val idFlow = savedStateHandle.getStateFlow(EXTRA_ID, "")
    private val orderFlow = savedStateHandle.getStateFlow(EXTRA_ORDER, 1)
    val chapterPagingFlow = idFlow.flatMapLatest {
        Pager(config = PagingConfig(pageSize = 40)) {
            ChapterListPagingSource(it)
        }
            .flow
            .cachedIn(viewModelScope)
    }
    val currentPage = MutableStateFlow(0)
    val comicPagingFlow = combine(idFlow, orderFlow, ::Pair)
        .flatMapLatest { (id, order) ->
            Pager(config = PagingConfig(pageSize = 40)) {
                ComicPagingSource(id, order)
            }
                .flow
                .cachedIn(viewModelScope)
        }
}