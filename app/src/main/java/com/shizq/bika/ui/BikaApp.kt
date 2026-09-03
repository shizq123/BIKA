package com.shizq.bika.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.shizq.bika.core.ui.message.UserMessageEffect
import com.shizq.bika.navigation.AuthenticationRoute
import com.shizq.bika.navigation.authenticationSection
import com.shizq.bika.navigation.featureSection

val LocalUseBackAnimation = compositionLocalOf { false }

/**
 * 应用根节点。
 *
 * ## 图的选择由会话状态决定，不由返回栈决定
 *
 * [isLoggedIn] 直接来自 token 是否存在（`MainActivityViewModel`）。认证图与主图
 * 是互斥状态而非前后关系，因此这里用 `if` 选择要挂哪个 [NavDisplay]，而不是把
 * 两者塞进同一个返回栈。这样 401 清 token 之后界面会自动回到登录页——旧实现里
 * 这条链路是断的：`startDestination` 变了，但 `rememberNavigationState` 的
 * `remember` key 是常量，导航状态不会重建。
 *
 * ## 返回键
 *
 * 不再有自定义 `BackHandler`。栈已经扁平化，[NavDisplay] 自带的返回处理就是正确
 * 行为：栈深 > 1 时拦截并出栈，栈深 == 1 时不拦截，交给系统退出应用。旧实现里
 * BikaApp、根 NavDisplay、内层 NavDisplay 三层返回处理重叠，靠「内层优先」才没
 * 出现双重出栈。
 */
@Composable
fun BikaApp(
    appState: AppState,
    isLoggedIn: Boolean,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    usePredictiveBack: Boolean = false,
) {
    val navigator = appState.navigator

    // 会话切换时清理另一侧的栈：
    // - 登录后清认证栈，避免下次登出时直接落在注册页（用户可能是从注册页登进来的）
    // - 登出后清主图栈，避免下个账号进来还停在上一个账号的页面上
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            navigator.resetAuthenticationStack()
        } else {
            navigator.resetContentStack()
        }
    }

    CompositionLocalProvider(LocalUseBackAnimation provides usePredictiveBack) {
        Scaffold(
            modifier = modifier.semantics {
                testTagsAsResourceId = true
            },
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                val snackbarHostState = remember { SnackbarHostState() }
                UserMessageEffect(
                    source = appState.messageSource,
                    snackbarHostState = snackbarHostState,
                )
                SnackbarHost(
                    snackbarHostState,
                    modifier = Modifier.windowInsetsPadding(
                        WindowInsets.safeDrawing.exclude(
                            WindowInsets.ime,
                        ),
                    ),
                )
            },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal,
                        ),
                    ),
            ) {
                if (isLoggedIn) {
                    NavDisplay(
                        backStack = navigator.contentBackStack,
                        // ChannelSettingsNavKey 用 DialogSceneStrategy.dialog() 标记，
                        // 缺了这个策略它会被当成普通全屏页面渲染。
                        sceneStrategy = remember { DialogSceneStrategy<NavKey>() },
                        entryProvider = entryProvider {
                            featureSection(
                                navigator = navigator,
                                onLogout = onLogout,
                                useAnimation = usePredictiveBack,
                            )
                        },
                    )
                } else {
                    NavDisplay(
                        backStack = navigator.authenticationBackStack,
                        entryProvider = entryProvider {
                            authenticationSection(
                                navigateToRegister = {
                                    navigator.navigate(AuthenticationRoute.RegisterRoute)
                                },
                                onBackClick = navigator::goBackAuthentication,
                                useAnimation = usePredictiveBack,
                            )
                        },
                    )
                }
            }
        }
    }
}
