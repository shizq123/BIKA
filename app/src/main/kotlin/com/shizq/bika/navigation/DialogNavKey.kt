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

/**
 * 修改资料对话框。
 *
 * [initialSlogan] 进 key 而不是让对话框自己去读 profile：key 会被序列化进返回栈，
 * 进程死亡后重建仍是打开那一刻的签名，且省掉一次异步读取导致的输入框闪烁。
 */
@Serializable
data class EditProfileNavKey(val initialSlogan: String) : DialogNavKey

@Serializable
data object ChangePasswordNavKey : DialogNavKey

@Serializable
data class TagBlockDialogNavKey(val tag: String) : DialogNavKey