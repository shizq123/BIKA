package com.shizq.bika.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.shizq.bika.bean.Media
import com.shizq.bika.core.coroutine.FlowRestarter
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.model.Channel
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.result.Result
import com.shizq.bika.core.result.asResult
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import com.shizq.bika.utils.SPUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val userCredentialsDataSource: UserCredentialsDataSource,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val network: BikaDataSource,
) : ViewModel() {
    private val dashboardRestarter = FlowRestarter()
    val userChannelPreferences = userPreferencesDataSource.userData
        .map { it.channels }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList(),
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

    fun onChannelToggled(targetChannel: Channel, enable: Boolean) {
        viewModelScope.launch {
            val userPreferences = userPreferencesDataSource.userData.first()
            val currentList = userPreferences.channels

            val newList = currentList.map { item ->
                if (item.displayName == targetChannel.displayName) {
                    item.copy(isActive = enable)
                } else {
                    item
                }
            }

            userPreferencesDataSource.updateChannels(newList)
        }
    }
    fun onChannelsReordered(newOrderedList: List<Channel>) {
        viewModelScope.launch {
            userPreferencesDataSource.updateChannels(newOrderedList)
        }
    }
    // performAutoCheckIn
    // performInitialLogin
    fun onCheckIn() {
        viewModelScope.launch {
            val headers = BaseHeaders("users/punch-in", "POST").getHeaderMapAndToken()
            RetrofitUtil.service.punchInPOST(headers)
        }
    }

    fun onLogin() {
        viewModelScope.launch {
            val credentials = userCredentialsDataSource.userData.first()
            if (credentials.token == null) {
                userCredentialsDataSource.setToken(SPUtil.get("token", "") as String)
            }

            userCredentialsDataSource.setUsername(SPUtil.get("username", "") as String)
            userCredentialsDataSource.setPassword(SPUtil.get("password", "") as String)
        }
        viewModelScope.launch {
            val body = JsonObject().apply {
                addProperty("email", SPUtil.get("username", "") as String)
                addProperty("password", SPUtil.get("password", "") as String)
            }.asJsonObject.toString()
                .toRequestBody("application/json; charset=UTF-8".toMediaTypeOrNull())
            val headers = BaseHeaders("auth/sign-in", "POST").getHeaders()
            val signInPost2 = RetrofitUtil.service.signInPost2(body, headers)
            signInPost2.data?.let {
                userCredentialsDataSource.setToken(it.token)
                SPUtil.put("token", it.token)
            }
        }
    }
}