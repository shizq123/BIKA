package com.shizq.bika.core.database.di

import com.shizq.bika.core.database.BikaDatabase
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.database.dao.RecentSearchQueryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object DaosModule {
    @Provides
    fun providesRecentSearchQueryDao(
        database: BikaDatabase,
    ): RecentSearchQueryDao = database.recentSearchQueryDao()

    @Provides
    fun providesReadingHistoryDao(
        database: BikaDatabase,
    ): ReadingHistoryDao = database.readingHistoryDao()
}