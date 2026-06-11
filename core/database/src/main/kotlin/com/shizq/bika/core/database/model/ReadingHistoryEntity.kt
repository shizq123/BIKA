package com.shizq.bika.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Instant

@Entity(tableName = "readingHistory")
data class ReadingHistoryEntity(
    @PrimaryKey
    val id: String,

    // 标题
    val title: String,

    // 作者或其他副标题信息
    val author: String,

    // 封面图片 URL
    val coverUrl: String,

    // 最后交互时间（用于在历史记录列表中排序）
    val lastInteractionAt: Instant,

    // 分类列表
    val categories: List<String> = emptyList(),

    // 总页数
    val pagesCount: Int = 0,

    // 章节总数
    val epsCount: Int = 0,

    // 是否已完结
    val finished: Boolean = false,

    // 总点赞数
    val totalLikes: Int = 0,

    // 是否被收藏了
    val isFavourited: Boolean = false
)