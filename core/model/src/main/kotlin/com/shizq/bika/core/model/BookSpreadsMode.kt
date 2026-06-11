package com.shizq.bika.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class BookSpreadsMode(val label: String) {
    SINGLE("单页模式"),
    DOUBLE("双页模式"),
    AUTO("自动自适应")
}
