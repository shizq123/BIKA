package com.shizq.bika.feature.comicdetail.impl.detail

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.shizq.bika.core.designsystem.theme.BikaTheme
import com.shizq.bika.feature.comicdetail.impl.ComicDetail

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CoverPreviewSample() {
    BikaTheme(dynamicColor = false) {
        Surface(modifier = Modifier.safeDrawingPadding()) {
            DetailTab(
                detail = ComicDetail(
                    id = "cover-preview-sample",
                    title = "星海旅人",
                    cover = "file:///android_asset/cover_preview_sample.png",
                    author = "星野工作室",
                    chineseTeam = "星光汉化组",
                    categories = listOf("长篇"),
                    description = "一段关于星空、旅途与相遇的故事。",
                    finished = true,
                    epsCount = 12,
                    pagesCount = 240,
                ),
                recommendations = emptyList(),
                onRecommendedComicClick = {},
                onTranslateClick = {},
                onAuthorClick = {},
                onUploaderClick = { _, _ -> },
                navigationToTagBlock = {},
            )
        }
    }
}
