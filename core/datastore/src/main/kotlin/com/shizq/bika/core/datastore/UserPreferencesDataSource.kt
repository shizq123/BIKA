package com.shizq.bika.core.datastore

import androidx.datastore.core.DataStore
import com.shizq.bika.core.model.BookSpreadsMode
import com.shizq.bika.core.model.Channel
import com.shizq.bika.core.model.FavoriteTag
import com.shizq.bika.core.model.preferences.UserPreferences
import com.shizq.bika.core.model.preferences.UserProfileSnapshot
import com.shizq.bika.core.model.reader.ReadingMode
import com.shizq.bika.core.model.reader.ScreenOrientation
import com.shizq.bika.core.model.reader.TapZoneLayout
import com.shizq.bika.core.model.theme.DarkThemeConfig
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

class UserPreferencesDataSource @Inject constructor(
    private val userPreferences: DataStore<UserPreferences>,
) {
    val userData: Flow<UserPreferences> = userPreferences.data

    private suspend inline fun edit(crossinline block: (UserPreferences) -> UserPreferences) {
        userPreferences.updateData { block(it) }
    }

    suspend fun setReadingMode(mode: ReadingMode) = edit {
        it.copy(reader = it.reader.copy(readingMode = mode))
    }

    suspend fun setScreenOrientation(orientation: ScreenOrientation) = edit {
        it.copy(reader = it.reader.copy(screenOrientation = orientation))
    }

    suspend fun setTapZoneLayout(layout: TapZoneLayout) = edit {
        it.copy(reader = it.reader.copy(tapZoneLayout = layout))
    }

    suspend fun setIsVolumeKeyNavigation(enabled: Boolean) = edit {
        it.copy(reader = it.reader.copy(volumeKeyNavigationEnabled = enabled))
    }

    suspend fun updateChannels(channels: List<Channel>) = edit {
        it.copy(dashboard = it.dashboard.copy(channels = channels))
    }

    suspend fun setPreloadCount(count: Int) = edit {
        it.copy(reader = it.reader.copy(preloadCount = count))
    }

    suspend fun setDarkThemeConfig(config: DarkThemeConfig) = edit {
        it.copy(theme = it.theme.copy(darkThemeConfig = config))
    }

    suspend fun setAutoCheckIn(enabled: Boolean) = edit {
        it.copy(app = it.app.copy(autoCheckIn = enabled))
    }

    suspend fun setDns(dns: Set<String>) = edit {
        it.copy(
            network = it.network.copy(
                dns = it.network.dns.copy(
                    apiDnsHosts = it.network.dns.apiDnsHosts + dns,
                    imageDnsHosts = it.network.dns.imageDnsHosts + dns,
                ),
            ),
        )
    }

    suspend fun overwriteDns(dns: Set<String>) = edit {
        it.copy(
            network = it.network.copy(
                dns = it.network.dns.copy(apiDnsHosts = dns, imageDnsHosts = dns),
            ),
        )
    }

    suspend fun updateDnsSettings(
        apiDns: Set<String>,
        imageDns: Set<String>,
        activeDnsLine: String
    ) = edit {
        it.copy(
            network = it.network.copy(
                dns = it.network.dns.copy(
                    apiDnsHosts = apiDns,
                    imageDnsHosts = imageDns,
                    activeLine = activeDnsLine,
                ),
            ),
        )
    }

    suspend fun setFontScale(scale: Float) = edit {
        it.copy(app = it.app.copy(fontScale = scale))
    }

    suspend fun setIsLoggingEnabled(enabled: Boolean) = edit {
        it.copy(network = it.network.copy(isLoggingEnabled = enabled))
    }

    suspend fun setDownloadOverWifiOnly(enabled: Boolean) = edit {
        it.copy(download = it.download.copy(overWifiOnly = enabled))
    }

    suspend fun setMaxConcurrentDownloads(count: Int) = edit {
        it.copy(download = it.download.copy(maxConcurrentDownloads = count))
    }

    suspend fun setEyeCareEnabled(enabled: Boolean) = edit {
        it.copy(reader = it.reader.copy(eyeCare = it.reader.eyeCare.copy(enabled = enabled)))
    }

    suspend fun setEyeCareDarkness(darkness: Float) = edit {
        it.copy(reader = it.reader.copy(eyeCare = it.reader.eyeCare.copy(darkness = darkness)))
    }

    suspend fun setAutoScrollEnabled(enabled: Boolean) = edit {
        it.copy(reader = it.reader.copy(autoScroll = it.reader.autoScroll.copy(enabled = enabled)))
    }

    suspend fun setAutoScrollSpeed(speed: Int) = edit {
        it.copy(reader = it.reader.copy(autoScroll = it.reader.autoScroll.copy(speed = speed)))
    }

    suspend fun setBookSpreadsMode(mode: BookSpreadsMode) = edit {
        it.copy(reader = it.reader.copy(bookSpreadsMode = mode))
    }

    suspend fun setMagnifierEnabled(enabled: Boolean) = edit {
        it.copy(reader = it.reader.copy(magnifierEnabled = enabled))
    }

    suspend fun setStatusBarCapsuleEnabled(enabled: Boolean) = edit {
        it.copy(reader = it.reader.copy(statusBarCapsuleEnabled = enabled))
    }

    suspend fun setSecureScreenEnabled(enabled: Boolean) = edit {
        it.copy(app = it.app.copy(secureScreenEnabled = enabled))
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
        honorBadges: List<String>,
    ) = edit {
        it.copy(
            profile = UserProfileSnapshot(
                name = name,
                avatarUrl = avatarUrl,
                level = level,
                exp = exp,
                title = title,
                gender = gender,
                slogan = slogan,
                honorBadges = honorBadges,
            ),
        )
    }

    suspend fun setExcludeTopicsGlobal(enabled: Boolean) = edit {
        it.copy(filter = it.filter.copy(globalTopicBlockEnabled = enabled))
    }

    suspend fun setGlobalExcludedTopics(topics: List<String>) = edit {
        it.copy(filter = it.filter.copy(globalBlockedTopics = topics))
    }

    suspend fun updateFavoriteTags(tags: List<FavoriteTag>) = edit {
        it.copy(filter = it.filter.copy(favoriteTags = tags))
    }

    suspend fun setUsePredictiveBack(enabled: Boolean) = edit {
        it.copy(app = it.app.copy(predictiveBackEnabled = enabled))
    }

    suspend fun addBlockedTag(tag: String) = edit {
        it.copy(filter = it.filter.copy(blockedTags = it.filter.blockedTags + tag))
    }

    suspend fun removeBlockedTag(tag: String) = edit {
        it.copy(filter = it.filter.copy(blockedTags = it.filter.blockedTags - tag))
    }
}
