package com.shizq.bika.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.shizq.bika.paging.Chapter
import com.shizq.bika.paging.ChapterListPagingSource
import com.shizq.bika.paging.ComicPagingSource
import com.shizq.bika.ui.reader.ReaderActivity.Companion.EXTRA_ID
import com.shizq.bika.ui.reader.ReaderActivity.Companion.EXTRA_ORDER
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

class ReaderViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val idFlow = savedStateHandle.getStateFlow(EXTRA_ID, "")
    val chapterIndex = savedStateHandle.getStateFlow(EXTRA_ORDER, 1)
    val chapterPagingFlow = idFlow.flatMapLatest { id ->
        Pager(config = PagingConfig(pageSize = 40)) {
            ChapterListPagingSource(id)
        }
            .flow
            .cachedIn(viewModelScope)
    }
    val currentPage = MutableStateFlow(0)
    val comicPagingFlow = combine(idFlow, chapterIndex, ::Pair)
        .flatMapLatest { (id, order) ->
            Pager(config = PagingConfig(pageSize = 40)) {
                ComicPagingSource(id, order)
            }
                .flow
                .cachedIn(viewModelScope)
        }

    fun loadChapter(chapter: Chapter) {
        savedStateHandle[EXTRA_ORDER] = chapter.order
    }
}

data class ChapterItemUiState(
    val chapter: Chapter,
    val isCurrent: Boolean = false
)