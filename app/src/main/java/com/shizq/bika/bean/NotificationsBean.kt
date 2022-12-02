package com.shizq.bika.bean

//没收到过消息 无法分析数据
data class NotificationsBean(
    val notifications: Notifications
) {
    data class Notifications(
        val docs: List<Any>,
        val limit: Int,
        val page: Int,
        val pages: Int,
        val total: Int
    )
}

