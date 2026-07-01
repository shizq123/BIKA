package com.shizq.bika.core.datastore

import androidx.datastore.core.DataStore
import com.shizq.bika.core.model.Channel
import com.shizq.bika.core.model.DarkThemeConfig
import com.shizq.bika.core.model.BookSpreadsMode
import com.shizq.bika.core.model.ReadingMode
import com.shizq.bika.core.model.ScreenOrientation
import com.shizq.bika.core.model.TapZoneLayout
import com.shizq.bika.core.model.UserPreferences
import com.shizq.bika.core.model.FavoriteTag
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

class UserPreferencesDataSource @Inject constructor(
    private val userPreferences: DataStore<UserPreferences>,
) {
    val userData: Flow<UserPreferences> = userPreferences.data

    suspend fun setReadingMode(mode: ReadingMode) {
        userPreferences.updateData { currentPreferences ->
            currentPreferences.copy(readingMode = mode)
        }
    }

    suspend fun setScreenOrientation(orientation: ScreenOrientation) {
        userPreferences.updateData { currentPreferences ->
            currentPreferences.copy(screenOrientation = orientation)
        }
    }

    suspend fun setTapZoneLayout(layout: TapZoneLayout) {
        userPreferences.updateData { currentPreferences ->
            currentPreferences.copy(tapZoneLayout = layout)
        }
    }

    suspend fun setIsVolumeKeyNavigation(enabled: Boolean) {
        userPreferences.updateData { currentPreferences ->
            currentPreferences.copy(volumeKeyNavigation = enabled)
        }
    }

    suspend fun updateChannels(channels: List<Channel>) {
        userPreferences.updateData { currentPreferences ->
            currentPreferences.copy(channels = channels)
        }
    }

    suspend fun setPreloadCount(count: Int) {
        userPreferences.updateData { currentPreferences ->
            currentPreferences.copy(preloadCount = count)
        }
    }

    suspend fun setDarkThemeConfig(config: DarkThemeConfig) {
        userPreferences.updateData {
            it.copy(darkThemeConfig = config)
        }
    }



    suspend fun setAutoCheckIn(enabled: Boolean) {
        userPreferences.updateData {
            it.copy(autoCheckIn = enabled)
        }
    }

    suspend fun setDns(dns: Set<String>) {
        userPreferences.updateData {
            val combined = it.dns + dns
            it.copy(
                dns = combined,
                apiDns = it.apiDns + dns,
                imageDns = it.imageDns + dns
            )
        }
    }

    suspend fun overwriteDns(dns: Set<String>) {
        userPreferences.updateData {
            it.copy(
                dns = dns,
                apiDns = dns,
                imageDns = dns
            )
        }
    }

    suspend fun updateDnsSettings(apiDns: Set<String>, imageDns: Set<String>, activeDnsLine: String) {
        userPreferences.updateData {
            it.copy(
                apiDns = apiDns,
                imageDns = imageDns,
                dns = apiDns + imageDns,
                activeDnsLine = activeDnsLine
            )
        }
    }

    suspend fun setFontScale(scale: Float) {
        userPreferences.updateData {
            it.copy(fontScale = scale)
        }
    }

    suspend fun setIsLoggingEnabled(enabled: Boolean) {
        userPreferences.updateData {
            it.copy(isLoggingEnabled = enabled)
        }
    }

    suspend fun setDownloadOverWifiOnly(enabled: Boolean) {
        userPreferences.updateData {
            it.copy(downloadOverWifiOnly = enabled)
        }
    }

    suspend fun setMaxConcurrentDownloads(count: Int) {
        userPreferences.updateData {
            it.copy(maxConcurrentDownloads = count)
        }
    }

    suspend fun setEyeCareEnabled(enabled: Boolean) {
        userPreferences.updateData {
            it.copy(eyeCareEnabled = enabled)
        }
    }

    suspend fun setEyeCareDarkness(darkness: Float) {
        userPreferences.updateData {
            it.copy(eyeCareDarkness = darkness)
        }
    }

    suspend fun setAutoScrollEnabled(enabled: Boolean) {
        userPreferences.updateData {
            it.copy(autoScrollEnabled = enabled)
        }
    }

    suspend fun setAutoScrollSpeed(speed: Int) {
        userPreferences.updateData {
            it.copy(autoScrollSpeed = speed)
        }
    }

    suspend fun setBookSpreadsMode(mode: BookSpreadsMode) {
        userPreferences.updateData {
            it.copy(bookSpreadsMode = mode)
        }
    }

    suspend fun setMagnifierEnabled(enabled: Boolean) {
        userPreferences.updateData {
            it.copy(magnifierEnabled = enabled)
        }
    }

    suspend fun setStatusBarCapsuleEnabled(enabled: Boolean) {
        userPreferences.updateData {
            it.copy(statusBarCapsuleEnabled = enabled)
        }
    }

    suspend fun setSecureScreenEnabled(enabled: Boolean) {
        userPreferences.updateData {
            it.copy(secureScreenEnabled = enabled)
        }
    }

    /** 保存用户资料到本地，供无网时回退展示 */
    suspend fun saveUserProfileCache(
        name: String,
        avatarUrl: String,
        level: Int,
        exp: Int,
        title: String,
        gender: String,
        slogan: String,
        characters: List<String>,
    ) {
        userPreferences.updateData {
            it.copy(
                cachedUserName = name,
                cachedUserAvatarUrl = avatarUrl,
                cachedUserLevel = level,
                cachedUserExp = exp,
                cachedUserTitle = title,
                cachedUserGender = gender,
                cachedUserSlogan = slogan,
                cachedUserCharacters = characters,
            )
        }
    }

    suspend fun setExcludeTopicsGlobal(enabled: Boolean) {
        userPreferences.updateData {
            it.copy(excludeTopicsGlobal = enabled)
        }
    }

    suspend fun setGlobalExcludedTopics(topics: List<String>) {
        userPreferences.updateData {
            it.copy(globalExcludedTopics = topics)
        }
    }

    suspend fun updateFavoriteTags(tags: List<FavoriteTag>) {
        userPreferences.updateData {
            it.copy(favoriteTags = tags)
        }
    }

    suspend fun addBlockedTag(tag: String) {
        userPreferences.updateData {
            it.copy(blockedTags = it.blockedTags + tag)
        }
    }

    suspend fun removeBlockedTag(tag: String) {
        userPreferences.updateData {
            it.copy(blockedTags = it.blockedTags - tag)
        }
    }
}