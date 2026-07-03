package com.shizq.bika.core.datastore.serializer

import com.shizq.bika.core.model.AutoScrollConfig
import com.shizq.bika.core.model.BookSpreadsMode
import com.shizq.bika.core.model.Channel
import com.shizq.bika.core.model.EyeCareConfig
import com.shizq.bika.core.model.FavoriteTag
import com.shizq.bika.core.model.preferences.AppPreferences
import com.shizq.bika.core.model.preferences.ContentFilterPreferences
import com.shizq.bika.core.model.preferences.DashboardPreferences
import com.shizq.bika.core.model.preferences.DnsPreferences
import com.shizq.bika.core.model.preferences.DownloadPreferences
import com.shizq.bika.core.model.preferences.NetworkPreferences
import com.shizq.bika.core.model.preferences.ReaderPreferences
import com.shizq.bika.core.model.preferences.ThemePreferences
import com.shizq.bika.core.model.preferences.UserPreferences
import com.shizq.bika.core.model.preferences.UserProfileSnapshot
import com.shizq.bika.core.model.reader.ReadingMode
import com.shizq.bika.core.model.reader.ScreenOrientation
import com.shizq.bika.core.model.reader.TapZoneLayout
import com.shizq.bika.core.model.theme.DarkThemeConfig
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * 将旧版扁平结构的 [UserPreferences] JSON 迁移为新版嵌套聚合结构。
 *
 * 背景：DataStore 的 Json 配置开启了 ignoreUnknownKeys，直接用新 serializer 解析旧数据
 * 不会报错，而是静默丢弃全部扁平字段、回落默认值，导致用户设置丢失。因此这里主动探测
 * 旧结构并逐字段搬迁。
 */
internal object UserPreferencesMigration {

    /** 旧扁平结构才会出现在顶层的标志性字段。 */
    private val LEGACY_MARKER_KEYS = setOf(
        "readingMode", "eyeCareEnabled", "cachedUserName",
        "activeDnsLine", "downloadOverWifiOnly", "channels",
    )

    /** 新嵌套结构的聚合键。 */
    private val NESTED_KEYS = setOf(
        "reader", "theme", "network", "download", "filter", "app", "dashboard", "cachedProfile",
    )

    /** 顶层是否为需要迁移的旧扁平结构。 */
    fun isLegacyFlat(root: JsonObject): Boolean {
        if (root.keys.any { it in NESTED_KEYS }) return false
        return root.keys.any { it in LEGACY_MARKER_KEYS }
    }

    fun migrate(json: Json, root: JsonObject): UserPreferences {
        return UserPreferences(
            reader = migrateReader(json, root),
            theme = ThemePreferences(
                darkThemeConfig = root.enumOrDefault(
                    "darkThemeConfig",
                    DarkThemeConfig.FOLLOW_SYSTEM
                ),
            ),
            network = NetworkPreferences(
                dns = DnsPreferences(
                    apiDnsHosts = root.stringSet("apiDns", setOf(DnsPreferences.DEFAULT_DNS_IP)),
                    imageDnsHosts = root.stringSet(
                        "imageDns",
                        setOf(DnsPreferences.DEFAULT_DNS_IP)
                    ),
                    activeLine = root.stringOrDefault(
                        "activeDnsLine",
                        DnsPreferences.DEFAULT_DNS_LINE
                    ),
                ),
                isLoggingEnabled = root.boolOrDefault("isLoggingEnabled", false),
            ),
            download = DownloadPreferences(
                overWifiOnly = root.boolOrDefault("downloadOverWifiOnly", false),
                maxConcurrentDownloads = root.intOrDefault("maxConcurrentDownloads", 3),
            ),
            filter = ContentFilterPreferences(
                globalTopicBlockEnabled = root.boolOrDefault("excludeTopicsGlobal", false),
                globalBlockedTopics = root.stringList("globalExcludedTopics"),
                favoriteTags = root.favoriteTags(json),
                blockedTags = root.stringSet("blockedTags", emptySet()),
            ),
            app = AppPreferences(
                autoCheckIn = root.boolOrDefault("autoCheckIn", true),
                secureScreenEnabled = root.boolOrDefault("secureScreenEnabled", false),
                predictiveBackEnabled = root.boolOrDefault("usePredictiveBack", false),
                fontScale = root.floatOrDefault("fontScale", 1.0f),
            ),
            dashboard = DashboardPreferences(
                channels = root.channels(json),
            ),
            profile = UserProfileSnapshot(
                name = root.stringOrDefault("cachedUserName", ""),
                avatarUrl = root.stringOrDefault("cachedUserAvatarUrl", ""),
                level = root.intOrDefault("cachedUserLevel", 0),
                exp = root.intOrDefault("cachedUserExp", 0),
                title = root.stringOrDefault("cachedUserTitle", ""),
                gender = root.stringOrDefault("cachedUserGender", ""),
                slogan = root.stringOrDefault("cachedUserSlogan", ""),
                honorBadges = root.stringList("cachedUserCharacters"),
            ),
        )
    }

