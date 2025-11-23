package com.shizq.bika.core.model

/**
 * 屏幕点击区域布局策略.
 *
 * 定义了当用户点击屏幕不同位置时，应该触发何种操作（上一页、下一页、呼出菜单）。
 * 该枚举负责将点击坐标 (x, y) 映射为 [ReaderAction]。
 *
 * @property label 用于在设置界面显示的中文名称.
 */
enum class TapZoneLayout(val label: String) {

    /**
     * **经典三段式布局**.
     *
     * 将屏幕垂直划分为左、中、右三个区域。
     * - **中间 (34%)**: 呼出菜单
     * - **两侧 (33%)**: 翻页
     *
     * 示意图 (LTR):
     * ```
     * |  PREV  |  MENU  |  NEXT  |
     * | (33%)  | (34%)  | (33%)  |
     * ```
     */
    Sides("两侧"),

    /**
     * **Kindle 经典布局**.
     *
     * 专为沉浸式阅读设计，最大化“下一页”的触发面积。
     * - **顶部 (15%)**: 呼出菜单
     * - **左侧窄条 (20%)**: 上一页 (用于回看)
     * - **剩余大面积**: 下一页 (方便盲按)
     *
     * 示意图:
     * ```
     * |      MENU (Top 15%)      |
     * |--------------------------|
     * | PREV |                   |
     * | (Left|      NEXT         |
     * | 20%) |   (Rest Area)     |
     * |      |                   |
     * ```
     */
    Kindle("Kindle"),

    /**
     * **L 形布局 (单手模式)**.
     *
     * 专为单手持握优化。核心理念是：无论左手还是右手持机，拇指最自然的活动区域（屏幕下方）
     * 全部用于触发最高频的“下一页”操作。
     *
     * - **中心**: 呼出菜单
     * - **上方 & 反侧**: 上一页
     * - **下方 & 同侧**: 下一页 (形成一个 L 形包裹中心)
     *
     * 示意图 (RTL 日漫模式，下一页在左):
     * ```
     * | PREV |  MENU  | PREV |
     * |------+--------+------|
     * |      |  MENU  | PREV |
     * | NEXT |--------+------|
     * | (L)  |     NEXT      |
     * ```
     */
    LShape("L 形"),

    /**
     * **关闭点击翻页**.
     *
     * 点击屏幕任何位置均视为“呼出/隐藏菜单”。
     * 适用于只习惯通过**滑动 (Swipe)** 手势来翻页，且希望避免误触点击的用户。
     */
    Off("关闭");

    /**
     * 根据坐标和屏幕尺寸解析用户意图.
     *
     * @param x 点击事件的 X 坐标 (相对于组件).
     * @param y 点击事件的 Y 坐标 (相对于组件).
     * @param width 组件总宽度.
     * @param height 组件总高度.
     * @param isRtl 阅读方向是否为从右向左 (Right-to-Left).
     *              - `true`: 日漫模式 (点左边是下一页).
     *              - `false`: 国漫/条漫模式 (点右边是下一页).
     * @return [ReaderAction] 对应的动作指令.
     */
    fun resolve(
        x: Float,
        y: Float,
        width: Int,
        height: Int,
        isRtl: Boolean
    ): ReaderAction {
        val percentX = x / width.toFloat()
        val percentY = y / height.toFloat()

        return when (this) {
            Sides -> {
                val sideRegion = 0.33f
                if (percentX < sideRegion) {
                    if (isRtl) ReaderAction.NextPage else ReaderAction.PrevPage
                } else if (percentX > (1 - sideRegion)) {
                    if (isRtl) ReaderAction.PrevPage else ReaderAction.NextPage
                } else {
                    ReaderAction.ToggleMenu
                }
            }

            LShape -> {
                // 中间九宫格中心区域 (33% ~ 66%) -> 菜单
                val menuMin = 0.33f
                val menuMax = 0.67f
                if (percentX in menuMin..menuMax && percentY in menuMin..menuMax) {
                    return ReaderAction.ToggleMenu
                }

                val isNextPage = if (isRtl) {
                    // RTL: 除了"顶部"和"最右侧"，其余全是下一页
                    val isTop = percentY < menuMin
                    val isRight = percentX > menuMax
                    !(isTop || isRight)
                } else {
                    // LTR: 除了"顶部"和"最左侧"，其余全是下一页
                    val isTop = percentY < menuMin
                    val isLeft = percentX < menuMin
                    !(isTop || isLeft)
                }
                if (isNextPage) ReaderAction.NextPage else ReaderAction.PrevPage
            }

            Kindle -> {
                // 顶部 15% -> 菜单
                if (percentY < 0.15f) return ReaderAction.ToggleMenu

                // 左侧 20% -> 总是上一页 (或根据习惯调整)
                val isLeftStrip = percentX < 0.2f

                // 这里采用简单逻辑：左侧窄条总是往回翻，大面积总是往后翻
                // 这样符合物理直觉：点左边就是往左翻，点右边(大面积)就是往右翻
                if (isLeftStrip) {
                    if (isRtl) ReaderAction.NextPage else ReaderAction.PrevPage
                } else {
                    if (isRtl) ReaderAction.PrevPage else ReaderAction.NextPage
                }
            }

            Off -> ReaderAction.ToggleMenu
        }
    }
}