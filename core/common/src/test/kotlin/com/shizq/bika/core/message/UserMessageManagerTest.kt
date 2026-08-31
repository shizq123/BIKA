package com.shizq.bika.core.message

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UserMessageManagerTest {

    private lateinit var manager: UserMessageManager

    @Before
    fun setUp() {
        manager = UserMessageManager()
    }

    private fun message(
        id: String,
        text: String = id,
        duration: MessageDuration = MessageDuration.Short,
        action: MessageAction? = null,
    ) = UserMessage(
        text = UiText.Raw(text),
        duration = duration,
        action = action,
        id = MessageId(id),
    )

    @Test
    fun `初始状态无消息`() {
        assertNull(manager.current.value)
    }

    @Test
    fun `上报后立即成为当前消息`() {
        val m = message("a")
        manager.report(m)
        assertSame(m, manager.current.value)
    }

    @Test
    fun `第二条消息排队而非覆盖`() {
        val first = message("a")
        val second = message("b")
        manager.report(first)
        manager.report(second)

        assertSame(first, manager.current.value)

        manager.onOutcome(first.id, MessageOutcome.Dismissed)
        assertSame(second, manager.current.value)

        manager.onOutcome(second.id, MessageOutcome.Dismissed)
        assertNull(manager.current.value)
    }

    @Test
    fun `同 id 重复上报被合并`() {
        val first = message("dup", text = "第一次")
        val second = message("dup", text = "第二次")
        manager.report(first)
        manager.report(second)

        assertSame(first, manager.current.value)

        // 队列里只有一条，结束后应为空。
        manager.onOutcome(first.id, MessageOutcome.Dismissed)
        assertNull(manager.current.value)
    }

    @Test
    fun `点击操作触发 onPerformed 而非 onDismissed`() {
        var performed = false
        var dismissed = false
        val m = message(
            id = "a",
            action = MessageAction(
                label = UiText.Raw("重试"),
                onPerformed = { performed = true },
                onDismissed = { dismissed = true },
            ),
        )
        manager.report(m)
        manager.onOutcome(m.id, MessageOutcome.ActionPerformed)

        assertTrue(performed)
        assertFalse(dismissed)
    }

    @Test
    fun `主动关闭展示中的消息触发 onDismissed`() {
        var dismissed = false
        val m = message(
            id = "a",
            action = MessageAction(
                label = UiText.Raw("重试"),
                onPerformed = {},
                onDismissed = { dismissed = true },
            ),
        )
        manager.report(m)
        manager.dismiss(m.id)

        assertTrue(dismissed)
        assertNull(manager.current.value)
    }

    @Test
    fun `关闭排队中的消息不触发回调`() {
        var dismissed = false
        val head = message("a")
        val queued = message(
            id = "b",
            action = MessageAction(
                label = UiText.Raw("重试"),
                onPerformed = {},
                onDismissed = { dismissed = true },
            ),
        )
        manager.report(head)
        manager.report(queued)
        manager.dismiss(queued.id)

        assertFalse(dismissed)
        assertSame(head, manager.current.value)

        manager.onOutcome(head.id, MessageOutcome.Dismissed)
        assertNull(manager.current.value)
    }

    @Test
    fun `过期回传被忽略`() {
        val first = message("a")
        val second = message("b")
        manager.report(first)
        manager.report(second)
        manager.dismiss(first.id)

        assertSame(second, manager.current.value)

        // first 已经出队，迟到的回传不应把 second 顶掉。
        manager.onOutcome(first.id, MessageOutcome.Dismissed)
        assertSame(second, manager.current.value)
    }

    @Test
    fun `回调内再次上报不会死锁`() {
        val follow = message("b")
        val m = message(
            id = "a",
            action = MessageAction(
                label = UiText.Raw("重试"),
                onPerformed = { manager.report(follow) },
            ),
        )
        manager.report(m)
        manager.onOutcome(m.id, MessageOutcome.ActionPerformed)

        assertSame(follow, manager.current.value)
    }

    @Test
    fun `clear 清空队列且不触发回调`() {
        var dismissed = false
        val m = message(
            id = "a",
            action = MessageAction(
                label = UiText.Raw("重试"),
                onPerformed = {},
                onDismissed = { dismissed = true },
            ),
        )
        manager.report(m)
        manager.report(message("b"))
        manager.clear()

        assertNull(manager.current.value)
        assertFalse(dismissed)
    }

    @Test
    fun `相等性只看 id`() {
        assertEquals(message("a", text = "x"), message("a", text = "y"))
        assertEquals(message("a").hashCode(), message("a", text = "y").hashCode())
        assertFalse(message("a") == message("b"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Indefinite 缺少 action 时构造失败`() {
        message("a", duration = MessageDuration.Indefinite)
    }
}
