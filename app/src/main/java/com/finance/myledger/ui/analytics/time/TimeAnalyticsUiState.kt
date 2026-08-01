package com.finance.myledger.ui.analytics.time

import com.finance.myledger.domain.model.DateRange
import com.finance.myledger.domain.model.DayOfWeekSpend
import com.finance.myledger.domain.model.MonthlyTotal

sealed class TimeAnalyticsUiState {
    data object Loading : TimeAnalyticsUiState()

    data class Success(
        val selectedRange: DateRange,
        val monthlyTrend: List<MonthlyTotal>,
        val dayOfWeekSpend: List<DayOfWeekSpend>,
        val totalFees: Double,
        val feesPercentOfMoved: Double,
        val transferTotal: Double,
        val expenseTotal: Double,
        val incomeTotal: Double
    ) : TimeAnalyticsUiState()
}
