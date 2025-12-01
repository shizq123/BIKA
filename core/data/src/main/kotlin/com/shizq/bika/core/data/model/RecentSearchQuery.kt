package com.shizq.bika.core.data.model

import com.shizq.bika.core.database.model.RecentSearchQueryEntity
import kotlin.time.Clock
import kotlin.time.Instant

data class RecentSearchQuery(
    val query: String,
    val queriedDate: Instant = Clock.System.now(),
)

fun RecentSearchQueryEntity.asExternalModel() = RecentSearchQuery(
    query = query,
    queriedDate = queriedDate,
)