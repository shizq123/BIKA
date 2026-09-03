package com.shizq.bika.navigation

import kotlinx.serialization.Serializable

/**
 * 以对话框形式展示的目标。仍然进主图的返回栈，因此返回键/点击遮罩都走
 * [com.shizq.bika.navigation.Navigator.goBack]。
 *
 * 密封是为了让每个实现都必须在 `featureSection` 里注册对应 entry——
 * 否则 `entryProvider` 会在运行时才发现缺失。
 */
sealed interface DialogNavKey : Connected

@Serializable
data object ChannelSettingsNavKey : DialogNavKey