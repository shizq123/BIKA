package com.shizq.bika.ui.comiclist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.shizq.bika.core.designsystem.theme.BikaTheme
import com.shizq.bika.ui.comicinfo.ComicInfoActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * 漫画列表
 */
@AndroidEntryPoint
class ComicListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            BikaTheme {
                TopicScreen(
                    onBackClick = ::finish,
                    navigationToComicInfo = { ComicInfoActivity.start(this, it) }
                )
            }
        }
    }

    companion object {
        fun start(context: Context, tag: String, title: String, value: String) {
            val intent = Intent(context, ComicListActivity::class.java)
            intent.putExtra("tag", tag)
            intent.putExtra("title", title)
            intent.putExtra("value", value)
            context.startActivity(intent)
        }
    }
}