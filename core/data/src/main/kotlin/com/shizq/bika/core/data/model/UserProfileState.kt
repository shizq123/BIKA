package com.shizq.bika.core.data.model

import com.shizq.bika.core.model.preferences.UserProfileSnapshot

/**
 * 仓储对外发布的用户资料。
 *
 * [hasCheckedIn] 可空是这个模型的要点：快照里没有 isPunched（打卡状态是当天的，
 * 缓存下来必然过时），所以走缓存兜底时它只能是 null —— 「不知道」而不是「未打卡」。
 *
 * 原先这件事由两个字段合起来表达：映射时硬编码 `hasCheckedIn = false`，再靠 UI 层
 * 一个独立的 `isOfflineCache` 布尔把自动打卡短路掉。两个字段各自可写，谁漏了一个
 * 编译器都不管，结果是拿一个陈旧的「未打卡」去发打卡请求。收成一个可空字段后，
 * 「离线」和「打卡状态不可知」不再可能各说一套。
 */
data class UserProfileState(
    val profile: UserProfileSnapshot,
    val hasCheckedIn: Boolean?,
)
