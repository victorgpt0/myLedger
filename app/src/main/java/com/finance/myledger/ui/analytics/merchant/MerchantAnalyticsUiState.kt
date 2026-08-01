package com.finance.myledger.ui.analytics.merchant

import com.finance.myledger.domain.model.CounterpartySpend
import com.finance.myledger.domain.model.DateRange
import com.finance.myledger.domain.model.RecurringExpense

sealed class MerchantAnalyticsUiState {
    data object Loading : MerchantAnalyticsUiState()

    data class Success(
        val selectedRange: DateRange,
        val topMerchants: List<CounterpartySpend>,
        val recurring: List<RecurringExpense>
    ) : MerchantAnalyticsUiState()
}
