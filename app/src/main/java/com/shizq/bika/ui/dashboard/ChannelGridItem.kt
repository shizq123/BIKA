package com.shizq.bika.ui.dashboard

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.shizq.bika.R
import com.shizq.bika.core.model.Channel

@Composable
fun ChannelGridItem(
    iconRes: Int,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .padding(8.dp)
            .width(IntrinsicSize.Min)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            Image(
                painterResource(iconRes),
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

object ChannelIconRegistry {
    private val iconMap = mapOf(
        "ic_bika" to R.drawable.ic_bika,
        "ic_cat_ranking" to R.drawable.ic_cat_ranking,
        "ic_cat_message_board" to R.drawable.ic_cat_message_board,
        "ic_cat_recent" to R.drawable.ic_cat_recent,
        "ic_cat_random" to R.drawable.ic_cat_random,

        "ic_cat_trending" to R.drawable.ic_cat_trending,
        "ic_cat_master_choice" to R.drawable.ic_cat_master_choice,
        "ic_cat_history" to R.drawable.ic_cat_history,
        "ic_cat_staff_pick" to R.drawable.ic_cat_staff_pick,
        "ic_cat_translated" to R.drawable.ic_cat_translated,

        "ic_cat_full_color" to R.drawable.ic_cat_full_color,
        "ic_cat_long" to R.drawable.ic_cat_long,
        "ic_cat_doujin" to R.drawable.ic_cat_doujin,
        "ic_cat_short" to R.drawable.ic_cat_short,
        "ic_cat_tankoubon" to R.drawable.ic_cat_tankoubon,
        "ic_cat_cg" to R.drawable.ic_cat_cg,
        "ic_cat_english" to R.drawable.ic_cat_english,
        "ic_cat_raw" to R.drawable.ic_cat_raw,
        "ic_cat_webtoon" to R.drawable.ic_cat_webtoon,
        "ic_cat_western" to R.drawable.ic_cat_western,
        "ic_cat_cosplay" to R.drawable.ic_cat_cosplay,

        "ic_cat_vanilla" to R.drawable.ic_cat_vanilla,
        "ic_cat_yuri" to R.drawable.ic_cat_yuri,
        "ic_cat_yaoi" to R.drawable.ic_cat_yaoi,
        "ic_cat_crossdress" to R.drawable.ic_cat_crossdress,
        "ic_cat_harem" to R.drawable.ic_cat_harem,
        "ic_cat_futanari" to R.drawable.ic_cat_futanari,
        "ic_cat_sister_big" to R.drawable.ic_cat_sister_big,
        "ic_cat_sister_little" to R.drawable.ic_cat_sister_little,
        "ic_cat_bdsm" to R.drawable.ic_cat_bdsm,
        "ic_cat_gender_bender" to R.drawable.ic_cat_gender_bender,
        "ic_cat_foot" to R.drawable.ic_cat_foot,
        "ic_cat_milf" to R.drawable.ic_cat_milf,
        "ic_cat_ntr" to R.drawable.ic_cat_ntr,
        "ic_cat_forced" to R.drawable.ic_cat_forced,
        "ic_cat_monster" to R.drawable.ic_cat_monster,
        "ic_cat_hardcore" to R.drawable.ic_cat_hardcore,

        "ic_cat_madoka" to R.drawable.ic_cat_madoka,
        "ic_cat_granblue" to R.drawable.ic_cat_granblue,
        "ic_cat_kancolle" to R.drawable.ic_cat_kancolle,
        "ic_cat_lovelive" to R.drawable.ic_cat_lovelive,
        "ic_cat_sao" to R.drawable.ic_cat_sao,
        "ic_cat_fate" to R.drawable.ic_cat_fate,
        "ic_cat_touhou" to R.drawable.ic_cat_touhou,
        "ic_cat_index" to R.drawable.ic_cat_index,
    )

    @DrawableRes
    fun getIconResId(resName: String): Int {
        return iconMap[resName] ?: R.drawable.placeholder_avatar_2
    }
}

val Channel.iconResId: Int
    get() = ChannelIconRegistry.getIconResId(resName)