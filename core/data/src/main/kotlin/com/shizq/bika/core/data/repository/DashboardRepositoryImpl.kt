package com.shizq.bika.core.data.repository

import com.shizq.bika.core.data.model.DetailedReadingHistory
import com.shizq.bika.core.data.model.asExternalModel
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.model.Channel
import com.shizq.bika.core.model.FavoriteTag
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class DashboardRepositoryImpl @Inject constructor(
    private val historyDao: ReadingHistoryDao,
    private val userPreferencesDataSource: UserPreferencesDataSource,
) : DashboardRepository {

    /**
     * DAO 已按 lastInteractionAt DESC 排序，取首条即最近记录。
     * distinctUntilChanged：历史表任一行变动都会让 DAO 重新发射整表，
     * 但只要首条没变就不该惊动下游。
     */
    override val lastReadHistory: Flow<DetailedReadingHistory?> =
        historyDao.getDetailedHistories()
            .map { histories -> histories.firstOrNull()?.asExternalModel() }
            .distinctUntilChanged()

    override val activeChannels: Flow<List<Channel>> =
        userPreferencesDataSource.userData
            .map { prefs -> prefs.dashboard.channels.filter { it.isActive } }
            .distinctUntilChanged()

    override val favoriteTags: Flow<List<FavoriteTag>> =
        userPreferencesDataSource.userData
            .map { it.filter.favoriteTags }
            .distinctUntilChanged()

    override suspend fun updateFavoriteTags(
        transform: (List<FavoriteTag>) -> List<FavoriteTag>,
    ) = userPreferencesDataSource.updateFavoriteTags(transform)
}
