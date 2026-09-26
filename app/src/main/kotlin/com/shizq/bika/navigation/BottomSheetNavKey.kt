package com.shizq.bika.navigation

import kotlinx.serialization.Serializable

sealed interface BottomSheetNavKey : Connected

@Serializable
data class EpisodeDownloadSheetNavKey(
    val comicId: String,
    val comicTitle: String,
    val coverUrl: String,
) : BottomSheetNavKey