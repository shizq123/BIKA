package com.shizq.bika.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class History(
    @ColumnInfo(name = "time")
    var time: Long = 0,

    @ColumnInfo(name = "title")
    var title: String,

    @ColumnInfo(name = "fileServer")
    var fileServer: String,

    @ColumnInfo(name = "path")
    var path: String,

    @ColumnInfo(name = "comic_or_game")
    var comic_or_game: String,

    @ColumnInfo(name = "author")
    var author: String,

    @ColumnInfo(name = "comic_or_game_id")
    var comic_or_game_id: String,

    @ColumnInfo(name = "sort")//分类
    var sort: String,

    @ColumnInfo(name = "epsCount")//总章
    var epsCount: String,

    @ColumnInfo(name = "pagesCount")//总页
    var pagesCount: String,

    @ColumnInfo(name = "finished")//是否完结
    var finished: Boolean,

    @ColumnInfo(name = "likeCount")//爱心数
    var likeCount: String,

    @ColumnInfo(name = "ep")//观看到第几章
    var ep: String,

    @ColumnInfo(name = "page")//观看到第几页
    var page: String,
){
    @PrimaryKey(autoGenerate = true)
    var id:Long=0
}