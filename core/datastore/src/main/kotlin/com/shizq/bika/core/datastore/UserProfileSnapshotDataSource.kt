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
        snapshot.updateData { UserProfileSnapshot() }
    }
}
