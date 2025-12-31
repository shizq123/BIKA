package com.shizq.bika.ui.comiclist

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity

/**
 * 漫画列表
 */

class ComicListActivity : ComponentActivity() {

    companion object {
        private var tag = arrayOf<CharSequence>(
            "全彩",
            "長篇",
            "同人",
            "短篇",
            "圓神領域",
            "碧藍幻想",
            "CG雜圖",
            "英語 ENG",
            "生肉",
            "純愛",
            "百合花園",
            "耽美花園",
            "偽娘哲學",
            "後宮閃光",
            "扶他樂園",
            "單行本",
            "姐姐系",
            "妹妹系",
            "SM",
            "性轉換",
            "足の恋",
            "人妻",
            "NTR",
            "強暴",
            "非人類",
            "艦隊收藏",
            "Love Live",
            "SAO 刀劍神域",
            "Fate",
            "東方",
            "WEBTOON",
            "禁書目錄",
            "歐美",
            "Cosplay",
            "重口地帶"
        )
        private var tagInitial = booleanArrayOf(
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false
        )

        fun start(context: Context, tag: String, title: String, value: String) {
            val intent = Intent(context, ComicListActivity::class.java)
            intent.putExtra("tag", tag)
            intent.putExtra("title", title)
            intent.putExtra("value", value)
            context.startActivity(intent)
        }
    }
}