package com.shizq.bika.ui.comment

import com.shizq.bika.bean.CommentsBean
import com.shizq.bika.network.Result
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class CommentsRepository {
    suspend fun getCommentsFlow(comicsOrGames: String,id: String,page:Int): Flow<Result<CommentsBean>> = flow {
        emit(Result.Loading)
        val response = RetrofitUtil.service.comicsCommentsGet(
            comicsOrGames,
            id,
            page.toString(),
            BaseHeaders("$comicsOrGames/$id/comments?page=$page", "GET").getHeaderMapAndToken()
        )
        if (response.code == 200) {
            val data = response.data
            if (data != null) {
                if (data.comments.page == 1 &&data.topComments!=null) {
                        val listDocs=ArrayList<CommentsBean.Comments.Doc>()
                        for (index in data.topComments.size - 1 downTo  0) {
                            //倒序遍历
                            val commentsBean = CommentsBean.Comments.Doc(
                                data.topComments[index]._id,
                                data.topComments[index]._user,
                                data.topComments[index].commentsCount,
                                data.topComments[index].content,
                                data.topComments[index].created_at,
                                false,
                                data.topComments[index]._id,
                                data.topComments[index].isLiked,
                                true,
                                data.topComments[index].likesCount,
                                data.topComments[index].totalComments,
                                false
                            )
                            listDocs.add(commentsBean)
                        }
                        listDocs.addAll(data.comments.docs)
                        data.comments.docs = listDocs
                    }
                emit(Result.Success(data))
            } else {
                emit(Result.Error(response.code, "", "请求结果为空"))
            }
        } else {
            emit(Result.Error(response.code, response.error, response.message))
        }
    }.catch {
        //需要异常信息处理 （例如code=400 error=1014 审核中）
        emit(Result.Error(-1, "", "请求结果异常"))
    }.flowOn(Dispatchers.IO)

    suspend fun getSubCommentsFlow(commentId: String,subCommentsPage:Int): Flow<Result<CommentsBean>> = flow {
        emit(Result.Loading)
        val response = RetrofitUtil.service.commentsChildrensGet(
            commentId,
            subCommentsPage.toString(),
            BaseHeaders("comments/$commentId/childrens?page=$subCommentsPage", "GET").getHeaderMapAndToken()
        )
        if (response.code == 200) {
            val data = response.data
            if (data != null) {
                if (subCommentsPage == 1 && data.comments.pages!=1) {
                    //每次只加载5条数据，所以第一页多请求一遍 。还有只有一夜数据不重复加载
                    getSubCommentsFlow(commentId,subCommentsPage)
                }
                emit(Result.Success(data))
            } else {
                emit(Result.Error(response.code, "", "请求结果为空"))
            }
        } else {
            emit(Result.Error(response.code, response.error, response.message))
        }
    }.catch {
        //需要异常信息处理 （例如code=400 error=1014 审核中）
        emit(Result.Error(-1, "", "请求结果异常"))
    }.flowOn(Dispatchers.IO)

}