    private fun migrateReader(json: Json, root: JsonObject) = ReaderPreferences(
        readingMode = root.enumOrDefault("readingMode", ReadingMode.WEBTOON),
        screenOrientation = root.enumOrDefault("screenOrientation", ScreenOrientation.Portrait),
        tapZoneLayout = root.enumOrDefault("tapZoneLayout", TapZoneLayout.Sides),
        volumeKeyNavigationEnabled = root.boolOrDefault("volumeKeyNavigation", true),
        preloadCount = root.intOrDefault("preloadCount", 2),
        eyeCare = EyeCareConfig(
            enabled = root.boolOrDefault("eyeCareEnabled", false),
            darkness = root.floatOrDefault("eyeCareDarkness", 0.3f),
        ),
        autoScroll = AutoScrollConfig(
            enabled = root.boolOrDefault("autoScrollEnabled", false),
            speed = root.intOrDefault("autoScrollSpeed", 3),
        ),
        bookSpreadsMode = root.enumOrDefault("bookSpreadsMode", BookSpreadsMode.AUTO),
        magnifierEnabled = root.boolOrDefault("magnifierEnabled", true),
        statusBarCapsuleEnabled = root.boolOrDefault("statusBarCapsuleEnabled", true),
    )

    // --- JsonObject 取值辅助 ---

    private fun JsonObject.stringOrDefault(key: String, default: String): String =
        this[key]?.jsonPrimitive?.contentOrNull ?: default

    private fun JsonObject.boolOrDefault(key: String, default: Boolean): Boolean =
        this[key]?.jsonPrimitive?.booleanOrNull ?: default

    private fun JsonObject.intOrDefault(key: String, default: Int): Int =
        this[key]?.jsonPrimitive?.intOrNull ?: default

    private fun JsonObject.floatOrDefault(key: String, default: Float): Float =
        this[key]?.jsonPrimitive?.floatOrNull ?: default

    private inline fun <reified T : Enum<T>> JsonObject.enumOrDefault(key: String, default: T): T {
        val name = this[key]?.jsonPrimitive?.contentOrNull ?: return default
        return enumValues<T>().firstOrNull { it.name == name } ?: default
    }

    private fun JsonObject.stringList(key: String): List<String> =
        (this[key] as? JsonArray)
            ?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()

    private fun JsonObject.stringSet(key: String, default: Set<String>): Set<String> =
        (this[key] as? JsonArray)
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }?.toSet() ?: default

    private fun JsonObject.favoriteTags(json: Json): List<FavoriteTag> {
        val arr = this["favoriteTags"] ?: return emptyList()
        return runCatching {
            json.decodeFromJsonElement(ListSerializer(FavoriteTag.serializer()), arr)
        }.getOrDefault(emptyList())
    }

    private fun JsonObject.channels(json: Json): List<Channel> {
        val arr = this["channels"] as? JsonArray ?: return DashboardPreferences().channels
        // 旧数据的图标字段名为 resName，新模型改为 iconKey；逐项手动解析以兼容两种字段名，
        // 避免因缺失 iconKey 而整体回落默认、丢失用户的频道顺序与启停状态。
        return runCatching {
            arr.mapNotNull { element ->
                val obj = element as? JsonObject ?: return@mapNotNull null
                val displayName = obj.stringOrDefault("displayName", "")
                val iconKey = obj["iconKey"]?.jsonPrimitive?.contentOrNull
                    ?: obj["resName"]?.jsonPrimitive?.contentOrNull
                    ?: return@mapNotNull null
                if (displayName.isBlank()) return@mapNotNull null
                Channel(
                    label = displayName,
                    iconKey = iconKey,
                    isActive = obj.boolOrDefault("isActive", true),
                )
            }.ifEmpty { DashboardPreferences().channels }
        }.getOrDefault(DashboardPreferences().channels)
    }
}
