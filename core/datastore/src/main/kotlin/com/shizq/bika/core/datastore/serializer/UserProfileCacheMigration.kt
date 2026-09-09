package com.shizq.bika.core.datastore.serializer

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import com.shizq.bika.core.model.preferences.UserPreferences
import com.shizq.bika.core.model.preferences.UserProfileSnapshot
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.first

private val logger = KotlinLogging.logger("ProfileCacheMigration")

/**
 * 把资料快照从 `user_preferences` 搬到独立的 `user_profile_cache`。
 *
 * 不直接丢弃旧值：丢了的表现是「升级后首次冷启动且无网时资料卡空白」，
 * 而这恰好是这份缓存唯一的存在理由。
 *
 * [legacyPreferences] 用 lambda 传入而非直接注入 DataStore：迁移在 profile store
 * 首次读取时执行，此时若同步依赖另一个 DataStore 实例会把两者的初始化耦合起来。
 */
internal class UserProfileCacheMigration(
    private val legacyPreferences: () -> DataStore<UserPreferences>,
) : DataMigration<UserProfileSnapshot> {

    /**
     * name 为空即「尚无可用缓存」，与 UserRepository 判定缓存可用性的口径一致。
     * 迁移成功后 name 非空，后续冷启动不再进入。
     */
    override suspend fun shouldMigrate(currentData: UserProfileSnapshot): Boolean =
        currentData.name.isEmpty()

    override suspend fun migrate(currentData: UserProfileSnapshot): UserProfileSnapshot {
        val legacy = readLegacy() ?: return currentData
        // 旧文件里没存过资料时不覆盖，否则会把 currentData 换成一份等价的空值、
        // 白写一次文件。
        if (legacy.name.isEmpty()) return currentData
        logger.info { "已将用户资料快照迁移到独立存储" }
        return legacy
    }

    /**
     * 迁移落盘后再清理旧字段：反过来做的话，清理成功而迁移写入失败就等于丢缓存。
     */
    override suspend fun cleanUp() {
        runCatching {
            val store = legacyPreferences()
            // 无旧值时直接返回，避免每次冷启动都对偏好文件做一次等值重写。
            if (store.data.first().profile == UserProfileSnapshot()) return
            store.updateData { it.copy(profile = UserProfileSnapshot()) }
        }.onFailure {
            // 清理失败只是留了份废数据，不影响新存储可用，不该让 DataStore 初始化失败。
            logger.warn(it) { "清理 user_preferences 中的旧资料快照失败" }
        }
    }

    private suspend fun readLegacy(): UserProfileSnapshot? = runCatching {
        legacyPreferences().data.first().profile
    }.onFailure {
        logger.warn(it) { "读取旧资料快照失败，将以空缓存启动" }
    }.getOrNull()
}
