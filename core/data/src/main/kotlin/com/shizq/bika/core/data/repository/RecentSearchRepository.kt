package com.shizq.bika.core.data.repository

import com.shizq.bika.core.data.model.RecentSearchQuery
import com.shizq.bika.core.data.model.asExternalModel
import com.shizq.bika.core.database.dao.RecentSearchQueryDao
import com.shizq.bika.core.database.model.RecentSearchQueryEntity
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class RecentSearchRepository @Inject constructor(
    private val recentSearchQueryDao: RecentSearchQueryDao,
) {
    suspend fun insertOrReplaceRecentSearch(searchQuery: String) {
        recentSearchQueryDao.insertOrReplaceRecentSearchQuery(
            RecentSearchQueryEntity(
                query = searchQuery,
                queriedDate = Clock.System.now(),
            ),
        )
    }

    fun getRecentSearchQueries(limit: Int): Flow<List<RecentSearchQuery>> =
        recentSearchQueryDao.getRecentSearchQueryEntities(limit).map { searchQueries ->
            searchQueries.map { it.asExternalModel() }
        }

    suspend fun clearRecentSearches() = recentSearchQueryDao.clearRecentSearchQueries()
}