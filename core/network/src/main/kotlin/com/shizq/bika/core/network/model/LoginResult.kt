package com.shizq.bika.core.network.model

import kotlinx.serialization.Serializable

/**
 * 登录结果：成功拿到 token，或被服务端以业务方式拒绝（如账号密码不对）。
 *
 * 网络层/传输层的异常（超时、DNS 失败等）不在此类型内表达，而是照常
 * 从 [com.shizq.bika.core.network.BikaDataSource.login] 抛出，交给调用方
 * 统一的异常处理路径，不要与"表单提交被拒绝"这种业务结果混在一起。
 */
sealed interface LoginResult {
    data class Success(val token: String) : LoginResult
    data class Rejected(val reason: String) : LoginResult
}

/** `auth/sign-in` 信封 `data` 字段的载荷形状。 */
@Serializable
internal data class LoginTokenPayload(val token: String)

@Serializable
data class SignInData(
    val token: String = "",
    val question1: String = "",
    val question2: String = "",
    val question3: String = "",
    val password: String = ""
)