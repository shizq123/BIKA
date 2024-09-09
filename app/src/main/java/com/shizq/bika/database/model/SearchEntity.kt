package com.shizq.bika.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Search")
data class SearchEntity(
    @ColumnInfo(name = "text")
    val text: String,
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0
)
