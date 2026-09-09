package com.shizq.bika.core.data.repository

import com.shizq.bika.core.data.model.DetailedReadingHistory
import com.shizq.bika.core.model.Channel
import com.shizq.bika.core.model.FavoriteTag
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {

    /**
     * 最近一条阅读记录，无记录时为 null。
     *
     * 返回 data 层的 [DetailedReadingHistory] 而非 Room 的 DetailedHistory：
     * 后者带 @Embedded / @Relation 注解，属于持久化细节。
     */
    val lastReadHistory: Flow<DetailedReadingHistory?>

    /** 用户在频道设置里勾选保留的频道，已按 isActive 过滤。 */
    val activeChannels: Flow<List<Channel>>

    /** 收藏的标签。读写都经由本接口，避免读一条路径、写另一条路径。 */
    val favoriteTags: Flow<List<FavoriteTag>>

    /**
     * 是否启用自动打卡。
     *
     * 设置页早已能写这个开关（UserPreferencesDataSource.setAutoCheckIn），但此前
     * 没有任何读取方：AutoCheckIn 只看「资料已加载且未打卡」就发请求，关掉开关
     * 照样打卡。这里把它暴露出来，由 DashboardStateMachine 在打卡前判定。
     */
    val autoCheckInEnabled: Flow<Boolean>

    /**
     * 以原子的读-改-写方式更新收藏标签。
     *
     * [transform] 在 DataStore 事务内执行，必须是纯函数且不可阻塞。
     * 快速连续点击不会因「先读快照再整表写回」而丢更新。
     */
    suspend fun updateFavoriteTags(transform: (List<FavoriteTag>) -> List<FavoriteTag>)
}
