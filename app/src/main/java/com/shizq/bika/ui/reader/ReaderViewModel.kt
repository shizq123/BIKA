package com.shizq.bika.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.shizq.bika.paging.ComicPagingSource
import com.shizq.bika.ui.reader.ReaderActivity.Companion.EXTRA_ID
import com.shizq.bika.ui.reader.ReaderActivity.Companion.EXTRA_ORDER
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

class ReaderViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val idFlow = savedStateHandle.getStateFlow(EXTRA_ID, "")
    private val orderFlow = savedStateHandle.getStateFlow(EXTRA_ORDER, 1)

    val comicPagingFlow = combine(idFlow, orderFlow, ::Pair)
        .flatMapLatest { (id, order) ->
            Pager(
                config = PagingConfig(
                    pageSize = 40,
                    prefetchDistance = 5,
                    enablePlaceholders = false,
                    initialLoadSize = 40
                ),
                pagingSourceFactory = {
                    ComicPagingSource(id, order)
                }
            )
                .flow
                .cachedIn(viewModelScope)
        }
}