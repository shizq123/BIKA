package com.shizq.bika.ui.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class DashboardResultTest {

    /**
     * 本次改动要防住的回归：`message` 提到了 CheckInResult 接口上，消费方
     * （DashboardScreen 打卡对话框）直接读 `result.message`，不再写 when。
     * 若有人把某个分支的字段改回 `error`，或把 `message` 从接口上摘下来，
     * 这里会连编译一起失败——用接口类型持有、不做 when 是这条断言的关键。
     */
    @Test
    fun `两个分支都通过接口暴露 message`() {
        val results: List<CheckInResult> = listOf(
            CheckInResult.Success("打卡成功"),
            CheckInResult.Error("打卡失败"),
        )
        assertEquals(listOf("打卡成功", "打卡失败"), results.map { it.message })
    }

    /**
     * OperationResult.Success 必须保持 data object。
     *
     * DashboardScreen 的两个 LaunchedEffect 用 `OperationResult.Success ->` 这种
     * 等值分支匹配成功态。一旦它变成带参数的 data class（例如为了和 CheckInResult
     * 合并而塞进一个可空 message），等值分支就不再编译通过，须改成 `is`。
     * 这条断言把「不合并」这个决定钉在测试里。
     */
    @Test
    fun `成功态是单例 才能用等值分支匹配`() {
        val a: OperationResult = OperationResult.Success
        val b: OperationResult = OperationResult.Success
        assertSame(a, b)
        assertEquals(OperationResult.Success, a)
    }

    @Test
    fun `失败态按 message 区分`() {
        assertEquals(OperationResult.Error("网络错误"), OperationResult.Error("网络错误"))
        assertEquals(CheckInResult.Error("网络错误"), CheckInResult.Error("网络错误"))
    }
}
