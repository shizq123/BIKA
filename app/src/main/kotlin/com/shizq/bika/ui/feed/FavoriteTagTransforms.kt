package com.shizq.bika.ui.feed

import com.shizq.bika.core.model.FavoriteTag

internal fun addFavoriteTagToList(
    tags: List<FavoriteTag>,
    tag: FavoriteTag,
): List<FavoriteTag> = if (tags.any { it.isSameTag(tag) }) tags else tags + tag

internal fun removeFavoriteTagFromList(
    tags: List<FavoriteTag>,
    tag: FavoriteTag,
): List<FavoriteTag> = tags.filterNot { it.isSameTag(tag) }

internal fun renameFavoriteTagInList(
    tags: List<FavoriteTag>,
    tag: FavoriteTag,
    newName: String,
): List<FavoriteTag> {
    val normalizedName = normalizeFavoriteTagName(newName) ?: return tags
    return tags.map { current ->
        if (current.isSameTag(tag)) current.copy(name = normalizedName) else current
    }
}

internal fun moveFavoriteTagInList(
    tags: List<FavoriteTag>,
    fromIndex: Int,
    toIndex: Int,
): List<FavoriteTag> {
    if (fromIndex !in tags.indices || toIndex !in tags.indices || fromIndex == toIndex) {
        return tags
    }
    return tags.toMutableList().apply {
        add(toIndex, removeAt(fromIndex))
    }
}
