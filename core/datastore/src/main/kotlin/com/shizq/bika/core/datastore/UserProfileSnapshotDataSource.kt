package com.shizq.bika.core.datastore

import androidx.datastore.core.DataStore
import com.shizq.bika.core.model.preferences.UserProfileSnapshot
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * 本地留存的用户资料快照。
 */
class UserProfileSnapshotDataSource @Inject constructor(
    private val snapshot: DataStore<UserProfileSnapshot>,
) {
    val profile: Flow<UserProfileSnapshot> = snapshot.data

    /** 保存用户资料到本地，供无网时回退展示。 */
    suspend fun store(profile: UserProfileSnapshot) {
        snapshot.updateData { profile }
    }

    /** 清空缓存。登出时调用，避免下一个账号看到上一个账号的资料卡。 */
    suspend fun clear() {
        // 保留 legacyMigrationDone：登出只是清空业务字段，不代表"从未完成过旧存储迁移"。
        // 若这里整体重置为默认值，下次冷启动会被 UserProfileCacheMigration 误判为需要
        // 重新迁移，进而可能把上一个账号残留在旧存储里的资料快照恢复回来。
        snapshot.updateData { current ->
            UserProfileSnapshot(legacyMigrationDone = current.legacyMigrationDone)
        }
    }
}
