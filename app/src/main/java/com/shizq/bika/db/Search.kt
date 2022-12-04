package com.shizq.bika.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class Search(

    @ColumnInfo(name = "text")
    var text: String
){
    @PrimaryKey(autoGenerate = true)
    var id:Long=0
}
