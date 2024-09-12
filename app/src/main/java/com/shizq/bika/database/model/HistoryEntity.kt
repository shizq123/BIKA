package com.shizq.bika.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "HISTORY")
data class HistoryEntity(
    @ColumnInfo(name = "time")
    val time: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "fileServer")
    val fileServer: String,

    @ColumnInfo(name = "path")
    val path: String,

    @ColumnInfo(name = "comic_or_game")
    val comic_or_game: String,

    @ColumnInfo(name = "author")
    val author: String,

    @ColumnInfo(name = "comic_or_game_id")
    val comic_or_game_id: String,

    @ColumnInfo(name = "sort")//分类
    val sort: String,

    @ColumnInfo(name = "epsCount")//总章
    val epsCount: String,

    @ColumnInfo(name = "pagesCount")//总页
    val pagesCount: String,

    @ColumnInfo(name = "finished")//是否完结
    val finished: Boolean,

    @ColumnInfo(name = "likeCount")//爱心数
    val likeCount: String,

    @ColumnInfo(name = "ep")//观看到第几章
    val ep: String,

    @ColumnInfo(name = "page")//观看到第几页
    val page: String,
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
)
