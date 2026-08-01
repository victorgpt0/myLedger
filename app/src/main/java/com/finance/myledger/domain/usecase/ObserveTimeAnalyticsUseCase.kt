package com.finance.myledger.domain.usecase

import com.finance.myledger.domain.model.DateRange
import com.finance.myledger.domain.model.DateRangeBounds
import com.finance.myledger.domain.model.DayOfWeekSpend
import com.finance.myledger.domain.model.MonthlyTotal
import com.finance.myledger.domain.model.toBounds
import com.finance.myledger.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Time-based analytics aggregates for a selected [DateRange].
 */
class ObserveTimeAnalyticsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    data class TimeAnalytics(
        val monthlyTrend: List<MonthlyTotal>,
        val dayOfWeekSpend: List<DayOfWeekSpend>,
        val totalFees: Double,
        val moneyMoved: Double,
        val transferTotal: Double,
        val expenseTotal: Double,
        val incomeTotal: Double
    ) {
        val feesAsPercentOfMoved: Double
            get() = if (moneyMoved <= 0.0) 0.0 else (totalFees / moneyMoved) * 100.0
    }

    operator fun invoke(range: DateRange): Flow<TimeAnalytics> {
        val bounds: DateRangeBounds = range.toBounds()
        // Nest combines — Flow.combine is limited to 5 sources per call.
        val primary = combine(
            repository.observeMonthlyTrend(bounds),
            repository.observeDayOfWeekSpend(bounds),
            repository.observeTotalFeesInRange(bounds),
            repository.observeMoneyMovedInRange(bounds),
            repository.observeTransferTotalInRange(bounds)
        ) { trend, dow, fees, moved, transfers ->
            PrimarySlice(trend, dow, fees, moved, transfers)
        }
        return combine(
            primary,
            repository.observeExpenseTotalInRange(bounds),
            repository.observeIncomeTotalInRange(bounds)
        ) { slice, expense, income ->
            TimeAnalytics(
                monthlyTrend = slice.trend,
                dayOfWeekSpend = slice.dow,
                totalFees = slice.fees,
                moneyMoved = slice.moved,
                transferTotal = slice.transfers,
                expenseTotal = expense,
                incomeTotal = income
            )
        }
    }

    private data class PrimarySlice(
        val trend: List<MonthlyTotal>,
        val dow: List<DayOfWeekSpend>,
        val fees: Double,
        val moved: Double,
        val transfers: Double
    )
}
