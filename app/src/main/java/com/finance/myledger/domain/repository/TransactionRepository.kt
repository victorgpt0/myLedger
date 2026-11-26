package com.finance.myledger.domain.repository

import com.finance.myledger.domain.model.CategorySpend
import com.finance.myledger.domain.model.CounterpartySpend
import com.finance.myledger.domain.model.DateRangeBounds
import com.finance.myledger.domain.model.DayOfWeekSpend
import com.finance.myledger.domain.model.FeeByKind
import com.finance.myledger.domain.model.MonthlyTotal
import com.finance.myledger.domain.model.PeriodTotals
import com.finance.myledger.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Domain port for transaction persistence.
 * Implementation lives in data/; UI and use cases depend only on this interface so the
 * backing store (Room today, remote later) can change without touching callers.
 */
interface TransactionRepository {
    suspend fun insert(transaction: Transaction)

    fun observeAll(): Flow<List<Transaction>>

    fun observeRecent(limit: Int): Flow<List<Transaction>>

    fun observeInRange(bounds: DateRangeBounds): Flow<List<Transaction>>

    fun observeLatestBalance(): Flow<Double?>

    fun observeExpenseTotalInRange(bounds: DateRangeBounds): Flow<Double>

    fun observeIncomeTotalInRange(bounds: DateRangeBounds): Flow<Double>

    fun observeCategoryTotalsInRange(bounds: DateRangeBounds): Flow<List<CategorySpend>>

    fun observeTotalFeesInRange(bounds: DateRangeBounds): Flow<Double>

    fun observePeriodTotals(bounds: DateRangeBounds): Flow<PeriodTotals>

    fun observeFeesByKind(bounds: DateRangeBounds): Flow<List<FeeByKind>>

    fun observeFirstTimestamp(): Flow<Long?>

    fun observeMonthlyTrend(bounds: DateRangeBounds): Flow<List<MonthlyTotal>>

    fun observeDayOfWeekSpend(bounds: DateRangeBounds): Flow<List<DayOfWeekSpend>>

    fun observeTopCounterparties(bounds: DateRangeBounds, limit: Int = 10): Flow<List<CounterpartySpend>>
}
