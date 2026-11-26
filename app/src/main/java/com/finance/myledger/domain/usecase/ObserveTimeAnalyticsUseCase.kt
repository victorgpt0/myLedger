package com.finance.myledger.domain.usecase

import com.finance.myledger.domain.insights.PeriodMath
import com.finance.myledger.domain.model.ComparisonBasis
import com.finance.myledger.domain.model.DateRange
import com.finance.myledger.domain.model.FeeByKind
import com.finance.myledger.domain.model.MonthlyTotal
import com.finance.myledger.domain.model.PeriodTotals
import com.finance.myledger.domain.model.WeekdayAverage
import com.finance.myledger.domain.model.toBounds
import com.finance.myledger.domain.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Spending trends for a selected [DateRange]: headline totals compared with the previous
 * period, monthly trend, per-weekday averages, and fees by transaction kind.
 */
class ObserveTimeAnalyticsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    data class TimeAnalytics(
        val current: PeriodTotals,
        /** Null when there's no comparable earlier period (All time, or not enough history). */
        val previous: PeriodTotals?,
        val comparisonBasis: ComparisonBasis?,
        /** Calendar days with data in range (from first stored transaction up to today). */
        val daysInPeriod: Int,
        val projectedMonthEnd: Double?,
        val monthlyTrend: List<MonthlyTotal>,
        val weekdayAverages: List<WeekdayAverage>,
        val feesByKind: List<FeeByKind>
    ) {
        val averageDailySpend: Double
            get() = if (daysInPeriod == 0) 0.0 else current.expense / daysInPeriod
        val expenseChangePercent: Double?
            get() = previous?.let { PeriodMath.percentChange(current.expense, it.expense) }
        val incomeChangePercent: Double?
            get() = previous?.let { PeriodMath.percentChange(current.income, it.income) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(range: DateRange): Flow<TimeAnalytics> =
        repository.observeFirstTimestamp().flatMapLatest { firstTimestamp ->
            val now = System.currentTimeMillis()
            val bounds = range.toBounds(now)
            val span = PeriodMath.effectiveSpan(bounds, firstTimestamp, now)
            val comparison = PeriodMath.previousPeriod(range, firstTimestamp, now)
            val occurrences = span?.let { PeriodMath.weekdayOccurrences(it) } ?: IntArray(7)

            combine(
                repository.observePeriodTotals(bounds),
                comparison?.let { repository.observePeriodTotals(it.bounds) } ?: flowOf(null),
                repository.observeMonthlyTrend(bounds),
                repository.observeDayOfWeekSpend(bounds),
                repository.observeFeesByKind(bounds)
            ) { current, previous, trend, dayOfWeek, fees ->
                TimeAnalytics(
                    current = current,
                    previous = previous,
                    comparisonBasis = comparison?.basis,
                    daysInPeriod = occurrences.sum(),
                    projectedMonthEnd = PeriodMath.projectedMonthEnd(range, current.expense, now),
                    monthlyTrend = trend,
                    weekdayAverages = (0..6).map { dow ->
                        WeekdayAverage(
                            dayOfWeek = dow,
                            total = dayOfWeek.firstOrNull { it.dayOfWeek == dow }?.total ?: 0.0,
                            occurrences = occurrences[dow]
                        )
                    },
                    feesByKind = fees
                )
            }
        }
}
