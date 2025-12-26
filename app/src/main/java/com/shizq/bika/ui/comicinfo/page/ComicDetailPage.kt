package com.shizq.bika.ui.comicinfo.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import coil3.compose.AsyncImage
import com.shizq.bika.R
import com.shizq.bika.core.network.model.ComicData
import com.shizq.bika.ui.comicinfo.ComicDetail

@Composable
fun ComicDetailPage(
    detail: ComicDetail,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            AsyncImage(
                model = detail.creator.avatar.originalImageUrl,
                contentDescription = stringResource(R.string.comic_creator_avatar),
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
            ) {
                Text(
                    text = stringResource(R.string.comic_author_label, detail.author),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.comic_uploader_label, detail.creator.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (detail.description.isNotBlank()) {
            Text(
                text = detail.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val allTags = detail.tags + detail.categories
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy((-8).dp),
        ) {
            allTags.fastForEach { tag ->
                AssistChip(
                    onClick = { /* TODO: Handle tag click */ },
                    label = { Text(text = tag, style = MaterialTheme.typography.labelLarge) }
                )
            }
        }
    }
}

@Preview(showSystemUi = false, showBackground = true)
@Composable
private fun ComicDetailPagePreview() {
    val detail = ComicDetail(
        creator = ComicData.Comic.Creator(
            id = "594d1d1a8a452814b577c033",
            gender = "bot",
            name = "Atheist",
            title = "我",
            exp = 3272662,
            level = 181,
            characters = listOf(
                "knight",
                "vip",
                "streamer"
            ),
            role = "knight",
            slogan = "頭像是彼之初的男主—黑羽\n第八集1分57秒的截圖🥰😘",
        ),
        title = "セックスが好きで好きで大好きなクラスメイトのあの娘 FANZA特装版",
        description = "喜歡喜歡最喜歡做愛的那個同班女生\n外表看起來純真無邪，\n可愛到幾乎每天被男生告白的同班同學柊柑奈\n也是我佐野的夢中情人！\n無意間跟她一起應徵上同一個打工的公司，\n居然是A片拍攝助理，\n\n「佐野同學，拜託你…！」\n「和我一起加入吧!?」\n「佐野同學…！我有件事要拜託你!!」\n「我最喜歡色色的事情了…可以把你的肉棒借給我嗎…？」",
        author = "藤村久",
        chineseTeam = "甜族星人赞助汉化、紳士出版",
        categories = listOf(
            "單行本",
            "長篇"
        ),
        tags = listOf(
            "巨乳",
            "學生",
            "黑皮",
            "短髮",
            "口交",
            "顏射",
            "橫切面",
            "中出",
            "潮吹",
            "泳裝",
            "校服",
            "雙馬尾",
            "自慰",
            "性玩具",
            "教室",
            "騎乗",
            "乳交"
        ),
        pagesCount = 515,
        epsCount = 2,
        finished = true,
        totalLikes = 1130,
        totalViews = 86316,
        commentsCount = 63,
        isLiked = false
    )
    ComicDetailPage(detail)
}