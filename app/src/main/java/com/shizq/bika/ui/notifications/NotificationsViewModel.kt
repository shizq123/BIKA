package com.shizq.bika.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.shizq.bika.core.network.model.NotificationDoc
import com.shizq.bika.paging.NotificationsPagingSource
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationsPagingSource: NotificationsPagingSource,
) : ViewModel() {
    val notificationsFlow: Flow<PagingData<NotificationDoc>> = Pager(
        config = PagingConfig(20),
    ) {
        notificationsPagingSource
    }.flow
        .cachedIn(viewModelScope)
}