package com.shizq.bika.ui.feed

import com.shizq.bika.core.model.FavoriteTag
import com.shizq.bika.navigation.DiscoveryAction

/**
 * 可被收藏为标签的入口类型。
 *
 * [storageValue] 是写进 DataStore 的值，必须与历史版本写入的字符串保持一致，改动会导致
 * 用户已有收藏无法识别。
 *
 * 之所以不把 [FavoriteTag.actionType] 直接声明成枚举类型：kotlinx.serialization 反序列化
 * 未知枚举值会抛 SerializationException，而 UserPreferencesSerializer 未处理该异常，
 * 结果是一条脏数据让整份用户偏好读取失败。保持存储层为 String、在此处显式解析，
 * 未知值退化为 null，影响范围限于单个标签。
 */
enum class FeedActionType(val storageValue: String) {
    Channel("Channel"),
    Knight("Knight"),
    AdvancedSearch("AdvancedSearch");

    companion object {
        /** 未知值返回 null，由调用方决定如何呈现，不做静默兜底。 */
        fun fromStorageValue(value: String): FeedActionType? =
            entries.firstOrNull { it.storageValue == value }
    }
}

/** 收藏标签的身份判定：name + actionType 构成业务主键。 */
fun FavoriteTag.isSameTag(other: FavoriteTag): Boolean =
    name == other.name && actionType == other.actionType

/**
 * 可收藏的入口转为标签。返回 null 表示该入口不支持收藏（如「我的收藏」「随机本子」这类
 * 无参数的固定入口，收藏它们没有意义）。
 */
fun DiscoveryAction.toFavoriteTag(): FavoriteTag? = when (this) {
    is DiscoveryAction.Channel -> FavoriteTag(
        name = name,
        actionType = FeedActionType.Channel.storageValue
    )

    is DiscoveryAction.Knight -> FavoriteTag(
        name = name,
        actionType = FeedActionType.Knight.storageValue,
        actionId = id
    )

    is DiscoveryAction.AdvancedSearch -> FavoriteTag(
        name = name,
        actionType = FeedActionType.AdvancedSearch.storageValue
    )

    DiscoveryAction.ToCollections,
    DiscoveryAction.ToRecent,
    DiscoveryAction.ToRandom,
    DiscoveryAction.ToFavourite -> null
}

/**
 * 标签还原为入口。返回 null 表示 [FavoriteTag.actionType] 无法识别。
 *
 * 不提供兜底值：原先的 `else -> AdvancedSearch(name)` 会把任何无法识别的 actionType
 * 当成关键词搜索，用户点击后被静默带到一个无关页面，且看不出哪里出了问题。
 */
fun FavoriteTag.toAction(): DiscoveryAction? =
    when (FeedActionType.fromStorageValue(actionType)) {
        FeedActionType.Channel -> DiscoveryAction.Channel(name)

        // actionId 为空的骑士标签无法还原：查询必须带 id，空 id 会退化成一次无意义的请求。
        // 早期版本在 actionId 字段存在前写入的标签会命中这里。
        FeedActionType.Knight ->
            if (actionId.isBlank()) null else DiscoveryAction.Knight(name, actionId)

        FeedActionType.AdvancedSearch -> DiscoveryAction.AdvancedSearch(name)
        null -> null
    }
