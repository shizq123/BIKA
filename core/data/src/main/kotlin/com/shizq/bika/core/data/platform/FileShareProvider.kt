package com.shizq.bika.core.data.platform

import java.io.File

/**
 * 系统分享能力的抽象。data 层只管产出文件，具体怎么调起系统分享面板
 * 是平台细节，交给实现类处理（Intent / FileProvider 之类）。
 */
interface FileShareProvider {
    /**
     * 通过系统分享面板分享 [file]。
     *
     * @param mimeType 分享的 MIME 类型
     * @param title 分享面板标题
     */
    fun share(file: File, mimeType: String, title: String)
}
