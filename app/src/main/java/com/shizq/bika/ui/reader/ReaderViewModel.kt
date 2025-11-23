package com.shizq.bika.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.shizq.bika.core.datastore.di.com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.model.ScreenOrientation
import com.shizq.bika.paging.Chapter
import com.shizq.bika.paging.ChapterListPagingSource
import com.shizq.bika.paging.ComicPagingSource
import com.shizq.bika.ui.reader.ReaderActivity.Companion.EXTRA_ID
import com.shizq.bika.ui.reader.ReaderActivity.Companion.EXTRA_ORDER
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val userPreferencesDataSource: UserPreferencesDataSource,
) : ViewModel() {
    val screenOrientationFlow = userPreferencesDataSource.userData
        .map { it.screenOrientation }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            ScreenOrientation.System
        )
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

    fun updateChapterOrder(chapter: Chapter) {
        savedStateHandle[EXTRA_ORDER] = chapter.order
    }
}

data class ChapterItemUiState(
    val chapter: Chapter,
    val isCurrent: Boolean = false
)