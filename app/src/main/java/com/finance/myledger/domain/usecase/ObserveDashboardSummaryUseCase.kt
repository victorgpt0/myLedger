package com.finance.myledger.domain.usecase

import com.finance.myledger.domain.model.DashboardSummary
import com.finance.myledger.domain.model.DateRange
import com.finance.myledger.domain.model.toBounds
import com.finance.myledger.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Assembles dashboard aggregates from the repository.
 * Keeps combine/calendar logic out of the ViewModel so the UI layer stays thin.
 * Core baseline still uses "this month" for the spend snapshot and fees line.
 */
class ObserveDashboardSummaryUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(recentLimit: Int = 4): Flow<DashboardSummary> {
        val thisMonth = DateRange.ThisMonth.toBounds()
        return combine(
            repository.observeLatestBalance(),
            repository.observeRecent(recentLimit),
            repository.observeExpenseTotalInRange(thisMonth),
            repository.observeCategoryTotalsInRange(thisMonth),
            repository.observeTotalFeesInRange(thisMonth)
        ) { balance, recent, monthlySpend, categorySpend, fees ->
            DashboardSummary(
                latestBalance = balance,
                recentTransactions = recent,
                monthlySpendTotal = monthlySpend,
                categorySpend = categorySpend,
                feesPaidThisMonth = fees
            )
        }
    }
}
