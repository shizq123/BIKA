package com.shizq.bika.ui.comicinfo

import androidx.paging.PagingData
import com.shizq.bika.core.data.model.Comment
import com.shizq.bika.core.network.model.Episode

sealed interface UnitedDetailsAction {
    data object ToggleLike : UnitedDetailsAction
    data object ToggleFavorite : UnitedDetailsAction
    data class ToggleCommentLike(val commentId: String) : UnitedDetailsAction
    data object RecordVisit : UnitedDetailsAction

    // 副作用产生的结果
    data class DetailLoadResult(val result: Result<ComicDetail>) : UnitedDetailsAction
    data class RecommendationsLoadResult(val result: Result<List<ComicSummary>>) :
        UnitedDetailsAction

    data class PinnedCommentsUpdated(val comments: List<Comment>) : UnitedDetailsAction

    // PagingData 是流，单独处理
    data class EpisodesUpdated(val episodes: PagingData<Episode>) : UnitedDetailsAction
    data class RegularCommentsUpdated(val comments: PagingData<Comment>) : UnitedDetailsAction

    // 动作执行结果
    data class LikeRequestCompleted(val result: Boolean) : UnitedDetailsAction
    data class FavoriteRequestCompleted(val success: Boolean) : UnitedDetailsAction
}