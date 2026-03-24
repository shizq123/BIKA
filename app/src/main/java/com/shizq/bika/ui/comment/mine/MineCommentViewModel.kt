package com.shizq.bika.ui.comment.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.shizq.bika.core.network.model.Comment
import com.shizq.bika.paging.MineCommentPagingSource
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

@HiltViewModel
class MineCommentViewModel @Inject constructor(
    private val mineCommentPagingSource: MineCommentPagingSource,
) : ViewModel() {
    val myCommentsFlow: Flow<PagingData<Comment>> = Pager(
        config = PagingConfig(20),
    ) {
        mineCommentPagingSource
    }.flow
        .cachedIn(viewModelScope)

}
