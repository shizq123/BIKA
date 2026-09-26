package com.shizq.bika.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

/**
 * 创建可跨配置变更与进程死亡存活的导航状态。
 *
 * 两个栈都由 `rememberNavBackStack` 持有，因此各自独立地被 saved state 保存。
 */
@Composable
fun rememberNavigator(): Navigator {
    val authenticationBackStack = rememberNavBackStack(AuthenticationRoute.LoginRoute)
    val contentBackStack = rememberNavBackStack(ConnectedRoute.DashboardRoute)

    return remember(authenticationBackStack, contentBackStack) {
        Navigator(
            authenticationBackStack = authenticationBackStack,
            contentBackStack = contentBackStack,
        )
    }
}

/**
 * 导航状态的持有者与唯一修改入口。
 *
 * ## 为什么没有「根返回栈」
 *
 * 认证图与主图之间不存在导航语义：你不能从主图「返回」到登录页，也不能反向
 * 返回。两者是互斥状态，由 token 是否存在唯一决定。用返回栈表达互斥状态会
 * 导致 `navigate` 只能靠「当前在哪」猜「要去哪」，因此这里不再有 rootBackStack——
 * 图的选择在 `BikaApp` 里由会话状态直接 `when` 出来。
 *
 * 相应地，本类不提供切换图的方法。进入主图靠登录写入 token，回到认证图靠
 * [com.shizq.bika.core.network.auth.SessionManager] 清除 token。
 *
 * ## 为什么只有一个内容栈
 *
 * 主图只有一个 top-level 路由（Dashboard），导航全部靠侧边栏与列表项向前推进。
 * 多 top-level 栈（每个 Tab 一个栈、"exit through home"）的机制在这种结构下
 * 全程空转，因此收敛为单个 [contentBackStack]。
 */
@Stable
class Navigator(
    val authenticationBackStack: NavBackStack<NavKey>,
    val contentBackStack: NavBackStack<NavKey>,
) {
    /**
     * 在认证图内导航。
     */
    fun navigate(route: Authentication) {
        authenticationBackStack.add(route)
    }

    /**
     * 在主图内导航。
     *
     * 相邻去重：详情页的「为你推荐」等入口可能把当前路由再压一次，重复条目
     * 除了让返回键空按一次没有别的作用。
     */
    fun navigate(route: Connected) {
        if (contentBackStack.lastOrNull() == route) return
        contentBackStack.add(route)
    }

    /**
     * 在认证图内返回。
     */
    fun goBackAuthentication() {
        authenticationBackStack.removeLastOrNull()
    }

    /**
     * 在主图内返回。
     */
    fun goBack() {
        contentBackStack.removeLastOrNull()
    }

    /**
     * 登录成功后清理认证栈。
     *
     * 若用户是从注册页完成登录的，栈顶会停在 RegisterRoute；下次被登出时
     * 认证图会直接显示注册页而不是登录页。
     */
    fun resetAuthenticationStack() {
        authenticationBackStack.clear()
        authenticationBackStack.add(AuthenticationRoute.LoginRoute)
    }

    /**
     * 登出/会话终止后清理主图栈，避免下次登录进来还停在上一个账号的页面上。
     */
    fun resetContentStack() {
        contentBackStack.clear()
        contentBackStack.add(ConnectedRoute.DashboardRoute)
    }
}
