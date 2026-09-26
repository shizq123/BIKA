package com.shizq.bika.ui.download

sealed interface DownloadNavState {
    object ComicList : DownloadNavState
    data class ComicDetail(val comicId: String) : DownloadNavState
}