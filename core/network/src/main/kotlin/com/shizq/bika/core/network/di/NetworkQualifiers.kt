package com.shizq.bika.core.network.di

import jakarta.inject.Qualifier

/**
 * 图片链路专用客户端的限定符。
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ImageClient

/** GitHub 发布信息/APK 下载专用客户端的限定符。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GithubClient

/** DNS 解析专用客户端的限定符。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DnsClient
