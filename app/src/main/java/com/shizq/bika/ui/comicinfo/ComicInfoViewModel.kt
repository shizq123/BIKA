package com.shizq.bika.ui.comicinfo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.freeletics.flowredux2.initializeWith
import com.shizq.bika.core.data.model.Comment
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.Episode
import com.shizq.bika.paging.EpisodePagingSource
import com.shizq.bika.ui.comicinfo.paging.CommentPagingSource
import com.shizq.bika.ui.comicinfo.paging.ReplyPagingSource
import com.shizq.bika.ui.comicinfo.statemachine.UnitedDetailsStateMachine
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val TAG = "ComicInfoViewModel"

@HiltViewModel(assistedFactory = ComicInfoViewModel.Factory::class)
class ComicInfoViewModel @AssistedInject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val network: BikaDataSource,
    private val commentPagingSourceFactory: CommentPagingSource.Factory,
    private val replyPagingSourceFactory: ReplyPagingSource.Factory,
    unitedDetailsStateMachine: UnitedDetailsStateMachine,
    @Assisted val id: String,
) : ViewModel() {
    init {
        unitedDetailsStateMachine.initializeWith { UnitedDetailsUiState.Initialize(id) }
    }
    private val stateMachine = unitedDetailsStateMachine.launchIn(viewModelScope)
    val state = stateMachine.state
    val episodesFlow: Flow<PagingData<Episode>> = Pager(
                    config = PagingConfig(
                        pageSize = 40,
                    ),
                    pagingSourceFactory = { EpisodePagingSource(network, id) }
                ).flow
        .cachedIn(viewModelScope)
    val pinnedComments: StateFlow<List<Comment>>
        field = MutableStateFlow(emptyList())
    val regularComments: Flow<PagingData<Comment>> =
                Pager(
                    config = PagingConfig(
                        pageSize = 40,
                    ),
                ) {
                    commentPagingSourceFactory(id) {
                        pinnedComments.value = it
                    }
                }.flow

    val replyList = state.map {
        (it as? UnitedDetailsUiState.Content)?.viewingRepliesForId
    }.filterNotNull()
        .flatMapLatest {
            Pager(config = PagingConfig(pageSize = 5)) {
                replyPagingSourceFactory(it)
            }.flow
        }.cachedIn(viewModelScope)

    fun dispatch(action: UnitedDetailsAction) {
        viewModelScope.launch {
            stateMachine.dispatch(action)
        }
    }

    //  {
    //                                                                                                      "code": 200,
    //                                                                                                      "message": "success",
    //                                                                                                      "data": {
    //                                                                                                        "comments": {
    //                                                                                                          "docs": [
    //                                                                                                            {
    //                                                                                                              "_id": "694cabdda0e0811fc217464d",
    //                                                                                                              "content": "看了一下，多出来的100页应该是特别短篇和草稿。",
    //                                                                                                              "_user": {
    //                                                                                                                "_id": "61705f0c514da53453eccff9",
    //                                                                                                                "gender": "bot",
    //                                                                                                                "name": "就回家你不会",
    //                                                                                                                "title": "萌新",
    //                                                                                                                "verified": false,
    //                                                                                                                "exp": 3680,
    //                                                                                                                "level": 6,
    //                                                                                                                "characters": [],
    //                                                                                                                "role": "member",
    //                                                                                                                "avatar": {
    //                                                                                                                  "originalName": "avatar.jpg",
    //                                                                                                                  "path": "tobs/16356393-bef5-42b6-a3e1-df7332f4ce81.jpg",
    //                                                                                                                  "fileServer": "https://storage-b.picacomic.com"
    //                                                                                                                }
    //                                                                                                              },
    //                                                                                                              "_parent": "694cab249f922521c3d2e221",
    //                                                                                                              "_comic": "6659c87480342f0cfd435837",
    //                                                                                                              "totalComments": 0,
    //                                                                                                              "isTop": false,
    //                                                                                                              "hide": false,
    //                                                                                                              "created_at": "2025-12-25T03:13:33.917Z",
    //                                                                                                              "id": "694cabdda0e0811fc217464d",
    //                                                                                                              "likesCount": 0,
    //                                                                                                              "isLiked": false
    //                                                                                                            }
    //                                                                                                          ],
    //                                                                                                          "total": 1,
    //                                                                                                          "limit": 5,
    //                                                                                                          "page": "1",
    //                                                                                                          "pages": 1
    //                                                                                                        }
    //                                                                                                      }
    //                                                                                                    }
    fun setReplyId(id: String) {
        savedStateHandle["ReplyId"] = id
    }

    fun toggleCommentLike(id: String) {
//        val currentState = comicDetailUiState.value
//        if (currentState is ComicDetailUiState.Success) {
//            viewModelScope.launch {
//                network.toggleCommentLike(id)
//            }
//        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            id: String,
        ): ComicInfoViewModel
    }
}