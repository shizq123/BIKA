package com.shizq.bika.feature.comicdetail.impl.episodes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.shizq.bika.core.data.model.Chapter
import com.shizq.bika.core.data.repository.ChapterRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow

/**
 * 目录域 ViewModel。
 *
 * 章节分页由目录域单独持有，避免详情 ViewModel 同时承担分页数据源。
 * comicId 由导航参数通过 assisted injection 提供。
 */
@HiltViewModel(assistedFactory = EpisodesViewModel.Factory::class)
class EpisodesViewModel @AssistedInject constructor(
    chapterRepository: ChapterRepository,
    @Assisted private val comicId: String,
) : ViewModel() {

    val episodesFlow: Flow<PagingData<Chapter>> = chapterRepository
        .getChapterList(comicId)
        .cachedIn(viewModelScope)

    @AssistedFactory
    interface Factory {
        fun create(comicId: String): EpisodesViewModel
    }
}
