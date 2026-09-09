package com.shizq.bika.ui.dashboard

import com.shizq.bika.core.model.FavoriteTag
import com.shizq.bika.ui.feed.isSameTag

/**
 * 收藏标签的整表变换。
 *
 * 这些函数会被交给 `DashboardRepository.updateFavoriteTags(transform)`，在 DataStore
 * 事务内执行，因此必须是纯函数：不读外部可变状态、对同一入参恒产出同一结果。
 *
 * 之所以从 StateMachine 的 `onActionEffect` 里抽出来：内联在 lambda 里的判定（去重、
 * 空名守卫、移动越界、移位语义）是这块最容易出错的部分，内联时只能靠驱动整个
 * StateMachine 加假仓储才能覆盖到。抽成顶层函数后可直接单测，行为不变。
 */

/** 已存在同名同类型标签时原样返回，避免重复收藏。 */
internal fun addFavoriteTag(tags: List<FavoriteTag>, tag: FavoriteTag): List<FavoriteTag> =
    if (tags.any { it.isSameTag(tag) }) tags else tags + tag

internal fun removeFavoriteTag(tags: List<FavoriteTag>, tag: FavoriteTag): List<FavoriteTag> =
    tags.filterNot { it.isSameTag(tag) }

/** 空白名直接放弃改名：DataStore 里留下空名标签在 UI 上是一个不可点击的空条目。 */
internal fun renameFavoriteTag(
    tags: List<FavoriteTag>,
    tag: FavoriteTag,
    newName: String,
): List<FavoriteTag> {
    if (newName.isBlank()) return tags
    return tags.map { if (it.isSameTag(tag)) it.copy(name = newName) else it }
}

/**
 * 把 [fromIndex] 处的标签移到 [toIndex]。
 *
 * 任一下标越界时原样返回：拖拽手势在列表刚变化时可能带着过期下标到达。
 * `add(toIndex, removeAt(fromIndex))` 是标准移动语义 —— 先移除会使其后元素左移一位，
 * 随后在原目标下标插入，落点仍是调用方期望的位置。
 */
internal fun moveFavoriteTag(
    tags: List<FavoriteTag>,
    fromIndex: Int,
    toIndex: Int,
): List<FavoriteTag> {
    if (fromIndex !in tags.indices || toIndex !in tags.indices) return tags
    return tags.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
}
