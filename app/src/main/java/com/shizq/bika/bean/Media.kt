package com.shizq.bika.bean;

import kotlinx.serialization.SerialName;
import kotlinx.serialization.Serializable;

@Serializable
data class Media(
    @SerialName("fileServer")
    val fileServer: String = "",
    @SerialName("originalName")
    val originalName: String = "",
    @SerialName("path")
    val path: String = ""
)