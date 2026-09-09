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

    override val lastReadHistory: Flow<DetailedReadingHistory?> =
        historyDao.getLatestDetailedHistory()
            .map { history -> history?.asExternalModel() }
            .distinctUntilChanged()

    override val activeChannels: Flow<List<Channel>> =
        userPreferencesDataSource.userData
            .map { prefs -> prefs.dashboard.channels.filter { it.isActive } }
            .distinctUntilChanged()

    override val favoriteTags: Flow<List<FavoriteTag>> =
        userPreferencesDataSource.userData
            .map { it.filter.favoriteTags }
            .distinctUntilChanged()

    override val autoCheckInEnabled: Flow<Boolean> =
        userPreferencesDataSource.userData
            .map { it.app.autoCheckIn }
            .distinctUntilChanged()

    override suspend fun updateFavoriteTags(
        transform: (List<FavoriteTag>) -> List<FavoriteTag>,
    ) = userPreferencesDataSource.updateFavoriteTags(transform)
}
