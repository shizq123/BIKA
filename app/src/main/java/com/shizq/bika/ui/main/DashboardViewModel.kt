package com.shizq.bika.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.shizq.bika.bean.Media
import com.shizq.bika.core.coroutine.FlowRestarter
import com.shizq.bika.core.coroutine.restartable
import com.shizq.bika.core.result.Result
import com.shizq.bika.core.result.asResult
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import com.shizq.bika.utils.SPUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody

@HiltViewModel
class DashboardViewModel @Inject constructor() : ViewModel() {
    private val dashboardRestarter = FlowRestarter()

    val dashboardUiState = flow {
        emit(Result.Loading)
        try {
            val headers = BaseHeaders("categories", "GET").getHeaderMapAndToken()
            val response = RetrofitUtil.service.categoriesGet2(headers)

            val remoteSections = response.data?.categories?.map { it ->
                DashboardEntry.Remote(
                    title = it.title,
                    imageUrl = getFullUrl(it.thumb),
                    id = it.id,
                    isWeb = it.isWeb,
                    link = it.link
                )
            } ?: emptyList()

            emit(Result.Success(dashboardEntries + remoteSections))

        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }.map { result ->
        when (result) {
            is Result.Error -> DashboardUiState.Error(result.exception.message ?: "")
            Result.Loading -> DashboardUiState.Loading
            is Result.Success -> DashboardUiState.Success(result.data)
        }
    }
        .restartable(dashboardRestarter)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DashboardUiState.Loading
        )
    val userProfileUiState = flow {
        val profileGet2 = RetrofitUtil.service.profileGet2(
            BaseHeaders("users/profile", "GET").getHeaderMapAndToken()
        )
        emit(profileGet2)
    }.asResult()
        .map { result ->
            when (result) {
                Result.Loading -> UserProfileUiState.Loading
                is Result.Error -> UserProfileUiState.Error(result.exception.message ?: "")
                is Result.Success -> {
                    val user = result.data.data!!.user
                    UserProfileUiState.Success(
                        User(
                            name = user.name,
                            avatarUrl = getFullUrl(user.avatar),
                            characters = user.characters,
                            level = user.level,
                            exp = user.exp,
                            title = user.title,
                            gender = user.gender,
                            slogan = user.slogan,
                            hasCheckedIn = user.isPunched,
                        )
                    )
                }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UserProfileUiState.Loading
        )

    fun restart() {
        dashboardRestarter.restart()
    }

    private fun getFullUrl(media: Media): String {
        val path = media.path

        return "https://s3.picacomic.com/static/$path"
    }

    fun punch_In() {
        val headers = BaseHeaders("users/punch-in", "POST").getHeaderMapAndToken()
        RetrofitUtil.service.punchInPOST(headers)
    }

    fun getSignIn() {
        val body = RequestBody.create(
            "application/json; charset=UTF-8".toMediaTypeOrNull(),
            JsonObject().apply {
                addProperty("email", SPUtil.get("username", "") as String)
                addProperty("password", SPUtil.get("password", "") as String)
            }.asJsonObject.toString()
        )
        val headers = BaseHeaders("auth/sign-in", "POST").getHeaders()
        RetrofitUtil.service.signInPost(body, headers)
    }
}