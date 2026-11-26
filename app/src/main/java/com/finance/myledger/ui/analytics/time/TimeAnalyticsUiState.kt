package com.finance.myledger.ui.analytics.time

import com.finance.myledger.domain.model.DateRange
import com.finance.myledger.domain.usecase.ObserveTimeAnalyticsUseCase.TimeAnalytics

sealed class TimeAnalyticsUiState {
    data object Loading : TimeAnalyticsUiState()

    data class Success(
        val selectedRange: DateRange,
        val analytics: TimeAnalytics
    ) : TimeAnalyticsUiState()
}
