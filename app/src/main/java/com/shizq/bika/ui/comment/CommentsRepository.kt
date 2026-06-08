package com.shizq.bika.ui.comment

import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.CommentsData
import com.shizq.bika.core.network.model.Type
import com.shizq.bika.core.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

import jakarta.inject.Inject

class CommentsRepository @Inject constructor(private val bikaDataSource: BikaDataSource) {

    /**
     * 获取漫画或游戏的主评论列表
     * @param comicsOrGames 类型：comics 或 games
     * @param id 漫画/游戏 ID
     * @param page 页码
     */
    fun getCommentsFlow(
        comicsOrGames: String,
        id: String,
        page: Int,
    ): Flow<Result<CommentsData>> = flow {
        emit(Result.Loading)
        val type = if (comicsOrGames == "games") Type.GAME else Type.COMIC
        val data = bikaDataSource.getComments(type, id, page)
        emit(Result.Success(data))
    }.catch {
        emit(Result.Error(it))
    }.flowOn(Dispatchers.IO)

    /**
     * 获取指定评论的子评论（回复）列表
     * @param commentId 主评论 ID
     * @param page 页码
     */
    fun getSubCommentsFlow(
        commentId: String,
        page: Int,
    ): Flow<Result<CommentsData>> = flow {
        emit(Result.Loading)
        val data = bikaDataSource.getReplyReply(commentId, page)
        emit(Result.Success(data))
    }.catch {
        emit(Result.Error(it))
    }.flowOn(Dispatchers.IO)
}