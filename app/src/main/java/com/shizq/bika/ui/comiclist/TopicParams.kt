package com.shizq.bika.ui.comiclist

import com.shizq.bika.core.network.model.Sort

/**
 * 封装漫画搜索的所有参数
 * @param topic 通用搜索关键词 (原 topic)
 * @param tag 标签
 * @param authorName 作者名
 * @param knight 上传者/骑士
 * @param translationTeam 汉化组
 * @param sort 排序方式
 */
data class ComicSearchParams(
    val topic: String? = null,
    val tag: String? = null,
    val authorName: String? = null,
    val knight: String? = null,
    val translationTeam: String? = null,
    val sort: Sort,
)

sealed class TopicType(val key: String) {
    object Categories : TopicType("categories")
    object Latest : TopicType("latest")
    object Tags : TopicType("tags")
    object Author : TopicType("author")
    object Knight : TopicType("knight")
    object Translate : TopicType("translate")

    companion object {
        fun fromKey(key: String?): TopicType? {
            return when (key) {
                Categories.key -> Categories
                Latest.key -> Latest
                Tags.key -> Tags
                Author.key -> Author
                Knight.key -> Knight
                Translate.key -> Translate
                else -> null
            }
        }
    }
}