@file:OptIn(ExperimentalCoroutinesApi::class)

package com.shizq.bika.core.data.repository

import com.shizq.bika.core.coroutine.ApplicationScope
import com.shizq.bika.core.coroutine.FlowRestarter
import com.shizq.bika.core.coroutine.restartable
import com.shizq.bika.core.data.model.UserProfileState
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.datastore.UserProfileSnapshotDataSource
import com.shizq.bika.core.model.preferences.UserProfileSnapshot
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.UserProfile
import com.shizq.bika.core.result.Result
import com.shizq.bika.core.result.asResult
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

class UserRepositoryImpl @Inject constructor(
    private val network: BikaDataSource,
    private val userProfileSnapshotDataSource: UserProfileSnapshotDataSource,
    private val userCredentialsDataSource: UserCredentialsDataSource,
    @ApplicationScope private val scope: CoroutineScope,
) : UserRepository {

    private val restarter = FlowRestarter()

    /**
     * 算子顺序有讲究：
     * - `asResult` 在 `map` 之前：兜底逻辑要看到 Error 才能决定是否读缓存。
     * - `restartable` 在 `shareIn` 之内：反过来的话每个订阅者各自持有一条重启链，
     *   [refreshUserProfile] 只会刷新其中一条。
     * - `WhileSubscribed(5_000)`：抽屉开合、页面重建这类短暂无订阅者的间隙不重新发请求。
     */
    override val userProfile = flow { emit(fetchAndCache()) }
        .asResult()
        .map { it.orCachedFallback() }
        .restartable(restarter)
        .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    override suspend fun refreshUserProfile() {
        restarter.restart()
    }

    override suspend fun clearUserProfileSnapshot() {
        userProfileSnapshotDataSource.clear()
    }

    override suspend fun punchIn() {
        network.punchIn()
    }

    override suspend fun updateSlogan(slogan: String) {
        network.updateUserProfileSlogan(slogan)
    }

    /**
     * 修改密码，成功后同步本地已保存的密码。
     *
     * 本地密码用于登录页预填。改密后若不同步，预填的是旧密码，
     * 用户下次登录会拿一个必然失败的密码去提交，且很难意识到原因。
     *
     * 仅在本地原本存有密码时才写入：密码为 null 表示用户当初没勾"记住密码"，
     * 这里不应替他做出保存密码的决定。
     */
    override suspend fun changePassword(oldPassword: String, newPassword: String) {
        network.changePassword(oldPassword, newPassword)
        val hadStoredPassword = userCredentialsDataSource.userData.firstOrNull()?.password != null
        if (hadStoredPassword) {
            userCredentialsDataSource.setPassword(newPassword)
        }
    }

    private suspend fun fetchAndCache(): UserProfileState {
        val profile = network.fetchUserProfile().user
        val snapshot = profile.toSnapshot()
        userProfileSnapshotDataSource.store(snapshot)
        return UserProfileState(profile = snapshot, hasCheckedIn = profile.isPunched)
    }

    /**
     * 网络失败时回退到本地缓存。有缓存就当成功态发出（[UserProfileState.hasCheckedIn]
     * 为 null 标明这是缓存），没缓存才把错误透出去。
     */
    private suspend fun Result<UserProfileState>.orCachedFallback(): Result<UserProfileState> =
        if (this is Result.Error) {
            cachedProfileOrNull()?.let { Result.Success(it) } ?: this
        } else {
            this
        }

    /**
     * name 为空视为"没有可用缓存"而非"缓存了一个匿名用户"：
     * 默认快照是全空字段，直接返回会让 UI 渲染一张空白资料卡。
     */
    private suspend fun cachedProfileOrNull(): UserProfileState? =
        userProfileSnapshotDataSource.profile.first()
            .takeIf { it.name.isNotEmpty() }
            ?.let { UserProfileState(profile = it, hasCheckedIn = null) }
}

private fun UserProfile.toSnapshot() = UserProfileSnapshot(
    name = name,
    avatarUrl = imageUrl,
    level = level,
    exp = exp,
    title = title,
    gender = gender,
    slogan = slogan,
    honorBadges = characters,
)
