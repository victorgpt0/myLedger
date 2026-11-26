package com.finance.myledger.domain.usecase

import com.finance.myledger.domain.model.CounterpartySpend
import com.finance.myledger.domain.model.DateRange
import com.finance.myledger.domain.model.RecurringExpense
import com.finance.myledger.domain.model.toBounds
import com.finance.myledger.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Merchant / recurring analytics for a selected [DateRange].
 */
class ObserveMerchantAnalyticsUseCase @Inject constructor(
    private val repository: TransactionRepository,
    private val detectRecurring: DetectRecurringExpensesUseCase
) {
    data class MerchantAnalytics(
        val topMerchants: List<CounterpartySpend>,
        val recurring: List<RecurringExpense>
    )

    operator fun invoke(range: DateRange, topLimit: Int = 10): Flow<MerchantAnalytics> {
        val bounds = range.toBounds()
        return combine(
            repository.observeTopCounterparties(bounds, topLimit),
            // Recurring needs months of history, so it always uses its own look-back window
            // rather than the selected range (which could be a single month).
            detectRecurring()
        ) { top, recurring ->
            MerchantAnalytics(topMerchants = top, recurring = recurring)
        }
    }
}
