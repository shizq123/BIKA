package com.shizq.bika.core.model.preferences

import com.shizq.bika.core.model.Channel
import com.shizq.bika.core.model.DefaultChannels
import kotlinx.serialization.Serializable

/** 首页展示相关的用户配置。 */
@Serializable
data class DashboardPreferences(
    val channels: List<Channel> = DefaultChannels.all,
)
