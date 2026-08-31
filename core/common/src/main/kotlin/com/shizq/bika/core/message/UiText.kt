package com.shizq.bika.core.message

import android.content.Context
import androidx.annotation.StringRes

/**
 * 延迟解析的文案。让 data 层可以在不持有 [Context] 的前提下描述用户可见文本，
 * 真正的本地化在 UI 层通过 [asString] 完成。
 */
sealed interface UiText {
    /** 已经确定的字面量，通常来自服务端下发。 */
    data class Raw(val value: String) : UiText

    /** 资源引用，[args] 用于 `getString(id, *args)` 的占位符填充。 */
    data class Res(
        @StringRes val id: Int,
        val args: List<Any> = emptyList(),
    ) : UiText

    companion object {
        fun of(value: String): UiText = Raw(value)

        fun of(@StringRes id: Int, vararg args: Any): UiText = Res(id, args.toList())
    }
}

fun UiText.asString(context: Context): String = when (this) {
    is UiText.Raw -> value
    is UiText.Res -> if (args.isEmpty()) {
        context.getString(id)
    } else {
        context.getString(id, *args.toTypedArray())
    }
}
